/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.entityfiltering;

import java.util.List;

import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;

import org.glassfish.jersey.examples.entityfiltering.domain.Task;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.CoreMatchers.nullValue;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link org.glassfish.jersey.examples.entityfiltering.resource.TasksResource} unit tests.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class TaskResourceTest extends JerseyTest {

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return new EntityFilteringApplication();
    }

    @Test
    public void testTasks() throws Exception {
        for (final Task task : target("tasks").request().get(new GenericType<List<Task>>() {})) {
            testTask(task, false);
        }
    }

    @Test
    public void testTask() throws Exception {
        testTask(target("tasks").path("1").request().get(Task.class), false);
    }

    @Test
    public void testDetailedTasks() throws Exception {
        for (final Task task : target("tasks").path("detailed").request().get(new GenericType<List<Task>>() {})) {
            testTask(task, true);
        }
    }

    @Test
    public void testDetailedTask() throws Exception {
        testTask(target("tasks").path("1").queryParam("detailed", true).request().get(Task.class), true);
    }

    private void testTask(final Task task, final boolean isDetailed) {
        // Following properties should be in every returned task.
        assertThat(task.getId(), notNullValue());
        assertThat(task.getName(), notNullValue());
        assertThat(task.getDescription(), notNullValue());

        // Tasks and tasks should be in "detailed" view.
        if (!isDetailed) {
            assertThat(task.getProject(), nullValue());
            assertThat(task.getUser(), nullValue());
        } else {
            assertThat(task.getProject(), notNullValue());
            assertThat(task.getUser(), notNullValue());
        }
    }
}
