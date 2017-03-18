/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.NotNull;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.hibernate.validator.constraints.NotBlank;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.is;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * Tests to ensure that validation constraints on a superclass are validated as well.
 *
 * @author Michal Gajdos
 */
public class EntityInheritanceValidationTest extends JerseyTest {

    @Path("/")
    public static class Resource {

        @POST
        @Produces("application/json")
        public Entity post(@Valid final Entity entity) {
            return entity;
        }
    }

    public static class AbstractEntity {

        private String text;

        public AbstractEntity() {
        }

        public AbstractEntity(final String text) {
            this.text = text;
        }

        @NotNull
        @NotBlank
        public String getText() {
            return text;
        }

        public void setText(final String text) {
            this.text = text;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            final AbstractEntity that = (AbstractEntity) o;

            if (!text.equals(that.text)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            return text.hashCode();
        }
    }

    public static class Entity extends AbstractEntity {

        private Integer number;

        public Entity() {
        }

        public Entity(final String text, final Integer number) {
            super(text);
            this.number = number;
        }

        @Min(12)
        @Max(14)
        @NotNull
        public Integer getNumber() {
            return number;
        }

        public void setNumber(final Integer number) {
            this.number = number;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }
            if (!super.equals(o)) {
                return false;
            }

            final Entity entity = (Entity) o;

            if (!number.equals(entity.number)) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = super.hashCode();
            result = 31 * result + number.hashCode();
            return result;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(Resource.class)
                .register(JacksonFeature.class);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        config.register(JacksonFeature.class);
    }

    @Test
    public void testEntityInheritance() throws Exception {
        final Entity entity = new Entity("foo", 13);
        final Response response = target().request().post(javax.ws.rs.client.Entity.json(entity));

        assertThat(response.getStatus(), is(200));
        assertThat(response.readEntity(Entity.class), is(entity));
    }

    @Test
    public void testEntityInheritanceBlankText() throws Exception {
        final Response response = target().request().post(javax.ws.rs.client.Entity.json(new Entity("", 13)));

        assertThat(response.getStatus(), is(400));
    }

    @Test
    public void testEntityInheritanceInvalidNumber() throws Exception {
        final Response response = target().request().post(javax.ws.rs.client.Entity.json(new Entity("foo", 23)));

        assertThat(response.getStatus(), is(400));
    }
}
