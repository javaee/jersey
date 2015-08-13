/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.Request;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.ClassTypePair;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.server.spi.internal.ParameterValueHelper;

import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;

/**
 * A common interface for invocable resource components. This includes resource
 * methods, sub-resource methods and sub-resource locators bound to a concrete
 * handler class and a Java method (either directly or indirectly) declared &
 * implemented by the handler class.
 * <p/>
 * Invocable component information is used at runtime by a Java method dispatcher
 * when processing requests.
 * <p>
 * Class defines two kinds of {@link Method java methods}: {@link #getDefinitionMethod() definition method} and
 * {@link #getHandlingMethod() handling method}. Definition method is the java {@code Method} that is defined
 * by the user to be
 * executed. This can be java {@code Method} of the class but also method of the interface. If it is the
 * method of the interface (method handler class is the {@code class} but method itself is from
 * the {@code interface}) then the definition method is the method from the inherited {@code class}. In other words, the
 * handling method is the concrete method but definition method can be its parent abstract definition. However, in most
 * cases these methods are the same.
 * </p>
 *
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see ResourceMethod
 * @see org.glassfish.jersey.server.spi.internal.ResourceMethodDispatcher
 */
public final class Invocable implements Parameterized, ResourceModelComponent {

    /**
     * Method instance representing the {@link Inflector#apply(Object)} method.
     */
    static final Method APPLY_INFLECTOR_METHOD = initApplyMethod();

    private static Method initApplyMethod() {
        try {
            return Inflector.class.getMethod("apply", Object.class);
        } catch (NoSuchMethodException e) {
            IncompatibleClassChangeError error = new IncompatibleClassChangeError("Inflector.apply(Object) method not found");
            error.initCause(e);
            throw error;
        }
    }

    /**
     * Create a new resource method invocable model backed by an inflector instance.
     *
     * @param inflector inflector processing the request method.
     *
     * @return Invocable.
     */
    public static <T> Invocable create(Inflector<Request, T> inflector) {
        return create(MethodHandler.create(inflector), APPLY_INFLECTOR_METHOD, false);
    }

    /**
     * Create a new resource method invocable model backed by an inflector class.
     *
     * @param inflectorClass inflector syb-type processing the request method.
     * @return Invocable.
     */
    public static Invocable create(Class<? extends Inflector> inflectorClass) {
        return create(MethodHandler.create(inflectorClass), APPLY_INFLECTOR_METHOD, false);
    }

    /**
     * Create a new resource method invocable model. Parameter values will be
     * automatically decoded.
     *
     * @param handler        resource method handler.
     * @param handlingMethod handling Java method.
     * @return Invocable.
     */
    public static Invocable create(MethodHandler handler, Method handlingMethod) {
        return create(handler, handlingMethod, false);
    }

    /**
     * Create a new resource method invocable model.
     *
     * @param handler           resource method handler.
     * @param definitionMethod  method that is defined to be executed on the {@code handler}.
     * @param encodedParameters {@code true} if the automatic parameter decoding
     *                          should be disabled, false otherwise.
     * @return Invocable
     */
    public static Invocable create(MethodHandler handler, Method definitionMethod, boolean encodedParameters) {
        return create(handler, definitionMethod, null, encodedParameters);
    }

    /**
     * Create a new resource method invocable model.
     *
     * @param handler           resource method handler.
     * @param definitionMethod  method that is defined to be executed on the {@code handler}.
     * @param handlingMethod    specific and concrete method to be actually executed as a resource method. If {@code null}
     *                          then the {@code definitionMethod} will be used.
     * @param encodedParameters {@code true} if the automatic parameter decoding
     *                          should be disabled, false otherwise.
     * @return Invocable.
     */
    public static Invocable create(MethodHandler handler, Method definitionMethod, Method handlingMethod,
                                   boolean encodedParameters) {
        return new Invocable(handler, definitionMethod, handlingMethod, encodedParameters, null);
    }

