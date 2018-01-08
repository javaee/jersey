/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Arrays;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;

import org.glassfish.jersey.tests.e2e.json.JsonTestHelper;

/**
 * @author Jay Feenan (jay.feenan at oracle.com)
 * @author Michal Gajdos
 */
@SuppressWarnings("UnusedDeclaration")
@XmlRootElement(name = "person")
@XmlAccessorType(XmlAccessType.PROPERTY)
@XmlType(propOrder = {"name", "children"})
public class Person {

    public static Object createTestInstance() {
        Person daughter = new Person();
        daughter.setName("Jill Schmo");

        Person son = new Person();
        son.setName("Jack Schmo");

        Person person = new Person();
        person.setName("Joe Schmo");
        person.setChildren(new Person[]{daughter, son});

        return person;
    }

    @SuppressWarnings("SimplifiableIfStatement")
    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final Person other = (Person) obj;
        if ((this.m_name == null) ? (other.m_name != null) : !this.m_name.equals(other.m_name)) {
            return false;
        }
        return JsonTestHelper.areArraysEqual(m_children, other.m_children);
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + (this.m_name != null ? this.m_name.hashCode() : 0);
        hash = 19 * hash + (this.m_children != null ? Arrays.hashCode(this.m_children) : 0);
        return hash;
    }

    @Override
    public String toString() {
        return String.format("{person: %s, %s}", m_name, Arrays.toString(m_children));
    }

    public String getName() {
        return m_name;
    }

    public void setName(String name) {
        m_name = name;
    }

    @XmlElementWrapper(name = "children")
    @XmlElement(name = "child")
    public Person[] getChildren() {
        return m_children;
    }

    public void setChildren(Person[] children) {
        m_children = children;
    }

    private String m_name;
    private Person[] m_children;
}
