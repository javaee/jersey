/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.media.json.internal;

import java.io.StringReader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.Map;
import junit.framework.TestCase;

import org.glassfish.jersey.media.json.JsonConfiguration;
import org.glassfish.jersey.media.json.JsonJaxbContext;
import org.glassfish.jersey.media.json.JsonMarshaller;
import org.glassfish.jersey.media.json.JsonUnmarshaller;

/**
 *
 * @author Jakub.Podlesak@Sun.COM
 */
public class NamespaceAttributeTest extends TestCase {

    final NamespaceBeanWithAttribute one = JSONTestHelper.createTestInstance(NamespaceBeanWithAttribute.class);

    public void _disabledFailing_testBadgerfish() throws Exception {
        tryConfiguration(JsonConfiguration.badgerFish().build());
    }

    public void testMappedJettison() throws Exception {
        Map<String, String> ns2json = new HashMap<String, String>();
        ns2json.put("http://example.com", "example");
        tryConfiguration(JsonConfiguration.mappedJettison().xml2JsonNs(ns2json).build());
    }

    public void testNatural() throws Exception {
        tryConfiguration(JsonConfiguration.natural().rootUnwrapping(false).build());
    }

    public void testMapped() throws Exception {
        Map<String, String> ns2json = new HashMap<String, String>();
        ns2json.put("http://example.com", "example");
        tryConfiguration(JsonConfiguration.mapped().xml2JsonNs(ns2json).rootUnwrapping(false).build());
    }

    public void testMappedCustomNsSeparator() throws Exception {
        Map<String, String> ns2json = new HashMap<String, String>();
        ns2json.put("http://example.com", "example");
        tryConfiguration(JsonConfiguration.mapped().xml2JsonNs(ns2json).nsSeparator(':').rootUnwrapping(false).build());
    }

    private void tryConfiguration(JsonConfiguration configuration) throws Exception {

        final JsonJaxbContext ctx = new JsonJaxbContext(configuration, NamespaceBeanWithAttribute.class);
        final JsonMarshaller jm = ctx.createJSONMarshaller();
        final JsonUnmarshaller ju = ctx.createJSONUnmarshaller();


        NamespaceBeanWithAttribute beanTwo;

        final StringWriter sw = new StringWriter();

        jm.marshallToJSON(one, sw);

        System.out.println(String.format("%s", sw));

        beanTwo = ju.unmarshalFromJSON(new StringReader(sw.toString()), NamespaceBeanWithAttribute.class);

        assertEquals(one, beanTwo);
    }
}
