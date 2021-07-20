/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.tests.integration.jersey2255;

import java.lang.annotation.Annotation;
import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.inject.AnnotationLiteral;
import org.glassfish.jersey.message.filtering.EntityFiltering;

/**
 * Test resource.
 *
 * @author Eric Miles (emilesvt at gmail.com)
 */
@Path("/")
@Consumes("application/json")
@Produces("application/json")
public class Issue2255Resource {

    public static class A {

        public A() {
        }

        public A(String fieldA1) {
            this.fieldA1 = fieldA1;
        }

        private String fieldA1;

        @Detailed
        public String getFieldA1() {
            return fieldA1;
        }

        public void setFieldA1(final String fieldA1) {
            this.fieldA1 = fieldA1;
        }
    }

    public static class B extends A {

        public B() {
        }

        public B(String fieldA1, String fieldB1) {
            super(fieldA1);
            this.fieldB1 = fieldB1;
        }

        private String fieldB1;

        public String getFieldB1() {
            return fieldB1;
        }

        public void setFieldB1(final String fieldB1) {
            this.fieldB1 = fieldB1;
        }
    }

    @Target({ElementType.TYPE, ElementType.METHOD, ElementType.FIELD})
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @EntityFiltering
    public static @interface Detailed {

        /**
         * Factory class for creating instances of {@code ProjectDetailedView} annotation.
         */
        public static class Factory
                extends AnnotationLiteral<Detailed>
                implements Detailed {

            private Factory() {
            }

            public static Detailed get() {
                return new Factory();
            }
        }
    }

    @Path("A")
    @GET
    public Response getA(@QueryParam("detailed") final boolean isDetailed) {
        return Response
                .ok()
                .entity(new A("fieldA1Value"), isDetailed ? new Annotation[] {Detailed.Factory.get()} : new Annotation[0])
                .build();
    }

    @Path("B")
    @GET
    public Response getB(@QueryParam("detailed") final boolean isDetailed) {
        return Response
                .ok()
                .entity(new B("fieldA1Value", "fieldB1Value"),
                        isDetailed ? new Annotation[] {Detailed.Factory.get()} : new Annotation[0])
                .build();
    }

}
