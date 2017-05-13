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

import java.util.ArrayList;
import java.util.Formatter;
import java.util.List;

import javax.json.bind.annotation.JsonbTransient;
import javax.json.bind.annotation.JsonbVisibility;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlTransient;

import org.glassfish.jersey.tests.e2e.json.JsonTestHelper;

@SuppressWarnings({"UnusedDeclaration", "SimplifiableIfStatement", "StringEquality"})
@XmlRootElement()
@JsonbVisibility(CustomJsonbVisibilityStrategy.class)
public class AnotherArrayTestBean {

    public static Object createTestInstance() {
        AnotherArrayTestBean one = new AnotherArrayTestBean();
        AnotherCat c1 = new AnotherCat("Foo", "Kitty");
        one.addCat(c1);
        AnotherCat c2 = new AnotherCat("Bar", "Puss");
        one.addCat(c2);

        one.setProp("testProp");

        return one;
    }

    @XmlElement(required = true)
    protected List<AnotherCat> cats;
    protected String prop;

    public AnotherArrayTestBean() {
        this.cats = new ArrayList<>();
    }

    public void setCats(List<AnotherCat> cats) {
        this.cats = cats;
    }

    @JsonbTransient
    @XmlTransient
    public List<AnotherCat> getTheCats() {
        return this.cats;
    }

    public void addCat(AnotherCat c) {
        this.cats.add(c);
    }

    public String getProp() {
        return prop;
    }

    public void setProp(String prop) {
        this.prop = prop;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final AnotherArrayTestBean other = (AnotherArrayTestBean) obj;
        if (this.prop != other.prop && (this.prop == null || !this.prop.equals(other.prop))) {
            return false;
        }
        return JsonTestHelper.areCollectionsEqual(cats, other.cats);
    }

    @Override
    public int hashCode() {
        int hash = 5;
        hash = 79 * hash + (this.prop != null ? this.prop.hashCode() : 0);
        hash = 79 * hash + (this.cats != null ? this.cats.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return (new Formatter()).format("AATB(a=%s, cd=%s)", prop, cats).toString();
    }
}
