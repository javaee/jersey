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

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.ManyFilteringsOnClassEntity;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.OneFilteringOnClassEntity;

import org.junit.Test;

/**
 * Use-cases with entity-filtering annotations on class.
 *
 * @author Michal Gajdos
 */
public class EntityFilteringOnClassTest extends EntityFilteringTest {

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig()
                // Resources.
                .register(Resource.class)
                // Providers.
                .register(EntityFilteringFeature.class)
                .register(FilteringMessageBodyProvider.class);
    }

    @Path("/")
    @Consumes("entity/filtering")
    @Produces("entity/filtering")
    public static class Resource {

        @GET
        @Path("OneFilteringEntity")
        @PrimaryDetailedView
        public OneFilteringOnClassEntity getOneFilteringEntity() {
            return new OneFilteringOnClassEntity();
        }

        @GET
        @Path("OneFilteringEntityDefaultView")
        public OneFilteringOnClassEntity getOneFilteringEntityDefaultView() {
            return new OneFilteringOnClassEntity();
        }

        @POST
        @Path("OneFilteringEntity")
        public String postOneFilteringEntity(final String value) {
            return value;
        }

        @GET
        @Path("OneFilteringEntityDefaultViewResponse")
        public Response getOneFilteringEntityDefaultViewResponse() {
            return Response.ok().entity(new OneFilteringOnClassEntity(), new Annotation[] {new DefaultFilteringScope()}).build();
        }

        @GET
        @Path("ManyFilteringsEntityPrimaryView")
        @PrimaryDetailedView
        public ManyFilteringsOnClassEntity getManyFilteringsEntityPrimaryView() {
            return new ManyFilteringsOnClassEntity();
        }

        @GET
        @Path("ManyFilteringsEntitySecondaryView")
        @SecondaryDetailedView
        public ManyFilteringsOnClassEntity getManyFilteringsEntitySecondaryView() {
            return new ManyFilteringsOnClassEntity();
        }

        @GET
        @Path("ManyFilteringsEntityDefaultView")
        public ManyFilteringsOnClassEntity getManyFilteringsEntityDefaultView() {
            return new ManyFilteringsOnClassEntity();
        }

        @GET
        @Path("ManyFilteringsEntityManyViews")
        @PrimaryDetailedView
        @SecondaryDetailedView
        public ManyFilteringsOnClassEntity getManyFilteringsEntityManyViews() {
            return new ManyFilteringsOnClassEntity();
        }
    }

    @Test
    public void testOneEntityFilteringOnClass() throws Exception {
        final String fields = target("OneFilteringEntity").request().get(String.class);

        assertSameFields(fields, "field,accessor,property,subEntities.field2,subEntities.property2,subEntities.property1,"
                + "subEntities.field1,defaultEntities.field,defaultEntities.property");
    }

    @Test
    public void testOneEntityFilteringOnClassDefaultViewResponse() throws Exception {
        final String fields = target("OneFilteringEntityDefaultViewResponse").request().get(String.class);

        assertSameFields(fields, "");
    }

    @Test
    public void testOneEntityFilteringOnClassDefaultView() throws Exception {
        final String fields = target("OneFilteringEntityDefaultView").request().get(String.class);

        assertSameFields(fields, "");
    }

    @Test
    public void testMultipleViewsOnClass() throws Exception {
        testOneEntityFilteringOnClass();
        testOneEntityFilteringOnClassDefaultView();
    }

    @Test
    public void testManyFilteringsEntityPrimaryView() throws Exception {
        final String fields = target("ManyFilteringsEntityPrimaryView").request().get(String.class);

        assertSameFields(fields, "field,accessor,property,manyEntities.property1,manyEntities.field1,oneEntities.field2,"
                + "oneEntities.property2,oneEntities.property1,oneEntities.field1,defaultEntities.field,defaultEntities"
                + ".property");
    }

    @Test
    public void testManyFilteringsEntitySecondaryView() throws Exception {
        final String fields = target("ManyFilteringsEntitySecondaryView").request().get(String.class);

        assertSameFields(fields, "field,accessor,property,manyEntities.field2,manyEntities.property2,manyEntities.field1,"
                + "oneEntities.property2,oneEntities.field1,defaultEntities.field,defaultEntities.property");
    }

    @Test
    public void testManyFilteringsEntityDefaultView() throws Exception {
        final String fields = target("ManyFilteringsEntityDefaultView").request().get(String.class);

        assertSameFields(fields, "");
    }

    @Test
    public void testManyFilteringsEntityManyViews() throws Exception {
        final String fields = target("ManyFilteringsEntityManyViews").request().get(String.class);

        assertSameFields(fields, "field,accessor,property,manyEntities.field2,manyEntities.property2,manyEntities.property1,"
                + "manyEntities.field1,oneEntities.field2,oneEntities.property2,oneEntities.property1,oneEntities.field1,"
                + "defaultEntities.field,defaultEntities.property");
    }
}
