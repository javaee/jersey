/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.exception;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.glassfish.jersey.examples.exception.ExceptionResource.MyResponseFilter;
import static org.glassfish.jersey.examples.exception.Exceptions.MyExceptionMapper;
import static org.glassfish.jersey.examples.exception.Exceptions.MySubExceptionMapper;
import static org.glassfish.jersey.examples.exception.Exceptions.MySubSubException;
import static org.glassfish.jersey.examples.exception.Exceptions.WebApplicationExceptionMapper;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * ExceptionMappingTest class.
 *
 * @author Santiago.PericasGeertsen at oracle.com
 */
public class ExceptionMappingTest extends JerseyTest {

    @Override
    protected ResourceConfig configure() {
        // mvn test -Djersey.test.containerFactory=org.glassfish.jersey.test.inmemory.InMemoryTestContainerFactory
        // mvn test -Djersey.test.containerFactory=org.glassfish.jersey.test.grizzly.GrizzlyTestContainerFactory
        return new ResourceConfig(
                ExceptionResource.class,
                MyResponseFilter.class,
                MyExceptionMapper.class,
                MySubExceptionMapper.class,
                MySubSubException.class,
                WebApplicationExceptionMapper.class);
    }

    /**
     * Ensure we can access resource with response filter installed.
     */
    @Test
    public void testPingAndFilter() {
        WebTarget t = client().target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).build());
        Response r = t.request("text/plain").get();
        assertEquals(200, r.getStatus());
        assertTrue(r.readEntity(String.class).contains(MyResponseFilter.class.getSimpleName()));
    }

    /**
     * No mapper should be used if WebApplicationException already contains a
     * Response with a non-empty entity.
     */
    @Test
    public void testWebApplicationExceptionWithEntity() {
        WebTarget t = client().target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).path("webapplication_entity").build());
        Response r = t.request("text/plain").post(Entity.text("Code:200"));
        assertEquals(200, r.getStatus());
        final String entity = r.readEntity(String.class);
        assertTrue(entity.contains("Code:200"));
        assertTrue(entity.contains(MyResponseFilter.class.getSimpleName()));
    }

    /**
     * No mapper should be used if WebApplicationException already contains a
     * Response with a non-empty entity. Same as last test but using 400 code.
     */
    @Test
    public void testWebApplicationExceptionWithEntity400() {
        WebTarget t = client().target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).path("webapplication_entity").build());
        Response r = t.request("text/plain").post(Entity.text("Code:400"));
        assertEquals(400, r.getStatus());
        final String entity = r.readEntity(String.class);
        assertTrue(entity.contains("Code:400"));
        assertTrue(entity.contains(MyResponseFilter.class.getSimpleName()));
    }

    /**
     * WebApplicationExceptionMapper should be used if WebApplicationException contains
     * empty entity.
     */
    @Test
    public void testWebApplicationExceptionUsingMapper() {
        WebTarget t = client()
                .target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).path("webapplication_noentity").build());
        Response r = t.request("text/plain").post(Entity.text("Code:200"));
        assertEquals(200, r.getStatus());
        String entity = r.readEntity(String.class);
        assertTrue(entity.contains("Code:200"));
        assertTrue(entity.contains(WebApplicationExceptionMapper.class.getSimpleName()));
        assertTrue(entity.contains(MyResponseFilter.class.getSimpleName()));
    }

    /**
     * MyExceptionMapper should be used if MyException is thrown.
     */
    @Test
    public void testMyException() {
        WebTarget t = client().target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).path("my").build());
        Response r = t.request("text/plain").post(Entity.text("Code:200"));
        assertEquals(200, r.getStatus());
        String entity = r.readEntity(String.class);
        assertTrue(entity.contains("Code:200"));
        assertTrue(entity.contains(MyExceptionMapper.class.getSimpleName()));
        assertTrue(entity.contains(MyResponseFilter.class.getSimpleName()));
    }

    /**
     * MySubExceptionMapper should be used if MySubException is thrown.
     */
    @Test
    public void testMySubException() {
        WebTarget t = client().target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).path("mysub").build());
        Response r = t.request("text/plain").post(Entity.text("Code:200"));
        assertEquals(200, r.getStatus());
        String entity = r.readEntity(String.class);
        assertTrue(entity.contains("Code:200"));
        assertTrue(entity.contains(MySubExceptionMapper.class.getSimpleName()));
        assertTrue(entity.contains(MyResponseFilter.class.getSimpleName()));
    }

    /**
     * MySubExceptionMapper should be used if MySubSubException is thrown, given that
     * there is no mapper for MySubSubException and MySubException is the nearest
     * super type.
     */
    @Test
    public void testMySubSubException() {
        WebTarget t = client().target(UriBuilder.fromUri(getBaseUri()).path(App.ROOT_PATH).path("mysub").build());
        Response r = t.request("text/plain").post(Entity.text("Code:200"));
        assertEquals(200, r.getStatus());
        String entity = r.readEntity(String.class);
        assertTrue(entity.contains("Code:200"));
        assertTrue(entity.contains(MySubExceptionMapper.class.getSimpleName()));
        assertTrue(entity.contains(MyResponseFilter.class.getSimpleName()));
    }
}
