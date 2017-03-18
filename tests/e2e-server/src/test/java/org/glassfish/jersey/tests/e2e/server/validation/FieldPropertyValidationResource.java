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

package org.glassfish.jersey.tests.e2e.server.validation;

import javax.ws.rs.GET;
import javax.ws.rs.Path;

import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

/**
 * @author Michal Gajdos
 */
@Path("fieldPropertyValidationResource")
public class FieldPropertyValidationResource {

    @Path("valid")
    public SubResource valid() {
        return new SubResource("valid", "valid", "valid", "valid", "valid", "valid");
    }

    @Path("invalidPropertyGetterAndClassNull")
    public SubResource invalidPropertyGetterAndClassNull() {
        return new SubResource("valid", "valid", "valid", "valid", "valid", null);
    }

    @Path("invalidPropertyGetterAndClassLong")
    public SubResource invalidPropertyGetterAndClassLong() {
        return new SubResource("valid", "valid", "valid", "valid", "valid", "valid-valid");
    }

    @Path("invalidPropertyAndClassNull")
    public SubResource invalidPropertyAndClassNull() {
        return new SubResource("valid", "valid", "valid", "valid", null, "valid");
    }

    @Path("invalidFieldAndClassNull")
    public SubResource invalidFieldAndClassNull() {
        return new SubResource("valid", "valid", "valid", null, "valid", "valid");
    }

    @Path("invalidPropertyGetterNull")
    public SubResource invalidPropertyGetterNull() {
        return new SubResource("valid", "valid", null, "valid", "valid", "valid");
    }

    @Path("invalidPropertyGetterLong")
    public SubResource invalidPropertyGetterLong() {
        return new SubResource("valid", "valid", "valid-valid", "valid", "valid", "valid");
    }

    @Path("invalidPropertyNull")
    public SubResource invalidPropertyNull() {
        return new SubResource("valid", null, "valid", "valid", "valid", "valid");
    }

    @Path("invalidFieldNull")
    public SubResource invalidFieldNull() {
        return new SubResource(null, "valid", "valid", null, "valid", "valid");
    }

    @FieldPropertyValidation(elements = {"fieldAndClass", "propertyAndClass", "propertyGetterAndClass"})
    public static class SubResource {

        @NotNull
        @Size(min = 5)
        final String field;

        @NotNull
        @Size(min = 5)
        final String property;

        @NotNull
        @Size(min = 5)
        final String propertyGetter;

        final String fieldAndClass;

        final String propertyAndClass;

        final String propertyGetterAndClass;

        public SubResource(final String field, final String property, final String propertyAndGetter,
                           final String fieldAndClass, final String propertyAndClass, final String propertyGetterAndClass) {
            this.field = field;
            this.property = property;
            this.propertyGetter = propertyAndGetter;
            this.fieldAndClass = fieldAndClass;
            this.propertyAndClass = propertyAndClass;
            this.propertyGetterAndClass = propertyGetterAndClass;
        }

        public String getProperty() {
            return property;
        }

        @Size(max = 5)
        public String getPropertyGetter() {
            return propertyGetter;
        }

        public String getPropertyAndClass() {
            return propertyAndClass;
        }

        @Size(max = 5)
        public String getPropertyGetterAndClass() {
            return propertyGetterAndClass;
        }

        @GET
        public String method() {
            return "ok";
        }
    }
}
