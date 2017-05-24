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

package org.glassfish.jersey.inject.hk2;

import java.lang.annotation.Annotation;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.ForeignDescriptor;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.RequestScoped;

import org.glassfish.hk2.api.ActiveDescriptor;
import org.glassfish.hk2.api.Context;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * Class is able to communicate with {@link RequestScope} and provide request-scoped descriptors to HK2 DI provider to create or
 * destroy instances.
 */
@Singleton
public class RequestContext implements Context<RequestScoped> {

    private final RequestScope requestScope;

    @Inject
    public RequestContext(RequestScope requestScope) {
        this.requestScope = requestScope;
    }

    @Override
    public Class<? extends Annotation> getScope() {
        return RequestScoped.class;
    }

    @Override
    public <U> U findOrCreate(ActiveDescriptor<U> activeDescriptor, ServiceHandle<?> root) {
        Hk2RequestScope.Instance instance = (Hk2RequestScope.Instance) requestScope.current();

        U retVal = instance.get(ForeignDescriptor.wrap(activeDescriptor));
        if (retVal == null) {
            retVal = activeDescriptor.create(root);
            instance.put(ForeignDescriptor.wrap(activeDescriptor, obj -> activeDescriptor.dispose((U) obj)), retVal);
        }
        return retVal;
    }

    @Override
    public boolean containsKey(ActiveDescriptor<?> descriptor) {
        Hk2RequestScope.Instance instance = (Hk2RequestScope.Instance) requestScope.current();
        return instance.contains(ForeignDescriptor.wrap(descriptor));
    }

    @Override
    public boolean supportsNullCreation() {
        return true;
    }

    @Override
    public boolean isActive() {
        return requestScope.isActive();
    }

    @Override
    public void destroyOne(ActiveDescriptor<?> descriptor) {
        Hk2RequestScope.Instance instance = (Hk2RequestScope.Instance) requestScope.current();
        instance.remove(ForeignDescriptor.wrap(descriptor));
    }

    @Override
    public void shutdown() {
        requestScope.shutdown();
    }

    /**
     * Request scope injection binder.
     */
    public static class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindAsContract(RequestContext.class)
                    .to(new TypeLiteral<Context<RequestScoped>>() {}.getType())
                    .in(Singleton.class);
        }
    }
}