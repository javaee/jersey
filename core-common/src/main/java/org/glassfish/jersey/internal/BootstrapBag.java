/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal;

import java.lang.reflect.Type;
import java.util.List;
import java.util.Objects;

import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.model.internal.ManagedObjectsFinalizer;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.spi.ContextResolvers;
import org.glassfish.jersey.spi.ExceptionMappers;

/**
 * A holder that is used only during Jersey bootstrap to keep the instances of the given types and then use them during the
 * bootstrap. This works as a replacement of an injection framework during a bootstrap and intentionally keeps all needed types in
 * separate fields to make strong type nature and to preserve a clear view which types are needed to inject to other services.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class BootstrapBag {

    private Configuration configuration;
    private RequestScope requestScope;
    private MessageBodyWorkers messageBodyWorkers;
    private ExceptionMappers exceptionMappers;
    private ContextResolvers contextResolvers;
    private ManagedObjectsFinalizer managedObjectsFinalizer;
    private List<AutoDiscoverable> autoDiscoverables;

    /**
     * Gets a list of {@link AutoDiscoverable}.
     *
     * @return list of {@link AutoDiscoverable}.
     */
    public List<AutoDiscoverable> getAutoDiscoverables() {
        return autoDiscoverables;
    }

    /**
     * Sets a list of {@link AutoDiscoverable}.
     *
     * @param autoDiscoverables list of {@code AutoDiscoverable}.
     */
    public void setAutoDiscoverables(List<AutoDiscoverable> autoDiscoverables) {
        this.autoDiscoverables = autoDiscoverables;
    }

    /**
     * Gets an instance of {@link ManagedObjectsFinalizer}.
     *
     * @return {@code ManagedObjectsFinalizer} instance.
     */
    public ManagedObjectsFinalizer getManagedObjectsFinalizer() {
        return managedObjectsFinalizer;
    }

    /**
     * Sets an instance of {@link ManagedObjectsFinalizer}.
     *
     * @param managedObjectsFinalizer {@code ManagedObjectsFinalizer} instance.
     */
    public void setManagedObjectsFinalizer(ManagedObjectsFinalizer managedObjectsFinalizer) {
        this.managedObjectsFinalizer = managedObjectsFinalizer;
    }

    /**
     * Gets an instance of {@link RequestScope}.
     *
     * @return {@code RequestScope} instance.
     */
    public RequestScope getRequestScope() {
        requireNonNull(requestScope, RequestScope.class);
        return requestScope;
    }

    /**
     * Sets an instance of {@link RequestScope}.
     *
     * @param requestScope {@code RequestScope} instance.
     */
    public void setRequestScope(RequestScope requestScope) {
        this.requestScope = requestScope;
    }

    /**
     * Gets an instance of {@link MessageBodyWorkers}.
     *
     * @return {@code MessageBodyWorkers} instance.
     */
    public MessageBodyWorkers getMessageBodyWorkers() {
        requireNonNull(messageBodyWorkers, MessageBodyWorkers.class);
        return messageBodyWorkers;
    }

    /**
     * Sets an instance of {@link MessageBodyWorkers}.
     *
     * @param messageBodyWorkers {@code MessageBodyWorkers} instance.
     */
    public void setMessageBodyWorkers(MessageBodyWorkers messageBodyWorkers) {
        this.messageBodyWorkers = messageBodyWorkers;
    }

    /**
     * Gets an instance of {@link Configuration}.
     *
     * @return {@code Configuration} instance.
     */
    public Configuration getConfiguration() {
        requireNonNull(configuration, Configuration.class);
        return configuration;
    }

    /**
     * Sets an instance of {@link Configuration}.
     *
     * @param configuration {@code Configuration} instance.
     */
    public void setConfiguration(Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Gets an instance of {@link ExceptionMappers}.
     *
     * @return {@code ExceptionMappers} instance.
     */
    public ExceptionMappers getExceptionMappers() {
        requireNonNull(exceptionMappers, ExceptionMappers.class);
        return exceptionMappers;
    }

    /**
     * Sets an instance of {@link ExceptionMappers}.
     *
     * @param exceptionMappers {@code ExceptionMappers} instance.
     */
    public void setExceptionMappers(ExceptionMappers exceptionMappers) {
        this.exceptionMappers = exceptionMappers;
    }

    /**
     * Gets an instance of {@link ContextResolvers}.
     *
     * @return {@code ContextResolvers} instance.
     */
    public ContextResolvers getContextResolvers() {
        requireNonNull(contextResolvers, ContextResolvers.class);
        return contextResolvers;
    }

    /**
     * Sets an instance of {@link ContextResolvers}.
     *
     * @param contextResolvers {@code ContextResolvers} instance.
     */
    public void setContextResolvers(ContextResolvers contextResolvers) {
        this.contextResolvers = contextResolvers;
    }

    /**
     * Check whether the value is not {@code null} that means that the proper {@link BootstrapConfigurator} has not been configured
     * or in a wrong order.
     *
     * @param object tested object.
     * @param type   type of the tested object.
     */
    protected static void requireNonNull(Object object, Type type) {
        Objects.requireNonNull(object, type + " has not been added into BootstrapBag yet");
    }
}
