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

package org.glassfish.jersey.tests.e2e.server.mvc;

import java.io.InputStream;
import java.util.Properties;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.mvc.ErrorTemplate;
import org.glassfish.jersey.server.mvc.beanvalidation.MvcBeanValidationFeature;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.tests.e2e.server.mvc.provider.TestViewProcessor;

import org.hibernate.validator.constraints.Length;
import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.fail;

/**
 * @author Michal Gajdos
 */
public class BeanValidationErrorTemplateTest extends JerseyTest {

    private Properties props;

    @Before
    public void setUp() throws Exception {
        props = new Properties();

        super.setUp();
    }

    @Override
    protected Application configure() {
        enable(TestProperties.DUMP_ENTITY);
        enable(TestProperties.LOG_TRAFFIC);

        return new ResourceConfig(ErrorTemplateResource.class)
                .register(MvcBeanValidationFeature.class)
                .register(TestViewProcessor.class);
    }

    @Path("/")
    @Consumes("text/plain")
    public static class ErrorTemplateResource {

        @POST
        @Path("params")
        @ErrorTemplate
        public String invalidParams(@Length(min = 5) final String value) {
            fail("Should fail on Bean Validation!");
            return value;
        }

        @POST
        @Path("return")
        @ErrorTemplate
        @Length(min = 5)
        public String invalidReturnValue(final String value) {
            return value;
        }
    }

    @Test
    public void testInvalidParams() throws Exception {
        final Response response = target("params").request().post(Entity.text("foo"));
        props.load(response.readEntity(InputStream.class));

        assertThat(response.getStatus(), equalTo(400));
        assertThat(props.getProperty("model"),
                equalTo("{org.hibernate.validator.constraints.Length.message}_ErrorTemplateResource.invalidParams.arg0_foo"));
    }

    @Test
    public void testInvalidReturnValue() throws Exception {
        final Response response = target("return").request().post(Entity.text("foo"));
        props.load(response.readEntity(InputStream.class));

        assertThat(response.getStatus(), equalTo(500));
        assertThat(props.getProperty("model"),
                equalTo("{org.hibernate.validator.constraints.Length.message}_ErrorTemplateResource.invalidReturnValue."
                        + "<return value>_foo"));
    }
}
