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

import javax.json.bind.annotation.JsonbVisibility;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.tests.e2e.json.JsonTestHelper;

/**
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@XmlRootElement
@JsonbVisibility(CustomJsonbVisibilityStrategy.class)
public class ComplexBeanWithAttributes3 {

    @XmlElement
    SimpleBeanWithJustOneAttribute b, c;

    public static Object createTestInstance() {
        ComplexBeanWithAttributes3 instance = new ComplexBeanWithAttributes3();
        instance.b = JsonTestHelper.createTestInstance(SimpleBeanWithJustOneAttribute.class);
        instance.c = JsonTestHelper.createTestInstance(SimpleBeanWithJustOneAttribute.class);
        return instance;
    }

    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof ComplexBeanWithAttributes3)) {
            return false;
        }
        final ComplexBeanWithAttributes3 other = (ComplexBeanWithAttributes3) obj;
        if (this.b != other.b && (this.b == null || !this.b.equals(other.b))) {
            System.out.println("b differs");
            return false;
        }
        if (this.c != other.c && (this.c == null || !this.c.equals(other.c))) {
            System.out.println("c differs");
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 19 * hash + (this.b != null ? this.b.hashCode() : 0);
        hash = 19 * hash + (this.c != null ? this.c.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return String.format("CBWA2(%s,%s)", b, c);
    }
}
