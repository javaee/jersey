/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.jdk.connector.internal;

import java.io.IOException;
import java.net.CookieManager;
import java.net.InetSocketAddress;
import java.net.URI;
import java.nio.ByteBuffer;
import java.util.List;
import java.util.Map;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.net.ssl.SSLContext;

import org.glassfish.jersey.SslConfigurator;

/**
 * @author Petr Janouch (petr.janouch at oracle.com)
 */
class HttpConnection {

    /**
     * Input buffer that is used by {@link TransportFilter} when SSL is turned on.
     * The size cannot be smaller than a maximal size of a SSL packet, which is 16kB for payload + header, because
     * {@link SslFilter} does not have its own buffer for buffering incoming
     * data and therefore the entire SSL packet must fit into {@link SslFilter}
     * input buffer.
     * <p/>
     */
    private static final int SSL_INPUT_BUFFER_SIZE = 17_000;
    /**
     * Input buffer that is used by {@link TransportFilter} when SSL is not turned on.
     */
    private static final int INPUT_BUFFER_SIZE = 2048;

    private static final Logger LOGGER = Logger.getLogger(HttpConnection.class.getName());

    private final Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> filterChain;
    private final CookieManager cookieManager;
    // we are interested only in host-port pair, but URI is a convenient holder for it
    private final URI uri;
    private final StateChangeListener stateListener;
    private final ScheduledExecutorService scheduler;
    private final ConnectorConfiguration configuration;

    private HttpRequest httpRequest;
    private HttpResponse httResponse;
    private Throwable error;
    volatile State state = State.CREATED;

    // by default we treat all connection as persistent
    // this flag will change to false if we receive "Connection: Close" header
    private boolean persistentConnection = true;

    private Future<?> responseTimeout;
    private Future<?> idleTimeout;
    private Future<?> connectTimeout;

    HttpConnection(URI uri,
                   CookieManager cookieManager,
                   ConnectorConfiguration configuration,
                   ScheduledExecutorService scheduler,
                   StateChangeListener stateListener) {
        this.uri = uri;
        this.cookieManager = cookieManager;
        this.stateListener = stateListener;
        this.configuration = configuration;
        this.scheduler = scheduler;
        filterChain = createFilterChain(uri, configuration);
    }

    synchronized void connect() {
        if (state != State.CREATED) {
            throw new IllegalStateException(LocalizationMessages.HTTP_CONNECTION_ESTABLISHING_ILLEGAL_STATE(state));
        }
        changeState(State.CONNECTING);
        scheduleConnectTimeout();
        filterChain.connect(new InetSocketAddress(uri.getHost(), Utils.getPort(uri)), null);
    }

    synchronized void send(final HttpRequest httpRequest) {
        if (state != State.IDLE) {
            throw new IllegalStateException(
                    "Http request cannot be sent over a connection that is in other state than IDLE. Current state: " + state);
        }

        cancelIdleTimeout();

        this.httpRequest = httpRequest;
        // clean state left by previous request
        httResponse = null;
        error = null;
        persistentConnection = true;
        changeState(State.SENDING_REQUEST);

        addRequestHeaders();

        filterChain.write(httpRequest, new CompletionHandler<HttpRequest>() {
            @Override
            public void failed(Throwable throwable) {
                handleError(throwable);
            }

            @Override
            public void completed(HttpRequest result) {
                handleHeaderSent();
            }
        });
    }

    synchronized void close() {
        if (state == State.CLOSED) {
            return;
        }

        cancelAllTimeouts();
        filterChain.close();
        changeState(State.CLOSED);
    }

    private synchronized void handleHeaderSent() {
        if (state != State.SENDING_REQUEST) {
            return;
        }

        scheduleResponseTimeout();

        if (httpRequest.getBodyMode() == HttpRequest.BodyMode.NONE
                || httpRequest.getBodyMode() == HttpRequest.BodyMode.BUFFERED) {
            changeState(State.RECEIVING_HEADER);
        } else {
            ChunkedBodyOutputStream bodyStream = (ChunkedBodyOutputStream) httpRequest.getBodyStream();
            bodyStream.setCloseListener(() -> {
                synchronized (HttpConnection.this) {
                    if (state != State.SENDING_REQUEST) {
                        return;
                    }
                }
                changeState(State.RECEIVING_HEADER);
            });
        }
    }

