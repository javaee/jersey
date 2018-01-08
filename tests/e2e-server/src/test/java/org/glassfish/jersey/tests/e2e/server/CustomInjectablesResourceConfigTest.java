/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;

import javax.inject.Inject;
import javax.inject.Qualifier;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.AnnotationLiteral;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class CustomInjectablesResourceConfigTest extends JerseyTest {

    public static class MyHK2Binder extends AbstractBinder {

        @Override
        protected void configure() {
            // request scope binding
            bindAsContract(MyInjectablePerRequest.class).in(RequestScoped.class);

            // singleton binding
            bindAsContract(MyInjectableSingleton.class).in(Singleton.class);

            // singleton instance binding
            bind(new MyInjectableSingleton()).to(MyInjectableSingleton.class);

            // request scope binding with specified custom annotation
            bindAsContract(MyInjectablePerRequest.class).qualifiedBy(new MyQualifierImpl()).in(RequestScoped.class);
        }
    }

    public static class MyInjectablePerRequest {
        public int i = 0;
    }

    @Singleton
    public static class MyInjectableSingleton {
        public int i = 0;
    }

    @Retention(java.lang.annotation.RetentionPolicy.RUNTIME)
    @Target(ElementType.FIELD)
    @Qualifier
    public static @interface MyQualifier {

    }

    private static class MyQualifierImpl extends AnnotationLiteral<MyQualifier> implements MyQualifier {
    }

    @Path("/")
    public static class Resource {
        @Inject
        MyInjectablePerRequest myInjectablePerRequest;

        @Inject
        MyInjectableSingleton myInjectableSingleton;

        @Inject
        @MyQualifier
        MyInjectablePerRequest myInjectablePerRequest2;

        @GET
        @Path("/perrequest")
        public String getAndIncPerRequest() {
            return Integer.valueOf(++myInjectablePerRequest.i).toString();
        }

        @GET
        @Path("/perrequestCustomQualifier")
        public String getAndIncPerRequest2() {
            return Integer.valueOf(++myInjectablePerRequest2.i).toString();
        }

        @GET
        @Path("/singleton")
        @Produces("text/plain")
        public String getAndIncSingleton() {
            System.out.println(myInjectableSingleton);
            return Integer.valueOf(++myInjectableSingleton.i).toString();
        }
    }

    @Override
    protected Application configure() {
        ResourceConfig rc = new ResourceConfig();
        rc.registerClasses(Resource.class);
        rc.register(new MyHK2Binder());

        return rc;
    }

    @Test
    public void testPerRequest() throws Exception {
        final javax.ws.rs.client.WebTarget perRequest = target().path("perrequest");

        assertEquals("1", perRequest.request().get(String.class));
        assertEquals("1", perRequest.request().get(String.class));
        assertEquals("1", perRequest.request().get(String.class));
    }

    @Test
    public void testSingleton() throws Exception {
        final javax.ws.rs.client.WebTarget perRequest = target().path("singleton");

        assertEquals("1", perRequest.request().get(String.class));
        assertEquals("2", perRequest.request().get(String.class));
        assertEquals("3", perRequest.request().get(String.class));
    }

    @Test
    public void testCustomAnnotation() throws Exception {
        final javax.ws.rs.client.WebTarget perRequestCustomAnnotation = target().path("perrequestCustomQualifier");

        assertEquals("1", perRequestCustomAnnotation.request().get(String.class));
        assertEquals("1", perRequestCustomAnnotation.request().get(String.class));
        assertEquals("1", perRequestCustomAnnotation.request().get(String.class));
    }
}
