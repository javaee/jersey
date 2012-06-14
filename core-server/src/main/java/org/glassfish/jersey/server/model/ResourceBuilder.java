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
package org.glassfish.jersey.server.model;

import java.util.Set;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.process.Inflector;

/**
 * TODO fix javadoc.
 *
 * Jersey application builder that provides programmatic API for creating
 * server-side JAX-RS / Jersey applications. The programmatic API complements
 * the annotation-based resource API defined by JAX-RS.
 * <p />
 * A typical use case for the programmatic resource binding API is demonstrated
 * by the following example:
 *
 * <pre>  JerseyApplication.Builder appBuilder = JerseyApplication.builder();
 *
 *  appBuilder.bind("a")
 *     .method("GET").to(new Inflector&lt;Request, Response&gt;() {
 *          &#64;Override
 *          public Response apply(Request data) {
 *              // ...GET "/a" request method processing
 *          }
 *      })
 *      .method("HEAD", "OPTIONS").to(new Inflector&lt;Request, Response&gt;() {
 *          &#64;Override
 *          public Response apply(Request data) {
 *              // ...HEAD & OPTIONS "/a" request methods processing
 *          }
 *      });
 *  appBuilder
 *     .bind("b")
 *         .method("GET").to(new Inflector&lt;Request, Response&gt;() {
 *              &#64;Override
 *              public Response apply(Request data) {
 *                  // ...GET "/b" request method processing
 *              }
 *          })
 *          .subPath("c")
 *             .method("GET").to(new Inflector&lt;Request, Response&gt;() {
 *                  &#64;Override
 *                  public Response apply(Request data) {
 *                      // ...GET "/b/c" request method processing
 *                  }
 *              });
 *
 *  appBuilder.build();</pre>
 *
 * The application built in the example above is equivalent to an
 * application that contains the following annotation-based resources:
 *
 * <pre>  &#64;Path("a")
 *  public class ResourceA {
 *
 *      &#64;GET
 *      public Response get(Request request) { ... }
 *
 *      &#64;OPTIONS &#64;HEAD
 *      public Response optionsAndHead(Request request) { ... }
 *  }
 *
 *  &#64;Path("b")
 *  public class ResourceB {
 *
 *      &#64;GET
 *      public Response getB(Request request) { ... }
 *
 *      &#64;Path("c")
 *      &#64;GET
 *      public Response getBC(Request request) { ... }
 *  }
 * </pre>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
// TODO remove the interface
public interface ResourceBuilder {

    /**
     * Bind a new resource to a path within the application.
     *
     * The method is an entry point to the Jersey application builder
     * fluent programmatic resource binding API. It is equivalent to placing
     * {@link javax.ws.rs.Path &#64;Path} annotation on an annotation-based
     * resource class. See the {@link Builder application builder example} for
     * more information.
     * <p />
     * When invoked, the application builder creates a new {@link BoundBuilder
     * bound resource builder} that is bound to the supplied {@code path},
     * relative to the base application URI.
     *
     * @param path resource path relative to the base application URI.
     * @return resource builder bound to the {@code path}.
     */
    BoundResourceBuilder path(String path);

    /**
     * Build new set of programmatically defined resource bindings.
     *
     * @return new set of programmatically defined resource class models.
     */
    public Set<Resource> build();

    /**
     * Represents a supported resource path to which new resource methods and
     * sub-resource locators can be attached.
     */
    public static interface BoundResourceBuilder {

        /**
         * Bind new HTTP methods to the path previously configured in this
         * {@link BoundBuilder builder}.
         * If any of the specified methods has already been bound earlier, the
         * previous method binding will be overridden.
         * <p />
         * Invoking is method is equivalent to placing a {@link javax.ws.rs.HttpMethod
         * http method meta-annotated} annotation on a resource method in an
         * annotation-based resource class. See the {@link Builder application
         * builder example} for more information.
         *
         * @param methods set of HTTP methods to be bound. Any duplicate values
         *     will be automatically discarded.
         * @return configured {@link ResourceMethodBuilder resource method builder}
         *     instance.
         */
        public ResourceMethodBuilder method(String... methods);

        /**
         * Set supported response media types (equivalent of {@link javax.ws.rs.Produces})
         * for the current path. Overrides any previously set values.
         * <p />
         * Invoking is method is equivalent to placing {@link javax.ws.rs.Produces
         * &#64;Produces} annotation on a resource class in an annotation-based
         * resource class. See the {@link Builder application builder example}
         * for more information.
         *
         * @param mediaTypes supported response media types.
         * @return {@link ResourceMethodBuilder} updated builder instance}.
         */
        public BoundResourceBuilder produces(MediaType... mediaTypes);

        /**
         * Set supported request media types (equivalent of {@link javax.ws.rs.Consumes})
         * for the current path. Overrides any previously set values.
         * <p />
         * Invoking is method is equivalent to placing {@link javax.ws.rs.Consumes
         * &#64;Consumes} annotation on a resource class in an annotation-based
         * resource class. See the {@link Builder application builder example}
         * for more information.
         *
         * @param mediaTypes supported request media types.
         * @return {@link BoundBuilder} updated builder instance}.
         */
        public BoundResourceBuilder consumes(MediaType... mediaTypes);

//        /**
//         * Append sub-path to the current path which can be used to bind new
//         * sub-resource methods and locators.
//         * <p />
//         * Invoking is method is equivalent to putting {@link javax.ws.rs.Path
//         * &#64;Path} annotation on a sub-resource method or sub-resource locator
//         * in an annotation-based resource class. See the {@link Builder application
//         * builder example} for more information.
//         *
//         * @param subPath path to be appended to the current path value.
//         * @return {@link BoundBuilder updated builder instance} bound the the
//         *     new path.
//         */
//        public BoundResourceBuilder subPath(String subPath);
    }

    /**
     * Jersey application builder used for binding a new resource method to an
     * {@link Inflector Inflector&lt;Request, Response&gt;} responsible for
     * processing requests targeted at the bound path and the particular
     * method(s).
     */
    public static interface ResourceMethodBuilder {

        /**
         * Bind previously specified method(s) to provided request-to-response
         * {@link Inflector inflector} instance.
         * <p />
         * Invoking is method is equivalent to defining a resource method
         * in an annotation-based resource class. See the {@link Builder
         * application builder example} for more information.
         *
         * @param inflector request to response transformation implemented
         *     as an {@link Inflector Inflector&lt;Request, Response&gt;}.
         * @return updated resource builder.
         */
        public BoundResourceBuilder to(Inflector<Request, Response> inflector);

        /**
         * Bind previously specified method(s) to provided request-to-response
         * {@link Inflector inflector} class.
         * <p />
         * Invoking is method is equivalent to defining a resource method
         * in an annotation-based resource class. See the {@link Builder
         * application builder example} for more information.
         *
         * @param inflectorClass request to response transformation implemented
         *     as an {@link Inflector Inflector&lt;Request, Response&gt;}.
         * @return updated resource builder.
         */
        public BoundResourceBuilder to(Class<? extends Inflector<Request, Response>> inflectorClass);

        /**
         * Set supported response media types on a resource method.
         * Overrides any previously set values.
         * <p />
         * Invoking is method is equivalent to placing {@link javax.ws.rs.Produces
         * &#64;Produces} annotation on a resource method in an annotation-based
         * resource class. See the {@link Builder application builder example}
         * for more information.
         *
         * @param mediaTypes supported response media types.
         * @return {@link ResourceMethodBuilder} updated builder instance}.
         */
        public ResourceMethodBuilder produces(MediaType... mediaTypes);

        /**
         * Set accepted request media types on a resource method.
         * Overrides any previously set values.
         * <p />
         * Invoking is method is equivalent to placing {@link javax.ws.rs.Consumes
         * &#64;Consumes} annotation on a resource method in an annotation-based
         * resource class. See the {@link Builder application builder example}
         * for more information.
         *
         * @param mediaTypes accepted request media types.
         * @return {@link ResourceMethodBuilder} updated builder instance}.
         */
        public ResourceMethodBuilder consumes(MediaType... mediaTypes);
    }
}
