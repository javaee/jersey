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

package org.glassfish.jersey.server.monitoring;

import java.util.Set;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.model.ResourceModel;

/**
 * An event informing about application lifecycle changes. The event is created by Jersey runtime and
 * handled by user registered {@link ApplicationEventListener application event listener}.
 * <p/>
 * The event contains the {@link Type} which distinguishes between types of event. There are various
 * properties in the event (accessible by getters) and some of them might be relevant only to specific event types.
 * <p/>
 * Note that internal state of the event must be modified. Even the event is immutable it exposes objects
 * which might be mutable and the code of event listener must not change state of these objects.
 *
 * @author Miroslav Fuksa
 */
public interface ApplicationEvent {

    /**
     * The type of the event that identifies on which lifecycle change the event is triggered.
     */
    public static enum Type {
        /**
         * Initialization of the application has started. In this point no all the event properties
         * are initialized yet.
         */
        INITIALIZATION_START,
        /**
         * Initialization of {@link org.glassfish.jersey.server.ApplicationHandler jersey application} is
         * finished but the server might not be started and ready yet to serve requests (this will be
         * indicated by the {@link #INITIALIZATION_FINISHED} event). This event indicates only that the
         * environment is ready (all providers are registered, application is configured, etc.).
         *
         * @since 2.5
         */
        INITIALIZATION_APP_FINISHED,
        /**
         * Initialization of the application has finished, server is started and application is ready
         * to handle requests now.
         */
        INITIALIZATION_FINISHED,
        /**
         * Application has been destroyed (stopped). In this point the application cannot process any new requests.
         */
        DESTROY_FINISHED,
        /**
         * The application reload is finished. The reload can be invoked by
         * {@link org.glassfish.jersey.server.spi.Container#reload()} method. When this event is triggered
         * the reload is completely finished, which means that the new application is initialized (appropriate
         * events are called) and new reloaded application is ready to server requests.
         */
        RELOAD_FINISHED
    }

    /**
     * Return the type of the event.
     *
     * @return Event type.
     */
    public Type getType();


    /**
     * Get resource config associated with the application. The resource config is set for all event types.
     *
     * @return Resource config on which this application is based on.
     */
    public ResourceConfig getResourceConfig();

    /**
     * Get resource classes registered by the user in the current application. The set contains only
     * user resource classes and not resource classes added by Jersey
     * or by {@link org.glassfish.jersey.server.model.ModelProcessor}.
     * <p/>
     * User resources are resources that
     * were explicitly registered by the configuration, discovered by the class path scanning or that
     * constructs explicitly registered {@link org.glassfish.jersey.server.model.Resource programmatic resource}.
     *
     * @return Resource user registered classes.
     */
    public Set<Class<?>> getRegisteredClasses();

    /**
     * Get resource instances registered by the user in the current application. The set contains only
     * user resources and not resources added by Jersey
     * or by {@link org.glassfish.jersey.server.model.ModelProcessor}.
     * <p/>
     * User resources are resources that
     * were explicitly registered by the configuration, discovered by the class path scanning or that
     * constructs explicitly registered {@link org.glassfish.jersey.server.model.Resource programmatic resource}.
     *
     * @return Resource instances registered by user.
     */
    public Set<Object> getRegisteredInstances();

    /**
     * Get registered providers available in the runtime. The registered providers
     * are providers like {@link org.glassfish.jersey.server.model.MethodList.Filter filters},
     * {@link javax.ws.rs.ext.ReaderInterceptor reader} and {@link javax.ws.rs.ext.WriterInterceptor writer}
     * interceptors which are explicitly registered by configuration, or annotated by
     * {@link javax.ws.rs.ext.Provider @Provider} or registered in META-INF/services. The
     * set does not include providers that are by default built in Jersey.
     *
     * @return Set of provider classes.
     */
    public Set<Class<?>> getProviders();

    /**
     * Get the resource model of the application. The method returns null for
     * {@link Type#INITIALIZATION_START} event type as the resource model is not initialized yet.
     * The returned resource model is the final deployed model including resources enhanced by
     * {@link org.glassfish.jersey.server.model.ModelProcessor model processors}.
     *
     * @return Resource model of the deployed application.
     */
    public ResourceModel getResourceModel();

}
