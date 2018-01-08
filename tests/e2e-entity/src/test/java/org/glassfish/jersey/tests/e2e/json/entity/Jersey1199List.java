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
import java.util.EnumSet;
import java.util.Formatter;

import javax.json.bind.annotation.JsonbTypeAdapter;
import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.tests.e2e.json.Jersey1199Test;

/**
 * @author Michal Gajdos
 */
@SuppressWarnings({"UnusedDeclaration", "NumberEquality", "SimplifiableIfStatement"})
@XmlRootElement
public class Jersey1199List {

    public static Object createTestInstance() {
        final ColorHolder obj1 = new ColorHolder(EnumSet.of(Color.RED, Color.BLUE));
        final ColorHolder obj2 = new ColorHolder(EnumSet.of(Color.GREEN));

        return new Jersey1199List(new Object[]{obj1, obj2});
    }

    private Object[] objects;
    private Integer offset;
    private Integer total;

    public Jersey1199List() {
    }

    public Jersey1199List(final Object[] objects) {
        this.objects = objects;
        this.offset = 0;
        this.total = objects.length;
    }

    // Jackson 1
    @org.codehaus.jackson.annotate.JsonTypeInfo(
            use = org.codehaus.jackson.annotate.JsonTypeInfo.Id.NAME,
            include = org.codehaus.jackson.annotate.JsonTypeInfo.As.PROPERTY)
    @org.codehaus.jackson.annotate.JsonSubTypes({
            @org.codehaus.jackson.annotate.JsonSubTypes.Type(value = ColorHolder.class)
    })
    // Jackson 2
    @com.fasterxml.jackson.annotation.JsonTypeInfo(
            use = com.fasterxml.jackson.annotation.JsonTypeInfo.Id.NAME,
            include = com.fasterxml.jackson.annotation.JsonTypeInfo.As.PROPERTY)
    @com.fasterxml.jackson.annotation.JsonSubTypes({
            @com.fasterxml.jackson.annotation.JsonSubTypes.Type(value = ColorHolder.class)
    })
    // JSON-B
    @JsonbTypeAdapter(Jersey1199Test.JsonbObjectToColorHolderAdapter.class)
    public Object[] getObjects() {
        return objects;
    }

    public void setObjects(final Object[] objects) {
        this.objects = objects;
    }

    public Integer getOffset() {
        return offset;
    }

    public void setOffset(final Integer offset) {
        this.offset = offset;
    }

    public Integer getTotal() {
        return total;
    }

    public void setTotal(final Integer total) {
        this.total = total;
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (!(obj instanceof Jersey1199List)) {
            return false;
        }

        final Jersey1199List other = (Jersey1199List) obj;

        if (this.total != other.total && (this.total == null || !this.total.equals(other.total))) {
            return false;
        }
        if (this.offset != other.offset && (this.offset == null || !this.offset.equals(other.offset))) {
            return false;
        }

        return Arrays.equals(this.objects, other.objects);
    }

    @Override
    public String toString() {
        return new Formatter().format("Jersey1199List(%s, %d, %d)", Arrays.toString(objects), offset, total).toString();
    }

    @Override
    public int hashCode() {
        int hash = 43;
        hash += (offset != null ? 17 * offset : 0);
        hash += (total != null ? 17 * total : 0);
        hash += Arrays.hashCode(objects);
        return hash;
    }
}
