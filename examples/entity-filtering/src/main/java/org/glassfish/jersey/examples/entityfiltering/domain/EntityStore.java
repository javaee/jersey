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

package org.glassfish.jersey.examples.entityfiltering.domain;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Entity-store utility class. Class creates a sample instance of each entity.
 *
 * @author Michal Gajdos
 */
@SuppressWarnings({"JavaDoc", "UnusedDeclaration"})
public final class EntityStore {

    private static final Map<Long, Project> PROJECTS = new LinkedHashMap<>();
    private static final Map<Long, User> USERS = new LinkedHashMap<>();
    private static final Map<Long, Task> TASKS = new LinkedHashMap<>();

    static {
        // Projects.
        final Project project = createProject("Jersey", "Jersey is the open source (under dual CDDL+GPL license) JAX-RS 2.0 "
                + "(JSR 339) production quality Reference Implementation for building RESTful Web services.");

        // Users.
        final User robot = createUser("Jersey Robot", "very@secret.com");

        // Tasks.
        final Task filtering = createTask("ENT_FLT", "Entity Data Filtering");
        final Task oauth = createTask("OAUTH", "OAuth 1 + 2");

        // Project -> Users, Tasks.
        add(project, robot);
        filtering.setProject(project);
        oauth.setProject(project);

        // Users -> Projects, Tasks.
        add(robot, project);
        filtering.setUser(robot);
        oauth.setUser(robot);

        // Tasks -> Projects, Users.
        add(filtering, project);
        add(oauth, project);
        add(filtering, robot);
        add(oauth, robot);
    }

    public static void add(final Project project, final User user) {
        user.getProjects().add(project);
    }

    public static void add(final User user, final Project project) {
        project.getUsers().add(user);
    }

    public static void add(final Task task, final User user) {
        user.getTasks().add(task);
    }

    public static void add(final Task task, final Project project) {
        project.getTasks().add(task);
    }

    public static Project createProject(final String name, final String description) {
        return createProject(name, description, null, null);
    }

    public static Project createProject(final String name, final String description, final List<User> users,
                                        final List<Task> tasks) {
        final Project project = new Project(PROJECTS.size() + 1L, name, description);

        project.setTasks(tasks == null ? new ArrayList<Task>() : tasks);
        project.setUsers(users == null ? new ArrayList<User>() : users);
        PROJECTS.put(project.getId(), project);

        return project;
    }

    public static User createUser(final String name, final String email) {
        return createUser(name, email, null, null);
    }

    public static User createUser(final String name, final String email, final List<Project> projects, final List<Task> tasks) {
        final User user = new User(USERS.size() + 1L, name, email);

        user.setProjects(projects == null ? new ArrayList<Project>() : projects);
        user.setTasks(tasks == null ? new ArrayList<Task>() : tasks);
        USERS.put(user.getId(), user);

        return user;
    }

    public static Task createTask(final String name, final String description) {
        return createTask(name, description, null, null);
    }

    public static Task createTask(final String name, final String description, final Project project, final User user) {
        final Task task = new Task(TASKS.size() + 1L, name, description);

        task.setProject(project);
        task.setUser(user);
        TASKS.put(task.getId(), task);

        return task;
    }

    public static Project getProject(final Long id) {
        return PROJECTS.get(id);
    }

    public static User getUser(final Long id) {
        return USERS.get(id);
    }

    public static Task getTask(final Long id) {
        return TASKS.get(id);
    }

    public static List<Project> getProjects() {
        return new ArrayList<>(PROJECTS.values());
    }

    public static List<User> getUsers() {
        return new ArrayList<>(USERS.values());
    }

    public static List<Task> getTasks() {
        return new ArrayList<>(TASKS.values());
    }

    /**
     * Prevent instantiation.
     */
    private EntityStore() {
    }
}
