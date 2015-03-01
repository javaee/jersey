/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.performance.tools;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashSet;
import java.util.List;

import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlElementWrapper;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Example of a complex testing bean.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@XmlRootElement
public class TestBean {

    /* primitives */
    @GenerateForTest
    public byte bt;
    @GenerateForTest
    public short sh;
    @GenerateForTest
    public int i;
    @GenerateForTest
    public long l;
    @GenerateForTest
    public float f;
    @GenerateForTest
    public double d;
    @GenerateForTest
    public boolean bl;
    @GenerateForTest
    public char c;

    /* primitive wrappers */
    @GenerateForTest
    public Byte wrapBt;
    @GenerateForTest
    public Short wrapSh;
    @GenerateForTest
    public Integer wrapI;
    @GenerateForTest
    public Long wrapL;
    @GenerateForTest
    public Float wrapF;
    @GenerateForTest
    public Double wrapD;
    @GenerateForTest
    public Boolean wrapBl;
    @GenerateForTest
    public Character wrapC;

    /* arrays */
    @GenerateForTest(length = 7)
    public Integer[] array;

    @GenerateForTest
    public Date date;

    /* 1D - collections */
    @XmlElementWrapper(name = "StringElements")
    @XmlElement(name = "StringElement")
    @GenerateForTest(collectionMemberType = String.class, implementingClass = ArrayList.class, length = 10)
    public List<String> stringList;

    @XmlElementWrapper(name = "IntegerElements")
    @XmlElement(name = "IntegerElement")
    @GenerateForTest(collectionMemberType = Integer.class, length = 5)
    public HashSet<Integer> integerSet;

    /* enums */
    @GenerateForTest
    public TestBeanEnum en;

    /* custom types */
    @GenerateForTest
    public TestBeanInfo inner;

    /* recursive */
    @GenerateForTest
    public TestBean nextBean;

    /* and what about those? */
    // CharSequence cs;
    // Object o;
    // Map<String, String> map;

    @Override
    public String toString() {
        return printContent(0);
    }

    public String printContent(int level) {
        String pad = level == 0 ? "" : String.format("%1$" + level + "s", "");
        StringBuffer buf = new StringBuffer();
        buf.append(pad + "# TestBean[level=" + level + "]@" + Integer.toHexString(hashCode())).append("\n");

        buf.append(pad + "# Primitives").append("\n");
        buf.append(pad + "[" + bt + ", " + sh + ", " + i + ", " + l + ", " + f + ", " + d + ", " + bl + ", " + c + "]")
                .append("\n");

        buf.append(pad + "# Primitives wrappers").append("\n");
        buf.append(pad + "[" + wrapBt + ", " + wrapSh + ", " + wrapI + ", " + wrapL + ", " + wrapF + ", " + wrapD + ", "
                + wrapBl + ", " + wrapC + "]").append("\n");

        buf.append(pad + "# Arrays").append("\n");
        if (array != null) {
            buf.append(pad + "array: ");
            for (Integer i : array) {
                buf.append(i + ", ");
            }
            buf.append("\n");
        }

        buf.append(pad + "# Collections").append("\n");
        if (stringList != null) {
            buf.append(pad + "stringList: ");
            for (String s : stringList) {
                buf.append(s + ", ");
            }
            buf.append("\n");
        }
        if (integerSet != null) {
            buf.append(pad + "integerSet: ");
            for (Integer i : integerSet) {
                buf.append(i + ", ");
            }
            buf.append("\n");
        }

        if (date != null) {
            buf.append(pad + "date: " + date).append("\n");
        }

        buf.append(pad + "# Enums").append("\n");
        if (en != null) {
            buf.append(pad + "en=" + en).append("\n");
        }
        buf.append(pad + "# Inner bean").append("\n");
        if (inner != null) {
            buf.append(inner.printContent(level + 1));
        }
        buf.append("\n");
        buf.append(pad + "# Recursive bean").append("\n");
        if (nextBean != null) {
            buf.append(nextBean.printContent(level + 1));
        }
        return buf.toString();
    }

}
