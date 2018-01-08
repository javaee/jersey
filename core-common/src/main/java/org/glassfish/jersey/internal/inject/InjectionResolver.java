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

package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;

/**
 * This class allows users to provide a custom injection target for any annotation (including {@code &#64;Inject}). The user would
 * usually only provide a resolver for {@code &#64;Inject} if it were specializing the system provided resolver for
 * {@code &#64;Inject}. Otherwise, this resolver can be used to provide injection points for any annotation.
 * <p>
 * Jersey provides all {@code InjectionResolvers} for JAX-RS annotation and {@code org.glassfish.jersey.server.Uri} apart from
 * {@link javax.ws.rs.core.Context} which must be implemented and registered directly as a part of DI integration because of
 * many optimization which cannot be implemented on Jersey side.
 * <p>
 * The {@code InjectionResolvers} are delivered to DI integration using {@link InjectionManager#register(Binder)} and DI provider
 * just filter {@link InjectionResolverBinding} and internally register the annotation handling using its own mechanism.
 *
 * @param <T> This must be the annotation class of the injection annotation that this resolver will handle.
 */
public interface InjectionResolver<T extends Annotation> {

    /**
     * This method will return the object that should be injected into the given injection point.  It is the responsibility of the
     * implementation to ensure that the object returned can be safely injected into the injection point.
     * <p>
     * This method should not do the injection themselves.
     *
     * @param injectee The injection point this value is being injected into
     * @return A possibly null value to be injected into the given injection point
     */
    Object resolve(Injectee injectee);

    /**
     * This method should return true if the annotation that indicates that this is an injection point can appear in the parameter
     * list of a constructor.
     *
     * @return true if the injection annotation can appear in the parameter list of a constructor.
     */
    boolean isConstructorParameterIndicator();

    /**
     * This method should return true if the annotation that indicates that this is an injection point can appear in the parameter
     * list of a method.
     *
     * @return true if the injection annotation can appear in the parameter list of a method.
     */
    boolean isMethodParameterIndicator();

    /**
     * This method returns the annotation for what the injection resolver is implemented.
     *
     * @return handled annotation by injection resolver.
     */
    Class<T> getAnnotation();

}