    private void addRequestHeaders() {
        Map<String, List<String>> cookies;
        try {
            cookies = cookieManager.get(httpRequest.getUri(), httpRequest.getHeaders());
        } catch (IOException e) {
            handleError(e);
            return;
        }

        // unfortunately CookieManager returns ""Cookie" -> empty list" pair if the cookie is not set
        cookies.entrySet().stream().filter(cookieHeader -> cookieHeader.getValue() != null && !cookieHeader.getValue().isEmpty())
                .forEach(cookieHeader -> httpRequest.getHeaders().put(cookieHeader.getKey(), cookieHeader.getValue()));
    }

    private void processResponseHeaders(HttpResponse response) throws IOException {
        cookieManager.put(httpRequest.getUri(), httResponse.getHeaders());
        List<String> connectionValues = response.getHeader(Constants.CONNECTION);
        if (connectionValues != null) {
            connectionValues.stream().filter(connectionValue -> connectionValue.equalsIgnoreCase(Constants.CONNECTION_CLOSE))
                    .forEach(connectionValue -> persistentConnection = false);
        }
    }

    protected Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> createFilterChain(URI uri,
                                                                                             ConnectorConfiguration
                                                                                                     configuration) {
        boolean secure = Constants.HTTPS.equals(uri.getScheme());
        Filter<ByteBuffer, ByteBuffer, ?, ?> socket;

        if (secure) {
            SSLContext sslContext = configuration.getSslContext();
            TransportFilter transportFilter = new TransportFilter(SSL_INPUT_BUFFER_SIZE, configuration.getThreadPoolConfig(),
                    configuration.getContainerIdleTimeout());

            if (sslContext == null) {
                sslContext = SslConfigurator.getDefaultContext();

            }

            socket = new SslFilter(transportFilter, sslContext, uri.getHost(), configuration.getHostnameVerifier());
        } else {
            socket = new TransportFilter(INPUT_BUFFER_SIZE, configuration.getThreadPoolConfig(),
                    configuration.getContainerIdleTimeout());
        }

        int maxHeaderSize = configuration.getMaxHeaderSize();
        HttpFilter httpFilter = new HttpFilter(socket, maxHeaderSize, maxHeaderSize + INPUT_BUFFER_SIZE);

        ConnectorConfiguration.ProxyConfiguration proxyConfiguration = configuration.getProxyConfiguration();
        if (proxyConfiguration.isConfigured()) {
            ProxyFilter proxyFilter = new ProxyFilter(httpFilter, proxyConfiguration);
            return new ConnectionFilter(proxyFilter);
        }

        return new ConnectionFilter(httpFilter);
    }

    private void changeState(State newState) {
        State old = state;
        state = newState;

        if (LOGGER.isLoggable(Level.FINEST)) {
            LOGGER.finest(LocalizationMessages.CONNECTION_CHANGING_STATE(uri.getHost(), uri.getPort(), old, newState));
        }

        stateListener.onStateChanged(this, old, newState);
    }

    private void scheduleResponseTimeout() {
        if (configuration.getResponseTimeout() == 0) {
            return;
        }

        responseTimeout = scheduler.schedule(() -> {
            synchronized (HttpConnection.this) {
                if (state != State.RECEIVING_HEADER && state != State.RECEIVING_BODY) {
                    return;
                }

                responseTimeout = null;
                changeState(State.RESPONSE_TIMEOUT);
                close();
            }
        }, configuration.getResponseTimeout(), TimeUnit.MILLISECONDS);
    }

    private void cancelResponseTimeout() {
        if (responseTimeout != null) {
            responseTimeout.cancel(true);
            responseTimeout = null;
        }
    }

    private void scheduleConnectTimeout() {
        if (configuration.getConnectTimeout() == 0) {
            return;
        }

        connectTimeout = scheduler.schedule(() -> {
            synchronized (HttpConnection.this) {
                if (state != State.CONNECTING) {
                    return;
                }

                connectTimeout = null;
                changeState(State.CONNECT_TIMEOUT);
                close();
            }
        }, configuration.getConnectTimeout(), TimeUnit.MILLISECONDS);
    }

