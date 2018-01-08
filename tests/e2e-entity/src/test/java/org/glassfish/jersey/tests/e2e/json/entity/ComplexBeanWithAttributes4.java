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

package org.glassfish.jersey.tests.e2e.json.entity;

import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;

import javax.json.bind.annotation.JsonbVisibility;
import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.tests.e2e.json.JsonTestHelper;

/**
 * @author Michal Gajdos
 */
@SuppressWarnings({"StringEquality", "RedundantIfStatement", "NumberEquality"})
@XmlRootElement
@JsonbVisibility(CustomJsonbVisibilityStrategy.class)
public class ComplexBeanWithAttributes4 {

    @XmlAttribute
    public String a1;
    @XmlAttribute
    public Integer a2;
    @XmlElement
    public String filler1;
    @XmlElement
    public List<SimpleBeanWithObjectAttributes> list;
    @XmlElement
    public String filler2;
    @XmlElement
    SimpleBeanWithObjectAttributes b;

    public static Object createTestInstance() {
        ComplexBeanWithAttributes4 instance = new ComplexBeanWithAttributes4();
        instance.b = new SimpleBeanWithObjectAttributes();
        instance.list = new LinkedList<>();
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComplexBeanWithAttributes4)) {
            return false;
        }
        final ComplexBeanWithAttributes4 other = (ComplexBeanWithAttributes4) obj;
        if (this.a1 != other.a1 && (this.a1 == null || !this.a1.equals(other.a1))) {
            return false;
        }
        if (this.a2 != other.a2 && (this.a2 == null || !this.a2.equals(other.a2))) {
            return false;
        }
        if (this.b != other.b && (this.b == null || !this.b.equals(other.b))) {
            return false;
        }
        if (this.filler1 != other.filler1 && (this.filler1 == null || !this.filler1.equals(other.filler1))) {
            return false;
        }
        if (this.filler2 != other.filler2 && (this.filler2 == null || !this.filler2.equals(other.filler2))) {
            return false;
        }
        if (!JsonTestHelper.areCollectionsEqual(this.list, other.list)) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + (this.a1 != null ? this.a1.hashCode() : 0);
        hash = 19 * hash + this.a2;
        hash = 19 * hash + (this.b != null ? this.b.hashCode() : 0);
        hash = 19 * hash + (this.filler1 != null ? this.filler1.hashCode() : 0);
        hash = 19 * hash + (this.filler2 != null ? this.filler2.hashCode() : 0);
        hash = 19 * hash + (this.list != null ? this.list.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return (new Formatter()).format("CBWA4(%s, %d, %s)", a1, a2, b).toString();
    }
}
