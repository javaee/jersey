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

import java.util.Formatter;
import java.util.LinkedList;
import java.util.List;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.tests.e2e.json.JsonTestHelper;

/**
 * @author mchenryc
 * @author Michal Gajdos
 */
@SuppressWarnings("UnusedDeclaration")
@XmlRootElement(name = "listEmptyBean")
public class ListEmptyBean {

    private List<String> empty;

    public static Object createTestInstance() {
        ListEmptyBean instance = new ListEmptyBean();
        instance.empty = new LinkedList<>();
        return instance;
    }

    public List<String> getEmpty() {
        return empty;
    }

    public void setEmpty(List<String> empty) {
        this.empty = empty;
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ListEmptyBean other = (ListEmptyBean) obj;
        return this.empty == other.empty
                || (JsonTestHelper.isCollectionEmpty(this.empty) && JsonTestHelper.isCollectionEmpty(other.empty))
                || (this.empty != null && this.empty.equals(other.empty));
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (this.empty != null ? this.empty.hashCode() : 0);
        return hash;
    }

    @Override
    public String toString() {
        return (new Formatter()).format("LwNB(n=%d,isNull:%s)", (empty != null) ? empty.size() : 0, (empty == null)).toString();
    }
}
