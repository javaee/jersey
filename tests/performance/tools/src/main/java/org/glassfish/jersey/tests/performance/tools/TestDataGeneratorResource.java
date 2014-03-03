/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.performance.tools;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Resource for generating data for performance tests.
 * For more information, see {@link TestDataGeneratorApp}
 */
@Path("generate")
public class TestDataGeneratorResource {

    private static Logger LOG = Logger.getLogger(TestDataGeneratorResource.class.getName());

    /**
     * Generates plain text based data from a complex testing bean.
     * @return result of a call to a {@link Object#toString()} method of the populated bean.
     */
    @GET
    @Path("complex/text")
    public String generateComplexText() {
        return getComplexTestBean().toString();
    }

    /**
     * Generates json based data from a complex testing bean.
     * @return the bean to be converted to json
     */
    @GET
    @Path("complex/json")
    @Produces(MediaType.APPLICATION_JSON)
    public TestBean generateComplexJson() {
        return getComplexTestBean();
    }

    /**
     * Generates xml based data from a complex testing bean.
     * @return the bean to be converted to xml
     */
    @GET
    @Path("complex/xml")
    @Produces(MediaType.APPLICATION_XML)
    public TestBean generateComplexXml() {
        return getComplexTestBean();
    }

    /**
     * Generates plain text based data from a simple testing bean.
     * @return result of a call to a {@link Object#toString()} method of the populated bean.
     */
    @GET
    @Path("simple/text")
    public String generateSimpleText() {
        return getSimpleTestBean().toString();
    }

    /**
     * Generates json based data from a simple testing bean.
     * @return the bean to be converted to json
     */
    @GET
    @Path("simple/json")
    @Produces(MediaType.APPLICATION_JSON)
    public Person generateSimpleJson() {
        return getSimpleTestBean();
    }

    /**
     * Generates xml based data from a simple testing bean.
     * @return the bean to be converted to xml
     */
    @GET
    @Path("simple/xml")
    @Produces(MediaType.APPLICATION_XML)
    public Person generateSimpleXml() {
        return getSimpleTestBean();
    }

    private TestBean getComplexTestBean() {
        TestBean bean = new TestBean();
        try {
            TestDataGenerator.populateBeanByAnnotations(bean);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error while populating the testing bean.", e);
        }
        return bean;
    }

    private Person getSimpleTestBean() {
        Person bean = new Person();
        try {
            TestDataGenerator.populateBeanByAnnotations(bean);
        } catch (Exception e) {
            LOG.log(Level.SEVERE, "Error while populating the testing bean.", e);
        }
        return bean;
    }
}
