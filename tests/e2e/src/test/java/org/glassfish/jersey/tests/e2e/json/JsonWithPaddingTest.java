/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.json;

import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.JSONP;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.spi.TestContainer;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertTrue;

/**
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
@RunWith(Parameterized.class)
public class JsonWithPaddingTest extends JerseyTest {

    @SuppressWarnings("UnusedDeclaration")
    @XmlRootElement
    public static class JsonBean {

        private String attribute;

        public JsonBean() {
        }

        public JsonBean(final String attr) {
            this.attribute = attr;
        }

        public static JsonBean createTestInstance() {
            return new JsonBean("attr");
        }

        public String getAttribute() {
            return attribute;
        }

        public void setAttribute(final String attribute) {
            this.attribute = attribute;
        }
    }

    @Path("jsonp")
    @Produces({"application/x-javascript", "application/json"})
    public static class JsonResource {

        @GET
        @Path("JsonWithPadding")
        public JsonBean getJsonWithPadding() {
            return JsonBean.createTestInstance();
        }

        @GET
        @JSONP
        @Path("JsonWithPaddingDefault")
        public JsonBean getJsonWithPaddingDefault() {
            return JsonBean.createTestInstance();
        }

        @GET
        @JSONP(queryParam = "eval")
        @Path("JsonWithPaddingQueryCallbackParam")
        public JsonBean getJsonWithPaddingQueryCallbackParam() {
            return JsonBean.createTestInstance();
        }

        @GET
        @JSONP(callback = "parse", queryParam = "eval")
        @Path("JsonWithPaddingCallbackAndQueryCallbackParam")
        public JsonBean getJsonWithPaddingCallbackAndQueryCallbackParam() {
            return JsonBean.createTestInstance();
        }

        @GET
        @JSONP(callback = "eval")
        @Path("JsonWithPaddingCallback")
        public JsonBean getJsonWithPaddingCallback() {
            return JsonBean.createTestInstance();
        }
    }

    @Parameterized.Parameters()
    public static Collection<JsonTestProvider[]> getJsonProviders() throws Exception {
        final List<JsonTestProvider[]> testProviders = new LinkedList<JsonTestProvider[]>();

        for (JsonTestProvider jsonProvider : JsonTestProvider.JAXB_PROVIDERS) {
            testProviders.add(new JsonTestProvider[]{jsonProvider});
        }

        return testProviders;
    }

    private final JsonTestProvider jsonTestProvider;

    public JsonWithPaddingTest(final JsonTestProvider jsonTestProvider) throws Exception {
        super(configureJaxrsApplication(jsonTestProvider));
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        this.jsonTestProvider = jsonTestProvider;
    }

    @Override
    protected Client getClient(final TestContainer tc, final ApplicationHandler applicationHandler) {
        final Client client = super.getClient(tc, applicationHandler);
        client.configuration().register(jsonTestProvider.getFeature());
        return client;
    }

    private static Application configureJaxrsApplication(final JsonTestProvider jsonTestProvider) {
        final ResourceConfig resourceConfig = new ResourceConfig().
                addClasses(JsonResource.class).
                register(jsonTestProvider.getFeature());

        if (jsonTestProvider.getProviders() != null) {
            resourceConfig.addSingletons(jsonTestProvider.getProviders());
        }

        return resourceConfig;
    }

    @Test
    public void testJson() throws Exception {
        final String entity =
                target("jsonp").path("JsonWithPadding").request("application/json").get(String.class);

        assertTrue(
                String.format("%s: Received JSON entity content does not match expected JSON entity content.",
                        jsonTestProvider.getClass().getSimpleName()),
                !entity.matches("^callback\\([^\\)]+\\)$"));
    }

    @Test
    public void testJsonWithPaddingDefault() throws Exception {
        test("JsonWithPaddingDefault", "callback");
    }

    @Test
    public void testJsonWithPadding() throws Exception {
        test("JsonWithPadding", "callback");
    }

    @Test
    public void testJsonWithPaddingQueryCallbackParam() throws Exception {
        test("JsonWithPaddingQueryCallbackParam", "eval", "parse");
    }

    @Test
    public void testJsonWithPaddingQueryCallbackParamDefaultQueryParam() throws Exception {
        test("JsonWithPaddingQueryCallbackParam", "callback", "parse", "callback");
    }

    @Test
    public void testJsonWithPaddingQueryCallbackParamDefaultCallback() throws Exception {
        test("JsonWithPaddingQueryCallbackParam", null, "callback");
    }

    @Test
    public void testJsonWithPaddingQueryCallbackParamNegative() throws Exception {
        test("JsonWithPaddingQueryCallbackParam", "call", "parse", true);
    }

    @Test
    public void testJsonWithPaddingCallbackAndQueryCallbackParam() throws Exception {
        test("JsonWithPaddingCallbackAndQueryCallbackParam", "eval", "run");
    }

    @Test
    public void testJsonWithPaddingCallbackAndQueryCallbackParamNegative() throws Exception {
        test("JsonWithPaddingCallbackAndQueryCallbackParam", "eval", "run", "parse", true);
    }

    @Test
    public void testJsonWithPaddingCallbackAndQueryCallbackParamDefault() throws Exception {
        test("JsonWithPaddingCallbackAndQueryCallbackParam", "evalx", "parse");
    }

    @Test
    public void testJsonWithPaddingCallbackAndQueryCallbackParamDefaultNegative() throws Exception {
        test("JsonWithPaddingCallbackAndQueryCallbackParam", "evalx", "xlave", "eval", true);
    }

    @Test
    public void testJsonWithPaddingCallback() throws Exception {
        test("JsonWithPaddingCallback", "eval", "eval");
    }

    @Test
    public void testJsonWithPaddingCallbackNegative() throws Exception {
        test("JsonWithPaddingCallback", "eval", "lave", true);
    }

    private void test(final String path, final String callback) {
        test(path, null, null, callback);
    }

    private void test(final String path, final String queryParamName, final String callback) {
        test(path, queryParamName, callback, callback, false);
    }

    private void test(final String path, final String queryParamName, final String callback, final boolean isNegative) {
        test(path, queryParamName, callback, callback, isNegative);
    }

    private void test(final String path, final String queryParamName, final String queryParamValue, final String callback) {
        test(path, queryParamName, queryParamValue, callback, false);
    }

    private void test(final String path, final String queryParamName, final String queryParamValue, final String callback,
                      final boolean isNegative) {

        WebTarget tempTarget = target("jsonp").path(path);
        if (queryParamName != null) {
            tempTarget = tempTarget.queryParam(queryParamName, queryParamValue);
        }
        final String entity =
                tempTarget.request("application/x-javascript").get(String.class);

        assertTrue(
                String.format("%s: Received JSON entity content does not match expected JSON entity content.",
                        jsonTestProvider.getClass().getSimpleName()),
                isNegative ^ entity.matches("^" + callback + "\\([^\\)]+\\)$"));
    }

}
