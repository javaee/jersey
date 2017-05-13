/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.jdk.connector;

import java.io.IOException;
import java.net.CookieManager;
import java.net.URI;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.ScheduledExecutorService;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class DestinationConnectionPool {

    private final ConnectorConfiguration configuration;
    private final Queue<HttpConnection> idleConnections = new ConcurrentLinkedDeque<>();
    private final Set<HttpConnection> connections = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final Queue<RequestRecord> pendingRequests = new ConcurrentLinkedDeque<>();
    private final Map<HttpConnection, RequestRecord> requestsInProgress = new HashMap<>();
    private final CookieManager cookieManager;
    private final ScheduledExecutorService scheduler;
    private final ConnectionStateListener connectionStateListener;

    private volatile ConnectionCloseListener connectionCloseListener;

    private int connectionCounter = 0;
    private boolean closed = false;

    DestinationConnectionPool(ConnectorConfiguration configuration,
                              CookieManager cookieManager,
                              ScheduledExecutorService scheduler) {
        this.configuration = configuration;
        this.cookieManager = cookieManager;
        this.scheduler = scheduler;
        this.connectionStateListener = new ConnectionStateListener();
    }

    void setConnectionCloseListener(ConnectionCloseListener connectionCloseListener) {
        this.connectionCloseListener = connectionCloseListener;
    }

    void send(HttpRequest httpRequest, CompletionHandler<HttpResponse> completionHandler) {
        pendingRequests.add(new RequestRecord(httpRequest, completionHandler));
        processPendingRequests();
    }

    private void processPendingRequests() {
        HttpConnection connection;
        HttpRequest httpRequest;
        CompletionHandler<HttpResponse> completionHandler;

        synchronized (this) {
            /* this is synchronized so that another thread does not steal the pending request at the head of the queue
            while we investigate if we can execute it. */
            RequestRecord pendingHead = pendingRequests.peek();
            if (pendingHead == null) {
                // no pending requests
                return;
            }

            httpRequest = pendingHead.request;
            completionHandler = pendingHead.completionHandler;

            connection = idleConnections.poll();
            if (connection != null) {
                pendingRequests.poll();
            }
        }

        if (connection != null) {
            // if there was a connection available just use it
            requestsInProgress.put(connection, new RequestRecord(httpRequest, completionHandler));
            connection.send(httpRequest);
            return;
        }

        // if there was not a connection available keep this requests in pending list and try to create a connection
        synchronized (this) {
            // synchronized because other thread might open/close connections, so we have to make sure we get the limits right.

            if (configuration.getMaxConnectionsPerDestination() == connectionCounter) {
                // we are at the limit for this destination, just wait for a connection to become idle or close
                return;
            }

            // create a connection
            connection = new HttpConnection(httpRequest.getUri(), cookieManager, configuration, scheduler,
                    connectionStateListener);
            connections.add(connection);
            connectionCounter++;
        }

        // we don't want to connect inside the synchronized block
        connection.connect();
    }

    synchronized void close() {
        if (closed) {
            return;
        }

        closed = true;

        connections.forEach(HttpConnection::close);
    }

    private RequestRecord getRequest(HttpConnection connection) {
        RequestRecord requestRecord = requestsInProgress.get(connection);
        if (requestRecord == null) {
            throw new IllegalStateException("Request not found");
        }

        return requestRecord;
    }

    private RequestRecord removeRequest(HttpConnection connection) {
        RequestRecord requestRecord = requestsInProgress.get(connection);
        if (requestRecord == null) {
            throw new IllegalStateException("Request not found");
        }

        return requestRecord;
    }

    private void handleIdleConnection(HttpConnection connection) {
        idleConnections.add(connection);
        processPendingRequests();
    }

    private void cleanClosedConnection(HttpConnection connection) {
        if (closed) {
            return;
        }

        RequestRecord pendingRequest;
        synchronized (this) {
            idleConnections.remove(connection);
            connections.remove(connection);
            connectionCounter--;

            pendingRequest = pendingRequests.peek();
            if (pendingRequest == null) {
                if (connectionCounter == 0) {
                    connectionCloseListener.onLastConnectionClosed();
                }
                return;
            }
        }

        processPendingRequests();
    }

    private void handleIllegalStateTransition(HttpConnection.State oldState, HttpConnection.State newState) {
        throw new IllegalStateException("Illegal state transition, old state: " + oldState + " new state: " + newState);
    }

    private synchronized void removeAllPendingWithError(Throwable t) {
        for (RequestRecord requestRecord : pendingRequests) {
            requestRecord.completionHandler.failed(t);
        }

        pendingRequests.clear();
    }

    private class ConnectionStateListener implements HttpConnection.StateChangeListener {

        @Override
        public void onStateChanged(HttpConnection connection, HttpConnection.State oldState, HttpConnection.State newState) {
            switch (newState) {

                case IDLE: {
                    switch (oldState) {
                        case RECEIVED:
                        case CONNECTING: {
                            handleIdleConnection(connection);
                            return;
                        }

                        default: {
                            handleIllegalStateTransition(oldState, newState);
                            return;
                        }
                    }
                }

                case RECEIVED: {
                    switch (oldState) {
                        case RECEIVING_HEADER: {
                            RequestRecord request = removeRequest(connection);
                            request.completionHandler.completed(connection.getHttResponse());
                            return;
                        }

                        case RECEIVING_BODY: {
                            removeRequest(connection);
                            return;
                        }

                        default: {
                            handleIllegalStateTransition(oldState, newState);
                            return;
                        }
                    }
                }

                case RECEIVING_BODY: {
                    switch (oldState) {
                        case RECEIVING_HEADER: {
                            RequestRecord request = getRequest(connection);
                            request.response = connection.getHttResponse();
                            request.completionHandler.completed(connection.getHttResponse());
                            return;
                        }

                        default: {
                            handleIllegalStateTransition(oldState, newState);
                            return;
                        }
                    }
                }

                case ERROR: {
                    switch (oldState) {
                        case SENDING_REQUEST: {
                            RequestRecord request = removeRequest(connection);
                            request.completionHandler.failed(connection.getError());
                            return;
                        }

                        case RECEIVING_HEADER: {
                            RequestRecord request = removeRequest(connection);
                            request.completionHandler.failed(connection.getError());
                            return;
                        }

                        case RECEIVING_BODY: {
                            requestsInProgress.remove(connection);
                            return;
                        }

                        case CONNECTING: {
                            removeAllPendingWithError(connection.getError());
                            return;
                        }

                        default: {
                            connection.getError().printStackTrace();
                            handleIllegalStateTransition(oldState, newState);
                            return;
                        }
                    }
                }

                case RESPONSE_TIMEOUT: {
                    switch (oldState) {
                        case RECEIVING_HEADER: {
                            RequestRecord request = removeRequest(connection);
                            request.completionHandler
                                    .failed(new IOException(LocalizationMessages.TIMEOUT_RECEIVING_RESPONSE()));
                            return;
                        }

                        case RECEIVING_BODY: {
                            RequestRecord request = requestsInProgress.remove(connection);
                            request.response.getBodyStream()
                                    .notifyError(new IOException(LocalizationMessages.TIMEOUT_RECEIVING_RESPONSE_BODY()));
                            return;
                        }

                        default: {
                            handleIllegalStateTransition(oldState, newState);
                            return;
                        }
                    }
                }

                case CLOSED_BY_SERVER: {
                    switch (oldState) {
                        case SENDING_REQUEST: {
                            RequestRecord request = removeRequest(connection);
                            request.completionHandler
                                    .failed(new IOException(LocalizationMessages.CLOSED_WHILE_SENDING_REQUEST()));
                            return;
                        }

                        case RECEIVING_HEADER: {
                            RequestRecord request = removeRequest(connection);
                            request.completionHandler
                                    .failed(new IOException(LocalizationMessages.CLOSED_WHILE_RECEIVING_RESPONSE(),
                                            connection.getError()));
                            return;
                        }

                        case RECEIVING_BODY: {
                            RequestRecord request = requestsInProgress.remove(connection);
                            request.response.getBodyStream().notifyError(
                                    new IOException(LocalizationMessages.CLOSED_WHILE_RECEIVING_BODY(),
                                            connection.getError()));
                            return;
                        }

                        case CONNECTING: {
                            removeAllPendingWithError(new IOException(LocalizationMessages.CONNECTION_CLOSED()));
                            return;
                        }
                    }
                }

                case CLOSED: {
                    switch (oldState) {
                        case SENDING_REQUEST: {
                            RequestRecord request = removeRequest(connection);
                            request.completionHandler
                                    .failed(new IOException(LocalizationMessages.CLOSED_BY_CLIENT_WHILE_SENDING()));
                            cleanClosedConnection(connection);
                            return;
                        }

                        case RECEIVING_HEADER: {
                            RequestRecord request = removeRequest(connection);
                            request.completionHandler
                                    .failed(new IOException(LocalizationMessages.CLOSED_WHILE_RECEIVING_RESPONSE()));
                            cleanClosedConnection(connection);
                            return;
                        }

                        case RECEIVING_BODY: {
                            RequestRecord request = requestsInProgress.remove(connection);
                            request.response.getBodyStream().notifyError(
                                    new IOException(LocalizationMessages.CLOSED_BY_CLIENT_WHILE_RECEIVING_BODY(),
                                            connection.getError()));
                            cleanClosedConnection(connection);
                            return;
                        }

                        default: {
                            cleanClosedConnection(connection);
                            return;
                        }
                    }
                }

                case CONNECT_TIMEOUT: {
                    switch (oldState) {
                        case CONNECTING: {
                            removeAllPendingWithError(new IOException(LocalizationMessages.CONNECTION_TIMEOUT()));
                            return;
                        }

                        default: {
                            cleanClosedConnection(connection);
                        }
                    }
                }
            }
        }
    }

    private static class RequestRecord {

        private final HttpRequest request;
        private final CompletionHandler<HttpResponse> completionHandler;
        private HttpResponse response;

        public RequestRecord(HttpRequest request, CompletionHandler<HttpResponse> completionHandler) {
            this.request = request;
            this.completionHandler = completionHandler;
        }
    }

    static class DestinationKey {

        private final String host;
        private final int port;
        private final boolean secure;

        DestinationKey(URI uri) {
            host = uri.getHost();
            port = Utils.getPort(uri);
            secure = Constants.HTTPS.equalsIgnoreCase(uri.getScheme());
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            DestinationKey that = (DestinationKey) o;

            return port == that.port && secure == that.secure && host.equals(that.host);
        }

        @Override
        public int hashCode() {
            int result = host.hashCode();
            result = 31 * result + port;
            result = 31 * result + (secure ? 1 : 0);
            return result;
        }
    }

    interface ConnectionCloseListener {

        void onLastConnectionClosed();
    }
}
