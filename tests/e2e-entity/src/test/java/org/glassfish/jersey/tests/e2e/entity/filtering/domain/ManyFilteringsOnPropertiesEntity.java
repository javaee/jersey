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

package org.glassfish.jersey.tests.e2e.entity.filtering.domain;

import java.util.Collections;
import java.util.List;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;
import javax.xml.bind.annotation.XmlTransient;

import org.glassfish.jersey.tests.e2e.entity.filtering.PrimaryDetailedView;
import org.glassfish.jersey.tests.e2e.entity.filtering.SecondaryDetailedView;

import com.fasterxml.jackson.annotation.JsonAutoDetect;
import com.fasterxml.jackson.annotation.JsonIgnore;

/**
 * @author Michal Gajdos
 */
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.ANY)
@XmlAccessorType(XmlAccessType.PROPERTY)
public class ManyFilteringsOnPropertiesEntity {

    public static final ManyFilteringsOnPropertiesEntity INSTANCE;

    static {
        INSTANCE = new ManyFilteringsOnPropertiesEntity();
        INSTANCE.field = 90;
        INSTANCE.property = "property";
        INSTANCE.defaultEntities = Collections.singletonList(DefaultFilteringSubEntity.INSTANCE);
        INSTANCE.oneEntities = Collections.singletonList(OneFilteringSubEntity.INSTANCE);
        INSTANCE.manyEntities = Collections.singletonList(ManyFilteringsSubEntity.INSTANCE);
        INSTANCE.filtered = FilteredClassEntity.INSTANCE;
    }

    @XmlElement
    public int field;
    private String property;

    @XmlElement
    @PrimaryDetailedView
    public List<DefaultFilteringSubEntity> defaultEntities;

    @XmlElement
    @PrimaryDetailedView
    @SecondaryDetailedView
    public List<OneFilteringSubEntity> oneEntities;

    @XmlElement
    @SecondaryDetailedView
    public List<ManyFilteringsSubEntity> manyEntities;

    @XmlElement
    @PrimaryDetailedView
    @SecondaryDetailedView
    public FilteredClassEntity filtered;

    @XmlTransient
    @JsonIgnore
    public String accessorTransient;

    @PrimaryDetailedView
    @SecondaryDetailedView
    public String getProperty() {
        return property;
    }

    public void setProperty(final String property) {
        this.property = property;
    }

    public String getAccessor() {
        return accessorTransient == null ? property + property : accessorTransient;
    }

    public void setAccessor(final String accessor) {
        accessorTransient = accessor;
    }
}
