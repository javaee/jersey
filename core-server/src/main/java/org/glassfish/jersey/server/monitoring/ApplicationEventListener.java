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

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.spi.Contract;

/**
 * Jersey specific provider that listens to {@link ApplicationEvent application events}.
 * The implementation of this interface will be called for two kind of events:
 * application events and {@link RequestEvent request events}. This interface will listen to
 * all {@link org.glassfish.jersey.server.monitoring.ApplicationEvent.Type application event types}
 * but only to first request event which is the {@link RequestEvent.Type#START}. On this event the
 * application event listener can decide whether it will listen to the request and return {@link RequestEventListener
 * request event listener} for listening to further request events.
 * }
 * <p/>
 * The implementation of this interface can be registered as a standard Jersey/JAX-RS provider
 * by annotating with {@link javax.ws.rs.ext.Provider @Provider} annotation in the case of
 * class path scanning, by registering as a provider using {@link org.glassfish.jersey.server.ResourceConfig}
 * or by returning from {@link javax.ws.rs.core.Application#getClasses()}
 * or {@link javax.ws.rs.core.Application#getSingletons()}}. The provider can be registered only on the server
 * side.
 * <p/>
 * Application event listener can read data of events but must not modify them in any way. The implementation
 * must be thread safe (the methods might be called from different threads).
 *
 * @author Miroslav Fuksa
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface ApplicationEventListener {
    /**
     * Process the application {@code event}. This method is called when new event occurs.
     *
     * @param event Application event.
     */
    public void onEvent(ApplicationEvent event);

    /**
     * Process a new request and return a {@link RequestEventListener request event listener} if
     * listening to {@link RequestEvent request events} is required. The method is called once for
     * each new incoming request. If listening to the request is required then request event must be returned
     * from the method. Such a request event listener will receive all request events that one request. If listening
     * to request event for the request is not required then {@code null} must be returned
     * from the method (do not return empty mock listener in these
     * cases as it will have negative performance impact).
     *
     * @param requestEvent Event of type {@link RequestEvent.Type#START}.
     * @return Request event listener that will monitor the events of the request
     *         connected with {@code requestEvent}; null otherwise.
     */
    public RequestEventListener onRequest(RequestEvent requestEvent);
}
