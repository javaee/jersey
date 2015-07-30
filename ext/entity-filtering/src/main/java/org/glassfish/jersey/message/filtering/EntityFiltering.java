/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.message.filtering;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Meta-annotation used to create entity filtering annotations for entity (model) classes and resource methods and resources.
 * <p>
 * Entity Data Filtering via annotations is supposed to be used to annotate:
 * <ul>
 * <li>entity classes (supported on both, server and client sides), and</li>
 * <li>resource methods / resource classes (server side)</li>
 * </ul>
 * </p>
 * <p>
 * In entity filtering, a <i>entity-filtering</i> annotation is first defined using the {@code @EntityFiltering} meta-annotation:
 * <pre>
 *  &#64;Target({ ElementType.TYPE, ElementType.METHOD, ElementType.FIELD })
 *  &#64;Retention(value = RetentionPolicy.RUNTIME)
 *  <b>&#64;EntityFiltering</b>
 *  <b>public @interface DetailedView</b> {
 *
 *      public static class Factory extends <b>AnnotationLiteral&lt;DetailedView&gt;</b> implements <b>DetailedView</b> {
 *
 *         public static <b>DetailedView</b> get() {
               return new Factory();
           }
 *      }
 *  }
 * </pre>
 * </p>
 * <p>
 * Entity-filtering annotation should provide a factory class/method to create an instance of the annotation. Example of such
 * factory can be seen in the {@code DetailedView} above. Such instances can be then passed to the client/server runtime to
 * define/override entity-filtering scopes.
 * </p>
 * <p>
 * The defined entity-filtering annotation is then used to decorate a entity, it's property accessors or fields (more than one
 * entity may be decorated with the same entity-filtering annotation):
 * <pre>
 *  public class MyEntityClass {
 *
 *      <b>&#64;DetailedView</b>
 *      private String myField;
 *
 *      ...
 *  }
 * </pre>
 * </p>
 * <p>
 * At last, on the server-side, the entity-filtering annotations are applied to the resource or resource method(s) to which the
 * entity-filtering should be applied:
 * <pre>
 *  &#64;Path("/")
 *  public class MyResourceClass {
 *
 *      &#64;GET
 *      &#64;Produces("text/plain")
 *      &#64;Path("{id}")
 *      <b>&#64;DetailedView</b>
 *      public MyEntityClass get(@PathParam("id") String id) {
 *          // Return MyEntityClass.
 *      }
 *  }
 * </pre>
 * </p>
 * <p>
 * At last, on the client-side, the entity-filtering annotations are passed to the runtime via
 * {@link javax.ws.rs.client.Entity#entity(Object, javax.ws.rs.core.MediaType, java.lang.annotation.Annotation[]) Entity.entity()}
 * method and the entity-filtering scopes are then derived from the annotations:
 * <pre>
 *  ClientBuilder.newClient()
 *      .target("resource")
 *      .request()
 *      .post(Entity.entity(myentity, "application/json", <b>new Annotation[] {MyEntityClass.Factory.get()}</b>));
 * </pre>
 * </p>
 *
 * @author Michal Gajdos
 */
@Target(ElementType.ANNOTATION_TYPE)
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface EntityFiltering {
}
