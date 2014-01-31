/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.media.linking;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Specifies a link injection target in a returned representation bean. May be
 * used on fields of type String or URI. One of {@link #value()} or
 * {@link #resource()} must be specified.
 */
@Target(ElementType.FIELD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Ref {
    /**
     * Styles of URI supported
     */
    public enum Style {
        /**
         * An absolute URI. The URI template will be prefixed with the
         * absolute base URI of the application.
         */
        ABSOLUTE,
        
        /**
         * An absolute path. The URI template will be prefixed with the
         * absolute base path of the application.
         */
        ABSOLUTE_PATH,

        /**
         * A relative path. The URI template will be converted to a relative
         * path with no prefix.
         */
        RELATIVE_PATH

    };

    /**
     * The style of URI to inject
     */
    Style style() default Style.ABSOLUTE_PATH;

    /**
     * Specifies a URI template that will be used to build the injected URI. The
     * template may contain both URI template parameters (e.g. {id}) and EL
     * expressions (e.g. ${instance.id}) using the same implicit beans as
     * {@link Binding#value()}. URI template parameter values are resolved as
     * described in {@link #resource()}. E.g. the following three alternatives
     * are equivalent:
     * <pre>
     * &#64;Ref("{id}")
     * &#64;Ref(value="{id}", bindings={
     *   &#64;Binding(name="id" value="${instance.id}"}
     * )
     * &#64;Ref("${instance.id}")
     * </pre>
     */
    String value() default "";

    /**
     * Specifies a resource class whose @Path URI template will be used to build
     * the injected URI. Embedded URI template parameter values are resolved as
     * follows:
     * <ol>
     * <li>If the {@link #bindings()} property contains a binding
     * specification for the parameter then that is used</li>
     * <li>Otherwise an implicit binding is used that extracts the value
     * of a bean property by the same name as the URI template from the
     * implicit <code>instance</code> bean (see {@link Binding}).</li>
     * </ol>
     * <p>E.g. assuming a resource class <code>SomeResource</code> with the
     * following <code>@Path("{id}")</code> annotation, the following two
     * alternatives are therefore equivalent:</p>
     * <pre>
     * &#64;Ref(resource=SomeResource.class)
     * &#64;Ref(resource=SomeResource.class, bindings={
     *   &#64;Binding(name="id" value="${instance.id}"}
     * )
     * </pre>
     */
    Class<?> resource() default Class.class;

    /**
     * Used in conjunction with {@link #resource()} to specify a subresource
     * locator or method. The value is the name of the method. The value of
     * the method's @Path annotation will be appended to the value of the
     * class-level @Path annotation separated by '/' if necessary.
     */
    String method() default "";

    /**
     * Specifies the bindings for embedded URI template parameters.
     * @see Binding
     */
    Binding[] bindings() default {};

    /**
     * Specifies a boolean EL expression whose value determines whether a Ref
     * is set (true) or not (false). Omission of a condition will
     * always insert a ref.
     */
    String condition() default "";

}