    private void cancelConnectTimeout() {
        if (connectTimeout != null) {
            connectTimeout.cancel(true);
            connectTimeout = null;
        }
    }

    private void scheduleIdleTimeout() {
        if (configuration.getConnectionIdleTimeout() == 0) {
            return;
        }

        idleTimeout = scheduler.schedule(() -> {
            synchronized (HttpConnection.this) {
                if (state != State.IDLE) {
                    return;
                }
                idleTimeout = null;
                changeState(State.IDLE_TIMEOUT);
                close();
            }
        }, configuration.getConnectionIdleTimeout(), TimeUnit.MILLISECONDS);
    }

    private void cancelIdleTimeout() {
        if (idleTimeout != null) {
            idleTimeout.cancel(true);
            idleTimeout = null;
        }
    }

    private void cancelAllTimeouts() {
        cancelConnectTimeout();
        cancelIdleTimeout();
        cancelResponseTimeout();
    }

    private synchronized void handleError(Throwable t) {
        cancelAllTimeouts();
        error = t;
        changeState(State.ERROR);
        close();
    }

    private void changeStateToIdle() {
        scheduleIdleTimeout();
        changeState(State.IDLE);
    }

    Throwable getError() {
        return error;
    }

    HttpResponse getHttResponse() {
        return httResponse;
    }

    private synchronized void handleResponseRead() {
        cancelResponseTimeout();
        changeState(State.RECEIVED);
        if (!persistentConnection) {
            changeState(State.CLOSED);
            return;
        }
        changeStateToIdle();
    }

    private class ConnectionFilter extends Filter<HttpRequest, HttpResponse, HttpRequest, HttpResponse> {

        ConnectionFilter(Filter<HttpRequest, HttpResponse, ?, ?> downstreamFilter) {
            super(downstreamFilter);
        }

        @Override
        boolean processRead(HttpResponse response) {
            synchronized (HttpConnection.this) {
                if (state != State.RECEIVING_HEADER && state != State.SENDING_REQUEST) {
                    return false;
                }

                if (state == State.SENDING_REQUEST) {
                    // great we received response header so fast that we did not even switch into "receiving header" state,
                    // do it now to complete the formal lifecycle
                    // this happens when write completion listener is overtaken by "read event"
                    changeState(State.RECEIVING_HEADER);
                }

                httResponse = response;

                try {
                    processResponseHeaders(response);
                } catch (IOException e) {
                    handleError(e);
                    return false;
                }
            }

            if (response.getHasContent()) {
                AsynchronousBodyInputStream bodyStream = httResponse.getBodyStream();
                changeState(State.RECEIVING_BODY);
                bodyStream.setStateChangeLister(new AsynchronousBodyInputStream.StateChangeLister() {
                    @Override
                    public void onError(Throwable t) {
                        handleError(t);
                    }

                    @Override
                    public void onAllDataRead() {
                        handleResponseRead();
                    }
                });

            } else {
                handleResponseRead();
            }
            return false;
        }

        @Override
        void processConnect() {
            synchronized (HttpConnection.this) {
                if (state != State.CONNECTING) {
                    return;
                }

                downstreamFilter.startSsl();
            }
        }

        @Override
        void processSslHandshakeCompleted() {
            synchronized (HttpConnection.this) {
                if (state != State.CONNECTING) {
                    return;
                }

                cancelConnectTimeout();
                changeStateToIdle();
            }
        }

        @Override
        void processConnectionClosed() {
            cancelAllTimeouts();
            changeState(State.CLOSED_BY_SERVER);
            HttpConnection.this.close();
        }

        @Override
        void processError(Throwable t) {
            handleError(t);
        }

        @Override
        void write(HttpRequest data, CompletionHandler<HttpRequest> completionHandler) {
            downstreamFilter.write(data, completionHandler);
        }
    }

    enum State {
        CREATED,
        CONNECTING,
        CONNECT_TIMEOUT,
        IDLE,
        SENDING_REQUEST,
        RECEIVING_HEADER,
        RECEIVING_BODY,
        RECEIVED,
        RESPONSE_TIMEOUT,
        CLOSED_BY_SERVER,
        CLOSED,
        ERROR,
        IDLE_TIMEOUT
    }

    interface StateChangeListener {

        void onStateChanged(HttpConnection connection, State oldState, State newState);
    }
}