    /**
     * Create a new resource method invocable model.
     *
     * @param handler           resource method handler.
     * @param definitionMethod  method that is defined to be executed on the {@code handler}.
     * @param handlingMethod    specific and concrete method to be actually executed as a resource method. If {@code null}
     *                          then the {@code definitionMethod} will be used.
     * @param encodedParameters {@code true} if the automatic parameter decoding
     *                          should be disabled, false otherwise.
     * @param routingResponseType response type that will be used during the routing for purpose
     *                            of selection of the resource method to be executed. If this parameter is
     *                            non-{@code null} then it will override the return type of the
     *                            {@link #getHandlingMethod() the Java handling method}) for purposes of
     *                            resource method selection. This might be useful in cases when resource
     *                            method returns a type {@code A} but thanks to registered providers
     *                            (eg. {@link javax.ws.rs.ext.WriterInterceptor}) it will be always converted
     *                            to type {@code B}. Then the method selecting algorithm would check presence of
     *                            {@link javax.ws.rs.ext.MessageBodyWriter} for type {@code A} (which will
     *                            never be actually needed) and might result in choosing undesired method.
     *                            If the parameter is {@code null} then the default response type will be used.
     *
     * @return Invocable.
     */
    public static Invocable create(MethodHandler handler, Method definitionMethod, Method handlingMethod,
                                   boolean encodedParameters, Type routingResponseType) {
        return new Invocable(handler, definitionMethod, handlingMethod, encodedParameters, routingResponseType);
    }

    private final MethodHandler handler;
    private final Method definitionMethod;
    private final Method handlingMethod;
    private final List<Parameter> parameters;

    private final Class<?> rawResponseType;
    private final Type responseType;
    private final Type routingResponseType;
    private final Class<?> rawRoutingResponseType;

    private Invocable(MethodHandler handler, Method definitionMethod, Method handlingMethod, boolean encodedParameters,
                      Type routingResponseType) {
        this.handler = handler;
        this.definitionMethod = definitionMethod;
        this.handlingMethod = handlingMethod == null ? ReflectionHelper
                .findOverridingMethodOnClass(handler.getHandlerClass(), definitionMethod) : handlingMethod;

        final Class<?> handlerClass = handler.getHandlerClass();
        final Class<?> definitionClass = definitionMethod.getDeclaringClass();
        final ClassTypePair handlingCtPair = ReflectionHelper.resolveGenericType(
                handlerClass,
                this.handlingMethod.getDeclaringClass(),
                this.handlingMethod.getReturnType(),
                this.handlingMethod.getGenericReturnType());

        // here we need to find types also for definition method. Definition method is in most
        // cases used for parent methods (for example for interface method of resource class). But here we
        // consider also situation when resource is a proxy (for example proxy of EJB) and definition
        // method is the original method and handling method is method on proxy. So, we try to find generic
        // type in the original class using definition method.
        final ClassTypePair definitionCtPair = ReflectionHelper.resolveGenericType(
                definitionClass,
                this.definitionMethod.getDeclaringClass(),
                this.definitionMethod.getReturnType(),
                this.definitionMethod.getGenericReturnType());
        this.rawResponseType = handlingCtPair.rawClass();
        final boolean handlerReturnTypeIsParameterized = handlingCtPair.type() instanceof ParameterizedType;
        final boolean definitionReturnTypeIsParameterized = definitionCtPair.type() instanceof ParameterizedType;
        this.responseType =
                (handlingCtPair.rawClass() == definitionCtPair.rawClass()
                        && definitionReturnTypeIsParameterized && !handlerReturnTypeIsParameterized)
                        ? definitionCtPair.type() : handlingCtPair.type();
        if (routingResponseType == null) {
            this.routingResponseType = responseType;
            this.rawRoutingResponseType = rawResponseType;
        } else {
            GenericType routingResponseGenericType = new GenericType(routingResponseType);
            this.routingResponseType = routingResponseGenericType.getType();
            this.rawRoutingResponseType = routingResponseGenericType.getRawType();
        }

        this.parameters = Collections.unmodifiableList(Parameter.create(
                handlerClass, definitionMethod.getDeclaringClass(), definitionMethod, encodedParameters));
    }

