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
package org.glassfish.jersey.tests.e2e.json.entity;

import java.util.Collections;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.tests.e2e.json.JsonTestHelper;

/**
 * Note: With MOXy we need to ensure that collections (a list in this case) with predefined values (assigned to the list during
 * object initialization) are either uninitialized or empty during the object creation, otherwise there is a possibility that
 * these default values are doubled in the list (list is filled with default values when a new instance is created and after
 * unmarshalling XML/JSON stream additional elements are added to this list - MOXy doesn't override the existing list with a
 * new one created during unmarshalling).
 * <p/>
 * Workaround: Set {@link javax.xml.bind.annotation.XmlAccessorType} to {@link javax.xml.bind.annotation.XmlAccessType#FIELD},
 * do not initialize the list in the default constructor
 * (field initializer) and assign the value to the list that should contain predefined values manually (in this case the value
 * object is represented by {@code #DEFAULT_HEADERS}).
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Michal Gajdos
 */
@SuppressWarnings("UnusedDeclaration")
@XmlRootElement
@XmlAccessorType(XmlAccessType.FIELD)
public class UserTable {

    @SuppressWarnings("RedundantIfStatement")
    public static class JMakiTableHeader {

        public String id;
        public String label;

        public JMakiTableHeader() {
        }

        public JMakiTableHeader(String id, String label) {
            this.id = id;
            this.label = label;
        }

        @Override
        public int hashCode() {
            int hash = 13;
            hash = id != null ? 29 * id.hashCode() : hash;
            hash = label != null ? 29 * label.hashCode() : hash;
            return hash;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof JMakiTableHeader)) {
                return false;
            }
            JMakiTableHeader that = (JMakiTableHeader) obj;

            if ((id != null && !id.equals(that.id)) && that.id != null) {
                return false;
            }
            if ((label != null && !label.equals(that.label)) && that.label != null) {
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            return "JMakiTableHeader(id = " + id + ", label = " + label + ")";
        }
    }

    public static Object createTestInstance() {
        UserTable instance = new UserTable();
        instance.rows = new LinkedList<User>();
        instance.rows.add(JsonTestHelper.createTestInstance(User.class));
        instance.columns = DEFAULT_HEADERS;
        return instance;
    }

    public static Object createTestInstance2() {
        UserTable instance = new UserTable();
        instance.rows = new LinkedList<User>();
        instance.rows.add(JsonTestHelper.createTestInstance(User.class));
        instance.addColumn(new JMakiTableHeader("password", "Password"));
        return instance;
    }

    static List<JMakiTableHeader> initHeaders() {
        List<JMakiTableHeader> headers = new LinkedList<JMakiTableHeader>();
        headers.add(new JMakiTableHeader("userid", "UserID"));
        headers.add(new JMakiTableHeader("name", "User Name"));
        return Collections.unmodifiableList(headers);
    }

    public static final List<JMakiTableHeader> DEFAULT_HEADERS = initHeaders();

    private List<JMakiTableHeader> columns;
    private List<User> rows;

    public UserTable() {
    }

    public UserTable(List<User> users) {
        this.rows = new LinkedList<User>();
        this.rows.addAll(users);
        this.columns = DEFAULT_HEADERS;
    }

    public void addColumn(final JMakiTableHeader column) {
        getColumns().add(column);
    }

    public List<JMakiTableHeader> getColumns() {
        if (columns == null) {
            columns = new LinkedList<JMakiTableHeader>(DEFAULT_HEADERS);
        }
        return columns;
    }

    public void setColumns(final List<JMakiTableHeader> columns) {
        this.columns = columns;
    }

    public List<User> getRows() {
        return rows;
    }

    public void setRows(final List<User> rows) {
        this.rows = rows;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof UserTable)) {
            return false;
        }
        final UserTable other = (UserTable) obj;

        return JsonTestHelper.areCollectionsEqual(this.rows, other.rows)
                && JsonTestHelper.areCollectionsEqual(this.columns, other.columns);
    }

    @Override
    public int hashCode() {
        int hash = 16;
        if (null != rows) {
            for (User u : rows) {
                hash = 17 * hash + u.hashCode();
            }
        }
        if (null != columns) {
            for (JMakiTableHeader u : columns) {
                hash = 17 * hash + u.hashCode();
            }
        }
        return hash;
    }

    @Override
    public String toString() {
        return String.format("UserTable(%s,%s)", rows, columns);
    }
}
