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

package org.glassfish.jersey.tests.e2e.entity.filtering.domain;

import javax.xml.bind.annotation.XmlAccessType;
import javax.xml.bind.annotation.XmlAccessorType;
import javax.xml.bind.annotation.XmlElement;

import org.glassfish.jersey.tests.e2e.entity.filtering.PrimaryDetailedView;

import com.fasterxml.jackson.annotation.JsonAutoDetect;

/**
* @author Michal Gajdos
*/
@JsonAutoDetect(fieldVisibility = JsonAutoDetect.Visibility.ANY, getterVisibility = JsonAutoDetect.Visibility.ANY)
@XmlAccessorType(XmlAccessType.PROPERTY)
public class OneFilteringSubEntity {

    public static final OneFilteringSubEntity INSTANCE;

    static {
        INSTANCE = new OneFilteringSubEntity();
        INSTANCE.field1 = 20;
        INSTANCE.field2 = 30;
        INSTANCE.property1 = "property1";
        INSTANCE.property2 = "property2";
    }

    @XmlElement
    public int field1;

    @XmlElement
    @PrimaryDetailedView
    public int field2;

    private String property1;
    private String property2;

    @PrimaryDetailedView
    public String getProperty1() {
        return property1;
    }

    public void setProperty1(final String property1) {
        this.property1 = property1;
    }

    public String getProperty2() {
        return property2;
    }

    @PrimaryDetailedView
    public void setProperty2(final String property2) {
        this.property2 = property2;
    }
}
