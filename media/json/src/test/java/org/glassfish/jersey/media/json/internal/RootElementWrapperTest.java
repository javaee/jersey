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

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import junit.framework.TestCase;

/**
 *
 * @author Jakub Podlesak
 */
public class RootElementWrapperTest extends TestCase {

    public RootElementWrapperTest(String testName) {
        super(testName);
    }


    public void testWrapInput() throws Exception {

        String unwrappedJson = "{\"one\":1}";
        String rootName = "root";
        String expJson = "{\"root\":{\"one\":1}}";
        InputStream input = new ByteArrayInputStream(unwrappedJson.getBytes());

        InputStream result = RootElementWrapper.wrapInput(input, rootName);

        BufferedReader resultReader = new BufferedReader(new InputStreamReader(result));
        String resultJson = resultReader.readLine();

        assertEquals(expJson, resultJson);
    }


    public void testUnwrapInputForObject() throws Exception {
        _testUnwrapInput("{\"root\":{\"one\":1}}", "{\"one\":1}");
    }

    public void testUnwrapInputForArray() throws Exception {
        _testUnwrapInput("{\"root\":[\"one\", \"two\"]}", "[\"one\",\"two\"]");
    }

    public void testUnwrapInputForPrimitive() throws Exception {
        _testUnwrapInput("{\"root\":\"one\"}", "\"one\"");
    }

    public void testUnwrapInputForComplex() throws Exception {
        _testUnwrapInput("{\"root\":{\"one\":1,\"two\":[1,2,3],\"three\":{\"name\":\"John\",\"surname\":\"Big\"}}}",
                "{\"one\":1,\"two\":[1,2,3],\"three\":{\"name\":\"John\",\"surname\":\"Big\"}}");
    }

    public void _testUnwrapOutputForObject() throws Exception {
        _testUnwrapOutput("{\"root\":{\"one\":1}}", "{\"one\":1}");
    }


    private void _testUnwrapInput(String wrappedJson, String expectedJson) throws Exception {

        InputStream input = new ByteArrayInputStream(wrappedJson.getBytes());

        InputStream result = RootElementWrapper.unwrapInput(input);

        BufferedReader resultReader = new BufferedReader(new InputStreamReader(result));
        String resultJson = resultReader.readLine();

        System.out.println(String.format("Expected: %s, \nreturned: %s", expectedJson, resultJson));

        assertEquals(expectedJson, resultJson);
    }

    private void _testUnwrapOutput(String wrappedJson, String expectedJson) throws Exception {

        OutputStream result = new ByteArrayOutputStream();

        OutputStream inputOutput = RootElementWrapper.unwrapOutput(result);

        inputOutput.write(wrappedJson.getBytes());

        String resultJson = result.toString();

        System.out.println(String.format("Expected: %s, \nreturned: %s", expectedJson, resultJson));

        assertEquals(expectedJson, resultJson);
    }

}
