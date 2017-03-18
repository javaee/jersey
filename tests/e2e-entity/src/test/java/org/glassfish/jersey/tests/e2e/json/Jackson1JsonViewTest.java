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

package org.glassfish.jersey.tests.e2e.json;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.inject.Singleton;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson1.Jackson1Feature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.codehaus.jackson.map.annotate.JsonView;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Reproducer for JERSEY-1878.
 *
 * @author Michal Gajdos
 */
public class Jackson1JsonViewTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(MyResource.class, Jackson1Feature.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(Jackson1Feature.class);
    }

    public static class SimpleView {}
    public static class DetailedView {}

    public static class TestEntity {

        public static final TestEntity ENTITY = new TestEntity("simple", "detailed");
        public static final TestEntity DETAILED = new TestEntity(null, "detailed");

        private String simple;
        private String detailed;

        public TestEntity() {
        }

        public TestEntity(final String simple, final String detailed) {
            this.simple = simple;
            this.detailed = detailed;
        }

        @JsonView(SimpleView.class)
        public String getSimple() {
            return simple;
        }

        @JsonView(DetailedView.class)
        public String getDetailed() {
            return detailed;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final TestEntity that = (TestEntity) o;

            if (detailed != null ? !detailed.equals(that.detailed) : that.detailed != null) {
                return false;
            }
            if (simple != null ? !simple.equals(that.simple) : that.simple != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = simple != null ? simple.hashCode() : 0;
            result = 31 * result + (detailed != null ? detailed.hashCode() : 0);
            return result;
        }
    }

    @Path("/")
    @Singleton
    public static class MyResource {

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("async")
        @JsonView(DetailedView.class)
        public void getAsync(@Suspended final AsyncResponse response) {
            response.resume(TestEntity.ENTITY);
        }

        @GET
        @Produces(MediaType.APPLICATION_JSON)
        @Path("sync")
        @JsonView(DetailedView.class)
        public TestEntity getSync() {
            return TestEntity.ENTITY;
        }
    }

    @Test
    public void testSync() throws Exception {
        final Response response = target().path("sync").request().get();

        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(TestEntity.class), is(TestEntity.DETAILED));
    }

    @Test
    public void testAsync() throws Exception {
        final Response response = target().path("async").request().async().get().get();

        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(TestEntity.class), is(TestEntity.DETAILED));

        response.close();
    }
}
