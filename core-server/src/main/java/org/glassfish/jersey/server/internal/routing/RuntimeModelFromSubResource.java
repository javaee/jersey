/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.routing;

import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.uri.PathPattern;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.model.InflectorBasedResourceMethod;
import org.glassfish.jersey.server.model.ResourceClass;
import org.glassfish.jersey.server.model.ResourceConstructor;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.SubResourceLocator;
import org.glassfish.jersey.server.model.SubResourceMethod;


/**
 * Constructs runtime model for a sub-resource.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class RuntimeModelFromSubResource extends RuntimeModelProviderBase {

    /* package */ RuntimeModelFromSubResource(MessageBodyWorkers msgBodyWorkers) {
        super(msgBodyWorkers);
    }

    @Override
    TreeAcceptor createFinalTreeAcceptor(RouterModule.RootRouteBuilder<PathPattern> rootRouteBuilder,
                                         RouterModule.RouteToPathBuilder<PathPattern> lastRoutedBuilder) {
        return lastRoutedBuilder.build();
    }

    @Override
    public void visitResourceClass(ResourceClass resource) {
        // no need to do anything here
    }

    @Override
    public void visitResourceMethod(final ResourceMethod method) {
        addMethodInflector(PathPattern.EMPTY_PATH, method, createInflector(method));
    }

    @Override
    public void visitSubResourceMethod(SubResourceMethod method) {
        addSubResourceLocatorEntry("/", new SubResourceMethodEntry(method.getHttpMethod(), method, createInflector(method)));
    }

    @Override
    public void visitSubResourceLocator(SubResourceLocator locator) {
        addSubResourceLocatorEntry("/", new SubResourceLocatorEntry(locator, new SubResourceLocatorAcceptor(injector, services, workers, locator)));
    }

    @Override
    public void visitResourceConstructor(ResourceConstructor constructor) {
    }

    @Override
    public void visitInflectorResourceMethod(InflectorBasedResourceMethod method) {
        throw new IllegalStateException("Something strange happens here. "
                +"It should not be possible to register an inflector based resource method to a sub-resource.");
    }

    @Override
    TreeAcceptor adaptSubResourceMethodAcceptor(ResourceClass resource, TreeAcceptor acceptor) {
        return new PushUriAndDelegateTreeAcceptor(injector, acceptor);
    }

    @Override
    TreeAcceptor adaptSubResourceLocatorAcceptor(ResourceClass resource, TreeAcceptor acceptor) {
        return new PushUriAndDelegateTreeAcceptor(injector, acceptor);
    }
}
