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
package org.glassfish.jersey.tests.e2e.server;

import java.util.Formatter;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.jettison.JettisonFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * End to end test class for testing {@code ResourceConfig} features.
 *
 * @author Michal Gajdos
 */
public class ResourceConfigTest extends JerseyTest {

    @SuppressWarnings({"UnusedDeclaration", "StringEquality", "RedundantIfStatement"})
    @XmlRootElement
    public static class Property {

        private String name;
        private String value;

        public Property() {
        }

        public Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Property)) {
                return false;
            }
            final Property other = (Property) obj;
            if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
                return false;
            }
            if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 47 + (name == null ? 0 : name.hashCode());
            hash = hash * 47 + (value == null ? 0 : value.hashCode());
            return hash;
        }

        @Override
        public String toString() {
            return new Formatter().format("Property(name=%s,value=%s)", name, value).toString();
        }

    }

    @Path("/")
    @Produces("application/json")
    @Consumes("application/json")
    public static class Resource {

        @POST
        public Property post(final Property property) {
            return property;
        }

    }

    /**
     * Application similar to the one that needs to register a custom {@code Feature} to run properly
     * and is supposed to be present in WAR files.
     */
    public static class Jersey1094 extends ResourceConfig {

        public Jersey1094() {
            registerClasses(Resource.class, JettisonFeature.class);
        }

    }

    @Override
    protected ResourceConfig configure() {
        // Simulate the creation of the ResourceConfig as if it was created during servlet initialization
        return ResourceConfig.forApplicationClass(Jersey1094.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.register(new JettisonFeature());
    }

    /**
     * Tests whether the {@code ApplicationHandler} is able to register and use custom binders provided by an extension of
     * {@code ResourceConfig} if only a class reference of this extension is passed in the {@code ResourceConfig} to the
     * {@code ApplicationHandler}.
     * <p/>
     * Test is trying to simulate the behaviour of Jersey as if the application was deployed into a servlet container
     * with a servlet defined in {@code web.xml} file using {@code init-param} {@code javax.ws.rs.Application}.
     */
    @Test
    public void testJersey1094() throws Exception {
        Property testInstance = new Property("myProp", "myVal");

        final Property returnedValue = target()
                .path("/")
                .request("application/json")
                .post(Entity.entity(testInstance, "application/json"), Property.class);

        assertEquals(testInstance, returnedValue);
    }

    @Test
    @Ignore("TODO: Add test for reloading resource config in the container (once it is supported)")
    public void testJersey1094ReloadResourceConfig() throws Exception {
        // TODO test reloading resource config in the container (once it is supported)
    }

}
