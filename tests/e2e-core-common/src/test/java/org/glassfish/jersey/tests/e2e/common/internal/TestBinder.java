/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.common.internal;

import java.util.Collections;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Response;
import javax.ws.rs.ext.ExceptionMapper;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ProviderBinder;
import org.glassfish.jersey.message.internal.MessagingBinders;

/**
 * Binder for testing purposes.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class TestBinder extends AbstractBinder {

    public static void initProviders(final InjectionManager injectionManager) {
        initProviders(injectionManager, Collections.emptySet(), Collections.emptySet());
    }

    public static void initProviders(final InjectionManager injectionManager,
                                     final Iterable<Class<?>> providerClasses,
                                     final Iterable<Object> providerInstances) {
        final ProviderBinder providerBinder = new ProviderBinder(injectionManager);
        providerBinder.bindClasses(providerClasses);
        providerBinder.bindInstances(providerInstances);
    }

    @Override
    protected void configure() {
        install(new MessagingBinders.MessageBodyProviders(null, RuntimeType.SERVER));

        bind(new ExceptionMapper<Throwable>() {
            @Override
            public Response toResponse(Throwable exception) {
                if (exception instanceof NumberFormatException) {
                    return Response.ok(-1).build();
                }

                throw new RuntimeException(exception);
            }
        }).to(ExceptionMapper.class);
    }
}
