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

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.ForeignDescriptorImpl;
import org.glassfish.jersey.internal.inject.InjecteeImpl;
import org.glassfish.jersey.internal.util.ReflectionHelper;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;

/**
 * This class wraps the jersey class {@link org.glassfish.jersey.internal.inject.InjectionResolver} to make HK2 version of this
 * provided functionality. HK2 {@link InjectionResolver} can be then register in {@link org.glassfish.hk2.api.ServiceLocator} and
 * HK2 can handle the annotation which is register along with the interface.
 */
@Singleton
public class InjectionResolverWrapper<T extends Annotation> implements InjectionResolver<T> {

    private final org.glassfish.jersey.internal.inject.InjectionResolver jerseyResolver;

    /**
     * C'tor accepts jersey-like {@code InjectionResolver} on which the the processing is delegated.
     *
     * @param jerseyResolver jersey injection resolver.
     */
    InjectionResolverWrapper(org.glassfish.jersey.internal.inject.InjectionResolver<T> jerseyResolver) {
        this.jerseyResolver = jerseyResolver;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle root) {
        InjecteeImpl injecteeWrapper = new InjecteeImpl();
        injecteeWrapper.setRequiredType(injectee.getRequiredType());
        injecteeWrapper.setParent(injectee.getParent());
        injecteeWrapper.setRequiredQualifiers(injectee.getRequiredQualifiers());
        injecteeWrapper.setOptional(injectee.isOptional());
        injecteeWrapper.setPosition(injectee.getPosition());
        injecteeWrapper.setFactory(ReflectionHelper.isSubClassOf(injectee.getRequiredType(), Factory.class));
        injecteeWrapper.setInjecteeDescriptor(new ForeignDescriptorImpl(injectee.getInjecteeDescriptor()));

        Object instance = jerseyResolver.resolve(injecteeWrapper);
        if (injecteeWrapper.isFactory()) {
            return asFactory(instance);
        } else {
            return instance;
        }
    }

    private Factory asFactory(Object instance) {
        return new Factory() {
            @Override
            public Object provide() {
                return instance;
            }

            @Override
            public void dispose(final Object instance) {
            }
        };
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return jerseyResolver.isConstructorParameterIndicator();
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return jerseyResolver.isMethodParameterIndicator();
    }
}
