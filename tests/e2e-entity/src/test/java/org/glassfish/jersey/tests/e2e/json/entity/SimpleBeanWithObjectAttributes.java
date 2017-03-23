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

import java.net.URI;
import java.util.Formatter;

import javax.xml.bind.annotation.XmlAttribute;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * @author Michal Gajdos
 */
@SuppressWarnings({"StringEquality", "RedundantIfStatement", "NumberEquality"})
@XmlRootElement
public class SimpleBeanWithObjectAttributes {

    @XmlAttribute
    public URI uri;
    public String s1;
    @XmlAttribute
    public Integer i;
    @XmlAttribute
    public String j;

    public SimpleBeanWithObjectAttributes() {
    }

    public static Object createTestInstance() {
        return new SimpleBeanWithObjectAttributes();
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final SimpleBeanWithObjectAttributes other = (SimpleBeanWithObjectAttributes) obj;
        if (this.s1 != other.s1 && (this.s1 == null || !this.s1.equals(other.s1))) {
            return false;
        }
        if (this.j != other.j && (this.j == null || !this.j.equals(other.j))) {
            return false;
        }
        if (this.uri != other.uri && (this.uri == null || !this.uri.equals(other.uri))) {
            return false;
        }
        if (this.i != other.i && (this.i == null || !this.i.equals(other.i))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        if (null != s1) {
            hash += 17 * s1.hashCode();
        }
        if (null != j) {
            hash += 17 * j.hashCode();
        }
        if (null != uri) {
            hash += 17 * uri.hashCode();
        }
        hash += 13 * i;
        return hash;
    }

    @Override
    public String toString() {
        return (new Formatter()).format("SBWOA(%s,%d,%s,%s)", s1, i, j, uri).toString();
    }
}
