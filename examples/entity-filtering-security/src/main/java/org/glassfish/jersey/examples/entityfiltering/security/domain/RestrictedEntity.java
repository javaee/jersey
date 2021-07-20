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

package org.glassfish.jersey.examples.entityfiltering.security.domain;

import javax.annotation.security.DenyAll;
import javax.annotation.security.PermitAll;
import javax.annotation.security.RolesAllowed;
import javax.xml.bind.annotation.XmlRootElement;

/**
 * Restricted entity to demonstrate various security annotations.
 *
 * @author Michal Gajdos
 */
@XmlRootElement
public class RestrictedEntity {

    private String simpleField;

    private String denyAll;

    private String permitAll;

    private RestrictedSubEntity mixedField;

    public String getSimpleField() {
        return simpleField;
    }

    @DenyAll
    public String getDenyAll() {
        return denyAll;
    }

    @PermitAll
    public String getPermitAll() {
        return permitAll;
    }

    @RolesAllowed({"manager", "user"})
    public RestrictedSubEntity getMixedField() {
        return mixedField;
    }

    public void setSimpleField(final String simpleField) {
        this.simpleField = simpleField;
    }

    public void setDenyAll(final String denyAll) {
        this.denyAll = denyAll;
    }

    public void setPermitAll(final String permitAll) {
        this.permitAll = permitAll;
    }

    public void setMixedField(final RestrictedSubEntity mixedField) {
        this.mixedField = mixedField;
    }

    /**
     * Get an instance of RestrictedEntity. This method creates always a new instance of RestrictedEntity.
     *
     * @return an instance of RestrictedEntity.
     */
    public static RestrictedEntity instance() {
        final RestrictedEntity entity = new RestrictedEntity();

        entity.setSimpleField("Simple Field.");
        entity.setDenyAll("Deny All.");
        entity.setPermitAll("Permit All.");

        final RestrictedSubEntity mixedField = new RestrictedSubEntity();
        mixedField.setManagerField("Manager's Field.");
        mixedField.setUserField("User's Field.");

        entity.setMixedField(mixedField);

        return entity;
    }
}
