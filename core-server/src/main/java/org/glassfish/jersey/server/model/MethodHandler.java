/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.model;

import java.lang.reflect.Constructor;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.Encoded;

import org.glassfish.jersey.internal.inject.Injections;

import org.glassfish.hk2.api.ServiceLocator;


/**
 * Resource method handler model.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class MethodHandler implements ResourceModelComponent {

    /**
     * Create a class-based method handler from a class.
     *
     * @param handlerClass method handler class.
     * @return new class-based method handler.
     */
    public static MethodHandler create(final Class<?> handlerClass) {
        return new ClassBasedMethodHandler(handlerClass);
    }

    /**
     * Create a class-based method handler from a class.
     *
     * @param handlerClass         method handler class.
     * @param disableParamDecoding if set to {@code true}, any injected constructor
     *                             parameters must be kept encoded and must not be automatically decoded.
     * @return new class-based method handler.
     */
    public static MethodHandler create(final Class<?> handlerClass, final boolean disableParamDecoding) {
        return new ClassBasedMethodHandler(handlerClass, disableParamDecoding);
    }

    /**
     * Create a instance-based (singleton) method handler from a class.
     *
     * @param handlerInstance method handler instance (singleton).
     * @return new instance-based method handler.
     */
    public static MethodHandler create(final Object handlerInstance) {
        return new InstanceBasedMethodHandler(handlerInstance);
    }

    /**
     * Create a instance-based (singleton) method handler from a class.
     *
     * This method
     *
     * @param handlerInstance method handler instance (singleton).
     * @param handlerClass    declared handler class.
     * @return new instance-based method handler.
     */
    public static MethodHandler create(final Object handlerInstance, Class<?> handlerClass) {
        return new InstanceBasedMethodHandler(handlerInstance, handlerClass);
    }

    /**
     * Get the resource method handler class.
     *
     * @return resource method handler class.
     */
    public abstract Class<?> getHandlerClass();

    /**
     * Get the resource method handler constructors.
     *
     * The returned is empty by default. Concrete implementations may override
     * the method to return the actual list of constructors that will be used
     * for the handler initialization.
     *
     * @return resource method handler constructors.
     */
    public List<HandlerConstructor> getConstructors() {
        return Collections.emptyList();
    }

    /**
     * Get the injected resource method handler instance.
     *
     * @param locator service locator that can be used to inject get the instance.
     * @return injected resource method handler instance.
     */
    public abstract Object getInstance(final ServiceLocator locator);

    /**
     * Return whether the method handler {@link ServiceLocator creates instances}
     * based on {@link Class classes}.
     *
     * @return True is instances returned bu this method handler are created from {@link Class classes} given to HK2, false\
     *         otherwise (for example when method handler was initialized from instance)
     */
    public abstract boolean isClassBased();

    @Override
    public List<? extends ResourceModelComponent> getComponents() {
        return null;
    }

    @Override
    public void accept(final ResourceModelVisitor visitor) {
        visitor.visitMethodHandler(this);
    }

    private static class ClassBasedMethodHandler extends MethodHandler {

        private final Class<?> handlerClass;
        private final List<HandlerConstructor> handlerConstructors;

        public ClassBasedMethodHandler(final Class<?> handlerClass) {
            this(handlerClass, handlerClass.isAnnotationPresent(Encoded.class));
        }

        public ClassBasedMethodHandler(final Class<?> handlerClass, final boolean disableParamDecoding) {
            this.handlerClass = handlerClass;

            List<HandlerConstructor> constructors = new LinkedList<HandlerConstructor>();
            for (Constructor<?> constructor : handlerClass.getConstructors()) {
                constructors.add(new HandlerConstructor(
                        constructor, Parameter.create(handlerClass, handlerClass, constructor, disableParamDecoding)));
            }
            this.handlerConstructors = Collections.unmodifiableList(constructors);
        }

        @Override
        public Class<?> getHandlerClass() {
            return handlerClass;
        }

        @Override
        public List<HandlerConstructor> getConstructors() {
            return handlerConstructors;
        }

        @Override
        public Object getInstance(final ServiceLocator locator) {
            return Injections.getOrCreate(locator, handlerClass);
        }

        @Override
        public boolean isClassBased() {
            return true;
        }

        @Override
        protected Object getHandlerInstance() {
            return null;
        }

        @Override
        public List<? extends ResourceModelComponent> getComponents() {
            return handlerConstructors;
        }

        @Override
        public String toString() {
            return "ClassBasedMethodHandler{"
                    + "handlerClass=" + handlerClass
                    + ", handlerConstructors=" + handlerConstructors + '}';
        }
    }

    private static class InstanceBasedMethodHandler extends MethodHandler {

        private final Object handler;
        private final Class<?> handlerClass;

        public InstanceBasedMethodHandler(final Object handler) {
            this.handler = handler;
            this.handlerClass = handler.getClass();
        }

        public InstanceBasedMethodHandler(final Object handler, final Class<?> handlerClass) {
            this.handler = handler;
            this.handlerClass = handlerClass;
        }

        @Override
        public Class<?> getHandlerClass() {
            return handlerClass;
        }

        @Override
        protected Object getHandlerInstance() {
            return handler;
        }

        @Override
        public Object getInstance(final ServiceLocator locator) {
            return handler;
        }

        @Override
        public boolean isClassBased() {
            return false;
        }

        @Override
        public String toString() {
            return "InstanceBasedMethodHandler{"
                    + "handler=" + handler
                    + ", handlerClass=" + handlerClass
                    + '}';
        }
    }

    /**
     * Get the raw handler instance that is backing this method handler.
     *
     * @return raw handler instance. May return {@code null} if the handler is
     *         {@link #isClassBased() class-based}.
     */
    protected abstract Object getHandlerInstance();
}
