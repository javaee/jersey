/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Queue;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;

import org.glassfish.jersey.server.ExtendedResourceContext;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.internal.RuntimeExecutorsBinder;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener;
import org.glassfish.jersey.server.monitoring.RequestEvent;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * Process events of application and request processing into
 * {@link org.glassfish.jersey.server.monitoring.MonitoringStatistics monitoring statistics}.
 * The {@code MonitoringStatisticsProcessor} starts a new thread which process events in regular intervals
 * and for each new monitoring statistics it calls registered
 * {@link MonitoringStatisticsListener monitoring statistics event listeners}.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
class MonitoringStatisticsProcessor {

    private static final Logger LOGGER = Logger.getLogger(MonitoringStatisticsProcessor.class.getName());
    public static final int SHUTDOWN_TIMEOUT = 10;
    private final MonitoringEventListener monitoringEventListener;
    private final MonitoringStatisticsImpl.Builder statisticsBuilder;
    private final List<MonitoringStatisticsListener> statisticsCallbackList;
    private final ScheduledExecutorService scheduler;


    /**
     * Creates a new instance of processor.
     * @param serviceLocator Service locator.
     * @param monitoringEventListener Monitoring event listener.
     */
    MonitoringStatisticsProcessor(ServiceLocator serviceLocator, MonitoringEventListener monitoringEventListener) {
        this.monitoringEventListener = monitoringEventListener;
        final ResourceModel resourceModel = serviceLocator.getService(ExtendedResourceContext.class).getResourceModel();
        this.statisticsBuilder = new MonitoringStatisticsImpl.Builder(resourceModel);
        this.statisticsCallbackList = serviceLocator.getAllServices(MonitoringStatisticsListener.class);
        this.scheduler = serviceLocator.getService(ScheduledExecutorService.class,
                new RuntimeExecutorsBinder.BackgroundSchedulerLiteral());
    }

    /**
     * Start the thread that will process the events
     * into {@link org.glassfish.jersey.server.monitoring.MonitoringStatistics}.
     */
    public void startMonitoringWorker() {
        scheduler.scheduleWithFixedDelay(new Runnable() {
            @Override
            public void run() {
                try {
                    processApplicationEvents();
                    processRequestItems();
                    processResponseCodeEvents();
                    processExceptionMapperEvents();
                } catch (Throwable t) {
                    LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_MONITORING_STATISTICS_GENERATION(), t);
                    // rethrowing exception stops further task execution
                    throw new ProcessingException(LocalizationMessages.ERROR_MONITORING_STATISTICS_GENERATION(), t);
                }

                final MonitoringStatisticsImpl immutableStats = statisticsBuilder.build();
                final Iterator<MonitoringStatisticsListener> iterator = statisticsCallbackList.iterator();
                while (iterator.hasNext() && !Thread.currentThread().isInterrupted()) {
                    MonitoringStatisticsListener monitoringStatisticsListener = iterator.next();
                    try {
                        monitoringStatisticsListener.onStatistics(immutableStats);
                    } catch (Throwable t) {
                        LOGGER.log(Level.SEVERE, LocalizationMessages.ERROR_MONITORING_STATISTICS_LISTENER(
                                monitoringStatisticsListener.getClass()), t);
                        iterator.remove();
                    }
                }
            }
        }, 0, 500, TimeUnit.MILLISECONDS);
    }

    private void processApplicationEvents() {
        while (!monitoringEventListener.getApplicationEvents().isEmpty()) {
            final ApplicationEvent appEvent = monitoringEventListener.getApplicationEvents().remove();
            switch (appEvent.getType()) {
                case INITIALIZATION_FINISHED:
                case RELOAD_FINISHED:
                    final ApplicationStatisticsImpl initStatistics = new ApplicationStatisticsImpl(appEvent.getResourceConfig(),
                            new Date(monitoringEventListener.getApplicationStartTime()), appEvent.getRegisteredClasses(),
                            appEvent.getRegisteredInstances(), appEvent.getProviders());
                    statisticsBuilder.setApplicationStatisticsImpl(initStatistics);
                    break;
            }


        }
    }

    private void processExceptionMapperEvents() {
        final Queue<RequestEvent> eventQueue = monitoringEventListener.getExceptionMapperEvents();
        while (!eventQueue.isEmpty()) {
            final RequestEvent event = eventQueue.remove();
            final ExceptionMapperStatisticsImpl.Builder mapperStats = statisticsBuilder.getExceptionMapperStatisticsBuilder();

            if (event.getExceptionMapper() != null) {
                mapperStats.addExceptionMapperExecution(event.getExceptionMapper().getClass(), 1);
            }

            mapperStats.addMapping(event.isResponseSuccessfullyMapped(), 1);
        }
    }

    private void processRequestItems() {
        final Queue<MonitoringEventListener.RequestStats> requestQueuedItems = monitoringEventListener.getRequestQueuedItems();

        while (!requestQueuedItems.isEmpty()) {
            MonitoringEventListener.RequestStats event = requestQueuedItems.remove();
            final MonitoringEventListener.TimeStats requestStats = event.getRequestStats();
            statisticsBuilder.getRequestStatisticsBuilder().addExecution(requestStats.getStartTime(),
                    requestStats.getDuration());
            final MonitoringEventListener.MethodStats methodStat = event.getMethodStats();

            if (methodStat != null) {
                final ResourceMethod method = methodStat.getMethod();
                statisticsBuilder.addExecution(event.getRequestUri(), method,
                        methodStat.getStartTime(), methodStat.getDuration(),
                        requestStats.getStartTime(), requestStats.getDuration());
            }
        }
    }

    private void processResponseCodeEvents() {
        final Queue<Integer> responseEvents = monitoringEventListener.getResponseStatuses();
        Integer code;
        while (!responseEvents.isEmpty()) {
            code = responseEvents.remove();
            statisticsBuilder.addResponseCode(code);
        }

    }

    /**
     * Stops processing of any further execution of this processor. The internal thread will finish
     * processing of actual events and will be not executed again. The method finishes after the
     * internal thread finish its processing loop.
     *
     * @throw ProcessingException thrown when waiting for the thread to finish the work is interrupted. In this
     * case internal listeners will be still shutdown.
     */
    void shutDown() throws InterruptedException {
        scheduler.shutdown();
        boolean success = scheduler.awaitTermination(SHUTDOWN_TIMEOUT, TimeUnit.SECONDS);
        if (!success) {
            LOGGER.warning(LocalizationMessages.ERROR_MONITORING_SCHEDULER_DESTROY_TIMEOUT());
        }
    }
}