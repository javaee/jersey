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

package org.glassfish.jersey.examples.entityfiltering;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.examples.entityfiltering.domain.Project;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link org.glassfish.jersey.examples.entityfiltering.resource.UsersResource} unit tests.
 *
 * @author Michal Gajdos
 */
@RunWith(Parameterized.class)
public class ProjectsResourceTest extends JerseyTest {

    @Parameterized.Parameters(name = "Provider: {0}")
    public static Iterable<Class[]> providers() {
        return Arrays.asList(new Class[][]{{MoxyJsonFeature.class}, {JacksonFeature.class}});
    }

    public ProjectsResourceTest(final Class<Feature> filteringProvider) {
        super(new ResourceConfig(EntityFilteringFeature.class)
                .packages("org.glassfish.jersey.examples.entityfiltering.resource")
                .register(filteringProvider));

        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);
    }

    @Test
    public void testProjects() throws Exception {
        for (final Project project : target("projects").request().get(new GenericType<List<Project>>() {})) {
            testProject(project, false);
        }
    }

    @Test
    public void testProject() throws Exception {
        testProject(target("projects").path("1").request().get(Project.class), false);
    }

    @Test
    public void testDetailedProjects() throws Exception {
        for (final Project project : target("projects/detailed").request().get(new GenericType<List<Project>>() {})) {
            testProject(project, true);
        }
    }

    @Test
    public void testDetailedProject() throws Exception {
        testProject(target("projects/detailed").path("1").request().get(Project.class), true);
    }

    private void testProject(final Project project, final boolean isDetailed) {
        // Following properties should be in every returned project.
        assertThat(project.getId(), notNullValue());
        assertThat(project.getName(), notNullValue());
        assertThat(project.getDescription(), notNullValue());

        // Tasks and users should be only in "detailed" view.
        if (!isDetailed) {
            assertThat("Users present in non-detailed project view", project.getUsers(), nullValue());
            assertThat("Tasks present in non-detailed project view", project.getTasks(), nullValue());
        } else {
            assertThat("Users not present in detailed project view", project.getUsers(), notNullValue());
            assertThat("Tasks not present in detailed project view", project.getTasks(), notNullValue());
        }
    }
}
