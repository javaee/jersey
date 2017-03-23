/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity.filtering;

import java.lang.annotation.Annotation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.ManyFilteringsOnClassEntity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Suite;

/**
 * @author Michal Gajdos
 */
@RunWith(Suite.class)
@Suite.SuiteClasses({
        EntityFilteringServerTest.ConfigurationServerTest.class,
        EntityFilteringServerTest.ConfigurationDefaultViewServerTest.class,
        EntityFilteringServerTest.AnnotationsServerTest.class,
        EntityFilteringServerTest.AnnotationsOverConfigurationServerTest.class
})
public class EntityFilteringServerTest {

    @Path("/")
    @Produces("entity/filtering")
    public static class Resource {

        @GET
        @Path("configuration")
        public ManyFilteringsOnClassEntity getConfiguration() {
            return new ManyFilteringsOnClassEntity();
        }

        @GET
        @Path("configurationOverResource")
        @SecondaryDetailedView
        public ManyFilteringsOnClassEntity getConfigurationOverResource() {
            return new ManyFilteringsOnClassEntity();
        }

        @GET
        @Path("annotations")
        public Response getAnnotations() {
            return Response
                    .ok()
                    .entity(new ManyFilteringsOnClassEntity(), new Annotation[] {PrimaryDetailedView.Factory.get()})
                    .build();
        }

        @GET
        @Path("annotationsOverConfiguration")
        public Response getAnnotationsOverConfiguration() {
            return Response
                    .ok()
                    .entity(new ManyFilteringsOnClassEntity(), new Annotation[] {PrimaryDetailedView.Factory.get()})
                    .build();
        }

        @GET
        @Path("annotationsOverResource")
        @SecondaryDetailedView
        public Response getAnnotationsOverResource() {
            return Response
                    .ok()
                    .entity(new ManyFilteringsOnClassEntity(), new Annotation[] {PrimaryDetailedView.Factory.get()})
                    .build();
        }

        @GET
        @Path("annotationsOverConfigurationOverResource")
        @SecondaryDetailedView
        public Response getAnnotationsOverConfigurationOverResource() {
            return Response
                    .ok()
                    .entity(new ManyFilteringsOnClassEntity(), new Annotation[] {PrimaryDetailedView.Factory.get()})
                    .build();
        }
    }

    private static class FilteringResourceConfig extends ResourceConfig {

        private FilteringResourceConfig() {
            // Resources.
            register(Resource.class);

            // Providers.
            register(EntityFilteringFeature.class);
            register(FilteringMessageBodyProvider.class);
        }
    }

    public static class ConfigurationServerTest extends EntityFilteringTest {

        @Override
        protected Application configure() {
            enable(TestProperties.DUMP_ENTITY);
            enable(TestProperties.LOG_TRAFFIC);

            return new FilteringResourceConfig()
                    // Properties
                    .property(EntityFilteringFeature.ENTITY_FILTERING_SCOPE, PrimaryDetailedView.Factory.get());
        }

        @Test
        public void testConfiguration() throws Exception {
            final String fields = target("configuration").request().get(String.class);

            assertSameFields(fields, "field,accessor,property,manyEntities.property1,manyEntities.field1,oneEntities.field2,"
                    + "oneEntities.property2,oneEntities.property1,oneEntities.field1,defaultEntities.field,defaultEntities"
                    + ".property");
        }

        @Test
        public void testConfigurationOverResource() throws Exception {
            final String fields = target("configurationOverResource").request().get(String.class);

            assertSameFields(fields, "field,accessor,property,manyEntities.property1,manyEntities.field1,oneEntities.field2,"
                    + "oneEntities.property2,oneEntities.property1,oneEntities.field1,defaultEntities.field,defaultEntities"
                    + ".property");
        }
    }

    public static class ConfigurationDefaultViewServerTest extends EntityFilteringTest {

        @Override
        protected Application configure() {
            enable(TestProperties.DUMP_ENTITY);
            enable(TestProperties.LOG_TRAFFIC);

            return new FilteringResourceConfig();
        }

        @Test
        public void testConfiguration() throws Exception {
            final String fields = target("configuration").request().get(String.class);

            assertSameFields(fields, "");
        }
    }

    public static class AnnotationsServerTest extends EntityFilteringTest {

        @Override
        protected Application configure() {
            enable(TestProperties.DUMP_ENTITY);
            enable(TestProperties.LOG_TRAFFIC);

            return new FilteringResourceConfig();
        }

        @Test
        public void testAnnotations() throws Exception {
            final String fields = target("annotations").request().get(String.class);

            assertSameFields(fields, "field,accessor,property,manyEntities.property1,manyEntities.field1,oneEntities.field2,"
                    + "oneEntities.property2,oneEntities.property1,oneEntities.field1,defaultEntities.field,defaultEntities"
                    + ".property");
        }

        @Test
        public void testAnnotationsOverResource() throws Exception {
            final String fields = target("annotationsOverResource").request().get(String.class);

            assertSameFields(fields, "field,accessor,property,manyEntities.property1,manyEntities.field1,oneEntities.field2,"
                    + "oneEntities.property2,oneEntities.property1,oneEntities.field1,defaultEntities.field,defaultEntities"
                    + ".property");
        }
    }

    public static class AnnotationsOverConfigurationServerTest extends EntityFilteringTest {

        @Override
        protected Application configure() {
            enable(TestProperties.DUMP_ENTITY);
            enable(TestProperties.LOG_TRAFFIC);

            return new FilteringResourceConfig()
                    // Properties
                    .property(EntityFilteringFeature.ENTITY_FILTERING_SCOPE, new DefaultFilteringScope());
        }

        @Test
        public void testAnnotationsOverConfiguration() throws Exception {
            final String fields = target("annotationsOverConfiguration").request().get(String.class);

            assertSameFields(fields, "field,accessor,property,manyEntities.property1,manyEntities.field1,oneEntities.field2,"
                    + "oneEntities.property2,oneEntities.property1,oneEntities.field1,defaultEntities.field,defaultEntities"
                    + ".property");
        }

        @Test
        public void testAnnotationsOverConfigurationOverResource() throws Exception {
            final String fields = target("annotationsOverConfigurationOverResource").request().get(String.class);

            assertSameFields(fields, "field,accessor,property,manyEntities.property1,manyEntities.field1,oneEntities.field2,"
                    + "oneEntities.property2,oneEntities.property1,oneEntities.field1,defaultEntities.field,defaultEntities"
                    + ".property");
        }
    }
}
