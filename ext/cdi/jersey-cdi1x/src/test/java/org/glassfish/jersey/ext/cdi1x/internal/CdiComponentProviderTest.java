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
package org.glassfish.jersey.ext.cdi1x.internal;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.lang.reflect.Type;

import javax.ws.rs.GET;
import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.ext.MessageBodyReader;

import org.junit.Test;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

/**
 * Test for {@link CdiComponentProvider}.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class CdiComponentProviderTest {

    public static class MyMessageBodyReader implements MessageBodyReader {

        @Override
        public boolean isReadable(final Class type,
                                  final Type genericType,
                                  final Annotation[] annotations,
                                  final MediaType mediaType) {
            return true;
        }

        @Override
        public Object readFrom(final Class type,
                               final Type genericType,
                               final Annotation[] annotations,
                               final MediaType mediaType,
                               final MultivaluedMap httpHeaders,
                               final InputStream entityStream) throws IOException, WebApplicationException {
            return new Object();
        }
    }

    public static class MyOtherMessageBodyReader extends MyMessageBodyReader {
    }

    public static class MyPojo {
    }

    public static class LocatorSubResource {

        @Path("/")
        public Object locator() {
            return this;
        }
    }

    @Path("/")
    public static class ResourceMethodResource {

        @GET
        public Object get() {
            return this;
        }
    }

    public static class ResourceMethodSubResource {

        @GET
        public Object get() {
            return this;
        }
    }

    @Target({ElementType.METHOD})
    @Retention(RetentionPolicy.RUNTIME)
    @HttpMethod(HttpMethod.DELETE)
    public @interface BINGO {
    }

    public static class CustomResourceMethodSubResource {

        @BINGO
        public Object get() {
            return this;
        }
    }

    /**
     * Test provider detection.
     */
    @Test
    public void testProviders() {
        final CdiComponentProvider provider = new CdiComponentProvider();
        assertFalse(provider.isJaxRsComponentType(MyPojo.class));
        assertTrue(provider.isJaxRsComponentType(MyMessageBodyReader.class));
        assertTrue(provider.isJaxRsComponentType(MyOtherMessageBodyReader.class));
    }

    /**
     * Test sub-resource detection.
     */
    @Test
    public void testResources() {
        final CdiComponentProvider provider = new CdiComponentProvider();
        assertTrue(provider.isJaxRsComponentType(LocatorSubResource.class));
        assertTrue(provider.isJaxRsComponentType(ResourceMethodResource.class));
        assertTrue(provider.isJaxRsComponentType(ResourceMethodSubResource.class));
        assertTrue(provider.isJaxRsComponentType(CustomResourceMethodSubResource.class));
    }
}
