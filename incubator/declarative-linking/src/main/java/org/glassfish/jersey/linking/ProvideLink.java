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

package org.glassfish.jersey.linking;

import java.lang.annotation.ElementType;
import java.lang.annotation.Repeatable;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.net.URI;

import javax.ws.rs.core.Link;

import org.glassfish.jersey.Beta;

/**
 * Use this on resource methods to contribute links to a representation.
 *
 * It is the inverse of {@link InjectLink} instead of annotating the target you annotate the source of the links.
 * The added benefit is that since you annotate the method you don't need to specify the path to it.
 *
 * <p>
 * <pre>
 * &#64;ProvideLink(value = Order.class, rel = "self", bindings = @Binding(name = "orderId", value = "${instance.id}"))
 * &#64;ProvideLink(value = PaymentConfirmation.class, rel = "order",
 *                  bindings = @Binding(name = "orderId", value = "${instance.orderId}"))
 * public Response get(@PathParam("orderId") String orderId) { ...
 * </pre>
 * </p>
 *
 * It can also be used as a meta annotation, see the Javadoc of {@link InheritFromAnnotation} for details.
 *
 * @author Leonard Brünings
 */
@Target({ ElementType.METHOD, ElementType.TYPE})
@Repeatable(ProvideLinks.class)
@Retention(RetentionPolicy.RUNTIME)
@Beta
public @interface ProvideLink {

    /**
     * The style of URI to inject
     */
    InjectLink.Style style() default InjectLink.Style.DEFAULT;

    /**
     * Provide links for representation classes listed here.
     *
     * May use {@link InheritFromAnnotation} for Meta-Annotations
     */
    Class<?>[] value();

    /**
     * Specifies the bindings for embedded URI template parameters.
     *
     * @see Binding
     */
    Binding[] bindings() default {};

    /**
     * Specifies a boolean EL expression whose value determines whether a Ref is
     * set (true) or not (false). Omission of a condition will always insert a
     * ref.
     */
    String condition() default "";

    //
    // Link properties
    //

    /**
     * Specifies the relationship.
     */
    String rel() default "";

    /**
     * Specifies the reverse relationship.
     */
    String rev() default "";

    /**
     * Specifies the media type.
     */
    String type() default "";

    /**
     * Specifies the title.
     */
    String title() default "";

    /**
     * Specifies the anchor
     */
    String anchor() default "";

    /**
     * Specifies the media
     */
    String media() default "";

    /**
     * Specifies the lang of the referenced resource
     */
    String hreflang() default "";

    /**
     * Specifies extension parameters as name-value pairs.
     */
    InjectLink.Extension[] extensions() default {};


    class Util {

        static Link buildLinkFromUri(URI uri, ProvideLink link) {

            javax.ws.rs.core.Link.Builder builder = javax.ws.rs.core.Link.fromUri(uri);
            if (!link.rel().isEmpty()) {
                builder = builder.rel(link.rel());
            }
            if (!link.rev().isEmpty()) {
                builder = builder.param("rev", link.rev());
            }
            if (!link.type().isEmpty()) {
                builder = builder.type(link.type());
            }
            if (!link.title().isEmpty()) {
                builder = builder.param("title", link.title());
            }
            if (!link.anchor().isEmpty()) {
                builder = builder.param("anchor", link.anchor());
            }
            if (!link.media().isEmpty()) {
                builder = builder.param("media", link.media());
            }
            if (!link.hreflang().isEmpty()) {
                builder = builder.param("hreflang", link.hreflang());
            }
            for (InjectLink.Extension ext : link.extensions()) {
                builder = builder.param(ext.name(), ext.value());
            }
            return builder.build();
        }
    }

    /**
     * Special interface to indicate that the target should be inherited from the annotated annotation.
     * <p>
     * <pre>
     * &#64;ProvideLinks({
     *   &#64;ProvideLink(value = ProvideLink.InheritFromAnnotation.class, rel = "next", bindings = {
     *       &#64;Binding(name = "page", value = "${instance.number + 1}"),
     *       &#64;Binding(name =&#64; "size", value = "${instance.size}"),
     *     },
     *     condition = "${instance.nextPageAvailable}"),
     *   &#64;ProvideLink(value = ProvideLink.InheritFromAnnotation.class, rel = "prev", bindings = {
     *       &#64;Binding(name = "page", value = "${instance.number - 1}"),
     *       &#64;Binding(name = "size", value = "${instance.size}"),
     *     },
     *     condition = "${instance.previousPageAvailable}")
     * })
     * &#64;Target({ElementType.METHOD})
     * &#64;Retention(RetentionPolicy.RUNTIME)
     * &#64;Documented
     * public &#64;interface PageLinks {
     * Class<?> value();
     * }
     * </pre>
     * </p>
     * <p>
     * In this case the value of each {@link ProvideLink} will be the same as {@code PageLinks} value.
     * </p>
     */
    interface InheritFromAnnotation{}
}
