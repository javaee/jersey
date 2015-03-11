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

import javax.inject.Singleton;

import org.glassfish.jersey.server.monitoring.ApplicationEvent;
import org.glassfish.jersey.server.monitoring.ApplicationEventListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.server.spi.ContainerLifecycleListener;

import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Container listener that listens to container events and trigger the {@link ApplicationEvent application events}
 * and call them on supplied {@link org.glassfish.jersey.server.monitoring.RequestEventListener}.
 * <p/>
 * This listener must be registered as a standard provider in Jersey runtime.
 *
 * @author Miroslav Fuksa
 */
public final class MonitoringContainerListener implements ContainerLifecycleListener {

    private volatile ApplicationEvent initFinishedEvent;
    private volatile ApplicationEventListener listener;

    /**
     * Initializes the instance with listener that must be called and initialization event. If this method
     * is not called then events cannot not be triggered which might be needed when no
     * {@link ApplicationEventListener} is registered in Jersey runtime.
     *
     * @param listener Listener that should be called.
     * @param initFinishedEvent Event of type {@link ApplicationEvent.Type#INITIALIZATION_START}.
     */
    public void init(ApplicationEventListener listener, ApplicationEvent initFinishedEvent) {
        this.listener = listener;
        this.initFinishedEvent = initFinishedEvent;
    }

    @Override
    public void onStartup(Container container) {
        if (listener != null) {
            listener.onEvent(getApplicationEvent(ApplicationEvent.Type.INITIALIZATION_FINISHED));
        }
    }

    @Override
    public void onReload(Container container) {
        if (listener != null) {
            listener.onEvent(getApplicationEvent(ApplicationEvent.Type.RELOAD_FINISHED));
        }
    }

    private ApplicationEvent getApplicationEvent(ApplicationEvent.Type type) {
        return new ApplicationEventImpl(type,
                initFinishedEvent.getResourceConfig(), initFinishedEvent.getProviders(),
                initFinishedEvent.getRegisteredClasses(), initFinishedEvent.getRegisteredInstances(),
                initFinishedEvent.getResourceModel());
    }

    @Override
    public void onShutdown(Container container) {
        if (listener != null) {
            listener.onEvent(getApplicationEvent(ApplicationEvent.Type.DESTROY_FINISHED));
        }
    }

    /**
     * A binder that binds the {@link MonitoringContainerListener}.
     */
    public static class Binder extends AbstractBinder {
        @Override
        protected void configure() {
            bindAsContract(MonitoringContainerListener.class)
                    .to(ContainerLifecycleListener.class).in(Singleton.class);
        }
    }
}