    /**
     * Get the model of the resource method handler that will be used to invoke
     * the {@link #getHandlingMethod() handling resource method} on.
     *
     * @return resource method handler model.
     */
    public MethodHandler getHandler() {
        return handler;
    }

    /**
     * Getter for the Java method
     *
     * @return corresponding Java method
     */
    public Method getHandlingMethod() {
        return handlingMethod;
    }

    /**
     * Getter for the Java method that should be executed.
     *
     * @return corresponding Java method.
     */
    public Method getDefinitionMethod() {
        return definitionMethod;
    }

    /**
     * Get the resource method generic response type information.
     * <p>
     * The returned value provides the Type information that contains additional
     * generic declaration information for generic Java class types.
     * </p>
     *
     * @return resource method generic response type information.
     */
    public Type getResponseType() {
        return responseType;
    }

    /**
     * Get the resource method raw response type.
     * <p>
     * The returned value provides information about the raw Java class.
     * </p>
     *
     * @return resource method raw response type information.
     */
    public Class<?> getRawResponseType() {
        return rawResponseType;
    }

    /**
     * Check if the invocable represents an {@link Inflector#apply(Object) inflector
     * processing method}.
     *
     * @return {@code true}, if this invocable represents an inflector invocation,
     *         {@code false} otherwise.
     */
    public boolean isInflector() {
        // Method.equals(...) does not perform the identity check (in Java SE 6)
        return APPLY_INFLECTOR_METHOD == definitionMethod || APPLY_INFLECTOR_METHOD.equals(definitionMethod);
    }

    /**
     * Returns list of {@link org.glassfish.jersey.server.spi.internal.ValueFactoryProvider value providers} which provides
     * values for parameters of this Invocable returned by {@link #getParameters()}. Value providers are ordered in the same
     * order as parameters.
     *
     * @param locator HK2 service locator.
     * @return Set of value providers for this Invocable.
     */
    public List<? extends Factory<?>> getValueProviders(ServiceLocator locator) {
        return ParameterValueHelper.createValueProviders(locator, this);
    }

    @Override
    public boolean requiresEntity() {
        for (Parameter p : getParameters()) {
            if (Parameter.Source.ENTITY == p.getSource()) {
                return true;
            }
        }
        return false;
    }

    @Override
    public List<Parameter> getParameters() {
        return parameters;
    }

    @Override
    public void accept(ResourceModelVisitor visitor) {
        visitor.visitInvocable(this);
    }

    @Override
    public List<? extends ResourceModelComponent> getComponents() {
        return Arrays.asList(handler);
    }

    @Override
    public String toString() {
        return "Invocable{"
                + "handler=" + handler
                + ", definitionMethod=" + definitionMethod
                + ", parameters=" + parameters
                + ", responseType=" + responseType + '}';
    }

    /**
     * Get the response type of the {@link #handlingMethod} that will be used during
     * the routing for the purpose
     * of selection of the resource method. Returned value
     * is in most cases equal to the {@link #getResponseType() response type}.
     * If returned value is different then it overrides the response type for
     * purposes of resource method selection and will be used to look for available
     * {@link javax.ws.rs.ext.MessageBodyWriter message body writers}.
     *
     * @return Response type used for the routing.
     */
    public Type getRoutingResponseType() {
        return routingResponseType;
    }

    /**
     * Get the response {@link Class} of the {@link #handlingMethod} that will be used during
     * the routing for the purpose
     * of selection of the resource method. Returned value
     * is in most cases equal to the {@link #getResponseType() response type}.
     * If returned value is different then it overrides the response type for
     * purposes of resource method selection and will be used to look for available
     * {@link javax.ws.rs.ext.MessageBodyWriter message body writers}.
     *
     * @return Response type used for the routing.
     */
    public Class<?> getRawRoutingResponseType() {
        return rawRoutingResponseType;
    }
}
