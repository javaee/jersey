/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity.filtering.json;

import java.util.Arrays;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Feature;
import javax.ws.rs.ext.ContextResolver;

import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.EmptyEntity;
import org.glassfish.jersey.tests.e2e.entity.filtering.domain.NonEmptyEntity;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;

import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.junit.Assert.assertThat;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * Use-cases to check whether empty class causes problems (JERSEY-2824 reproducer).
 *
 * @author Michal Gajdos
 */
@RunWith(Parameterized.class)
public class JsonEmptyEntityTest extends JerseyTest {

    @Parameterized.Parameters(name = "Provider: {0}")
    public static Iterable<Class[]> providers() {
        return Arrays.asList(new Class[][] {{MoxyJsonFeature.class}, {JacksonFeature.class}});
    }

    @Path("/")
    @Consumes("application/json")
    @Produces("application/json")
    public static class Resource {

        @Path("nonEmptyEntity")
        @GET
        public NonEmptyEntity nonEmptyEntity() {
            return NonEmptyEntity.INSTANCE;
        }

        @Path("emptyEntity")
        @GET
        public EmptyEntity emptyEntity() {
            return new EmptyEntity();
        }
    }

    public JsonEmptyEntityTest(final Class<Feature> filteringProvider) {
        super(new ResourceConfig(Resource.class, EntityFilteringFeature.class)
                .register(filteringProvider)
                .register(new ContextResolver<ObjectMapper>() {
                    @Override
                    public ObjectMapper getContext(final Class<?> type) {
                        return new ObjectMapper()
                                .configure(SerializationFeature.FAIL_ON_EMPTY_BEANS, false)
                                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
                    }
                }));

        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);
    }

    @Test
    public void testNonEmptyEntity() throws Exception {
        final NonEmptyEntity entity = target("nonEmptyEntity").request().get(NonEmptyEntity.class);

        assertThat(entity.getValue(), is("foo"));
        assertThat(entity.getEmptyEntity(), nullValue());
    }

    @Test
    public void testEmptyEntity() throws Exception {
        assertThat(target("emptyEntity").request().get(String.class), is("{}"));
    }
}
