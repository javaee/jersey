/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.monitoring;

import java.util.List;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;

import javax.annotation.Priority;
import javax.inject.Inject;

import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.monitoring.DestroyListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;
import org.glassfish.jersey.server.monitoring.RequestEventListener;
import org.glassfish.jersey.uri.UriTemplate;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Queues;

/**
 * {@link ApplicationEventListener application event listener} that listens to {@link ApplicationEvent application}
 * and {@link RequestEvent request} events and supplies data to {@link MonitoringStatisticsProcessor} which
 * produces {@link org.glassfish.jersey.server.monitoring.MonitoringStatistics monitoring statistics}.
 * <p>
 * The {@link MonitoringStatisticsProcessor} is started by this class after the first application event
 * comes.
 * </p>
 * <p>
 * This event listener must be registered as a standard provider when monitoring statistics are required
 * in the runtime.
 * </p>
 *
 * @author Miroslav Fuksa
 * @see MonitoringStatisticsProcessor
 */
@Priority(ApplicationInfoListener.PRIORITY + 100)
public final class MonitoringEventListener implements ApplicationEventListener {

    private static final Logger LOGGER = Logger.getLogger(MonitoringEventListener.class.getName());
    private static final int EVENT_QUEUE_SIZE = 500_000;

    @Inject
    private ServiceLocator serviceLocator;

    private final Queue<RequestStats> requestQueuedItems = Queues.newArrayBlockingQueue(EVENT_QUEUE_SIZE);
    private final Queue<Integer> responseStatuses = Queues.newArrayBlockingQueue(EVENT_QUEUE_SIZE);
    private final Queue<RequestEvent> exceptionMapperEvents = Queues.newArrayBlockingQueue(EVENT_QUEUE_SIZE);
    private volatile MonitoringStatisticsProcessor monitoringStatisticsProcessor;

    /**
     * Time statistics.
     */
    static class TimeStats {

        private final long duration;
        private final long startTime;

        private TimeStats(final long startTime, final long requestDuration) {
            this.duration = requestDuration;
            this.startTime = startTime;
        }

        /**
         * Get duration.
         *
         * @return Duration in milliseconds.
         */
        long getDuration() {
            return duration;
        }

        /**
         * Get start time.
         *
         * @return Start time (Unix timestamp format).
         */
        long getStartTime() {
            return startTime;
        }
    }

    /**
     * Method statistics.
     */
    static class MethodStats extends TimeStats {

        private final ResourceMethod method;

        private MethodStats(final ResourceMethod method, final long startTime, final long requestDuration) {
            super(startTime, requestDuration);
            this.method = method;
        }

        /**
         * Get the resource method executed.
         *
         * @return resource method.
         */
        ResourceMethod getMethod() {
            return method;
        }
    }

    /**
     * Request statistics.
     */
    static class RequestStats {

        private final TimeStats requestStats;
        private final MethodStats methodStats; // might be null if a method was not executed during a request
        private final String requestUri;

        private RequestStats(final TimeStats requestStats, final MethodStats methodStats, final String requestUri) {
            this.requestStats = requestStats;
            this.methodStats = methodStats;
            this.requestUri = requestUri;
        }

        /**
         * Get request statistics.
         *
         * @return request statistics.
         */
        TimeStats getRequestStats() {
            return requestStats;
        }

        /**
         * Get method statistics.
         *
         * @return method statistics.
         */
        MethodStats getMethodStats() {
            return methodStats;
        }

        /**
         * Get the request uri.
         *
         * @return request uri.
         */
        String getRequestUri() {
            return requestUri;
        }
    }

    @Override
    public ReqEventListener onRequest(final RequestEvent requestEvent) {
        switch (requestEvent.getType()) {
            case START:
                return new ReqEventListener();

        }
        return null;
    }

    @Override
    public void onEvent(final ApplicationEvent event) {
        final ApplicationEvent.Type type = event.getType();
        switch (type) {
            case INITIALIZATION_START:
                break;
            case RELOAD_FINISHED:
            case INITIALIZATION_FINISHED:
                this.monitoringStatisticsProcessor = new MonitoringStatisticsProcessor(serviceLocator, this);
                this.monitoringStatisticsProcessor.startMonitoringWorker();
                break;
            case DESTROY_FINISHED:
                if (monitoringStatisticsProcessor != null) {
                    try {
                        monitoringStatisticsProcessor.shutDown();
                    } catch (final InterruptedException e) {
                        Thread.currentThread().interrupt();
                        throw new ProcessingException(LocalizationMessages.ERROR_MONITORING_SHUTDOWN_INTERRUPTED(), e);
                    }
                }

                // onDestroy
                final List<DestroyListener> listeners =
                        serviceLocator.getAllServices(DestroyListener.class);

                for (final DestroyListener listener : listeners) {
                    try {
                        listener.onDestroy();
                    } catch (final Exception e) {
                        LOGGER.log(Level.WARNING,
                                LocalizationMessages.ERROR_MONITORING_STATISTICS_LISTENER_DESTROY(listener.getClass()), e);
                    }
                }

                break;

        }
    }

    private class ReqEventListener implements RequestEventListener {

        private final long requestTimeStart;
        private volatile long methodTimeStart;
        private volatile MethodStats methodStats;

        public ReqEventListener() {
            this.requestTimeStart = System.currentTimeMillis();
        }

        @Override
        public void onEvent(final RequestEvent event) {
            final long now = System.currentTimeMillis();

            switch (event.getType()) {
                case RESOURCE_METHOD_START:
                    this.methodTimeStart = now;
                    break;
                case RESOURCE_METHOD_FINISHED:
                    final ResourceMethod method = event.getUriInfo().getMatchedResourceMethod();
                    methodStats = new MethodStats(method, methodTimeStart, now - methodTimeStart);
                    break;
                case EXCEPTION_MAPPING_FINISHED:
                    if (!exceptionMapperEvents.offer(event)) {
                        LOGGER.warning(LocalizationMessages.ERROR_MONITORING_QUEUE_MAPPER());
                    }
                    break;
                case FINISHED:
                    if (event.isResponseWritten()) {
                        if (!responseStatuses.offer(event.getContainerResponse().getStatus())) {
                            LOGGER.warning(LocalizationMessages.ERROR_MONITORING_QUEUE_RESPONSE());
                        }
                    }
                    final StringBuilder sb = new StringBuilder();
                    final List<UriTemplate> orderedTemplates = Lists.reverse(event.getUriInfo().getMatchedTemplates());

                    for (final UriTemplate uriTemplate : orderedTemplates) {
                        sb.append(uriTemplate.getTemplate());
                        if (!uriTemplate.endsWithSlash()) {
                            sb.append("/");
                        }
                        sb.setLength(sb.length() - 1);
                    }

                    if (!requestQueuedItems.offer(new RequestStats(new TimeStats(requestTimeStart, now - requestTimeStart),
                            methodStats, sb.toString()))) {
                        LOGGER.warning(LocalizationMessages.ERROR_MONITORING_QUEUE_REQUEST());
                    }

            }
        }
    }

    /**
     * Get the exception mapper event queue.
     *
     * @return Exception mapper event queue.
     */
    Queue<RequestEvent> getExceptionMapperEvents() {
        return exceptionMapperEvents;
    }

    /**
     * Get the request event queue.
     *
     * @return Request event queue.
     */
    Queue<RequestStats> getRequestQueuedItems() {
        return requestQueuedItems;
    }

    /**
     * Get the queue with response status codes.
     *
     * @return response status queue.
     */
    Queue<Integer> getResponseStatuses() {
        return responseStatuses;
    }
}
