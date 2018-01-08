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

package org.glassfish.jersey.tests.performance.benchmark.entity.json;

import java.util.Arrays;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

/**
 * Resource class for {@link Project projects}. Provides methods to retrieve projects in "default" view ({@link #getProjects()}
 * and in "detailed" view ({@link #getDetailedProjects()}.
 *
 * @author Michal Gajdos
 */
@Path("projects")
@Produces("application/json")
public class ProjectsResource {

    private static final List<Project> projects;

    static {
        final Project project = new Project(1L, "foo", "bar");
        final User user = new User(1L, "foo", "foo@bar.baz");
        final Task task = new Task(1L, "foo", "bar");

        project.setUsers(Arrays.asList(user));
        project.setTasks(Arrays.asList(task, task));

        projects = Arrays.asList(project, project);
    }

    @GET
    @Path("basic")
    public List<Project> getProjects() {
        return getDetailedProjects();
    }

    @GET
    @Path("detailed")
    @ProjectDetailedView
    public List<Project> getDetailedProjects() {
        return projects;
    }
}
