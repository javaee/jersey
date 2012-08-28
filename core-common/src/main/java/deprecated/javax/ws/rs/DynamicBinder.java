/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package deprecated.javax.ws.rs;

import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.WriterInterceptor;

/**
 * TODO remove.
 */
public interface DynamicBinder<T> {

    /**
     * Get the filter or interceptor instance or class that should be bound to the
     * particular resource method. May return {@code null}.
     * <p>
     * The returned provider instance or class is expected to be implementing one
     * or more of the following interfaces:
     * </p>
     * <ul>
     *     <li>{@link javax.ws.rs.container.ContainerRequestFilter}</li>
     *     <li>{@link javax.ws.rs.container.ContainerResponseFilter}</li>
     *     <li>{@link ReaderInterceptor}</li>
     *     <li>{@link WriterInterceptor}</li>
     * </ul>
     * A provider instance or class that does not implement any of the interfaces
     * above is ignored and a {@link java.util.logging.Level#WARNING warning}
     * message is logged.
     * <p />
     * <p>
     * If the returned object is a {@link Class Class&lt;P&gt;}, JAX-RS runtime will
     * resolve the class to an instance of type {@code P} by first looking at the
     * already registered provider instances.
     * If there is already a provider instance of the class registered, the JAX-RS
     * runtime will use it, otherwise a new provider instance of the class will be
     * instantiated, injected and registered by the JAX-RS runtime.
     * </p>
     * <p>
     * In case the resolving the returned provider class to an instance fails for
     * any reason, the dynamically bound provider class is ignored and a
     * {@link java.util.logging.Level#WARNING warning} message is logged.
     * </p>
     * <p>
     * The method is called during a (sub)resource method discovery phase (typically
     * once per each discovered (sub)resource method) to return a filter instance
     * that should be bound to a particular (sub)resource method identified by the
     * supplied {@link javax.ws.rs.container.ResourceInfo resource information}.
     * </p>
     *
     * @param resourceInfo resource class and method information.
     * @return a filter or interceptor instance that should be dynamically bound
     *     to the (sub)resource method or {@code null} otherwise.
     */
    public T getBoundProvider(ResourceInfo resourceInfo);
}
