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
import junit.framework.TestCase;

import javax.xml.bind.Unmarshaller;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import javax.xml.bind.JAXBContext;
import org.glassfish.jersey.media.json.JsonConfiguration;
import org.glassfish.jersey.media.json.internal.reader.JsonXmlStreamReader;

/**
 * Test for JERSEY-954
 *
 * @author Jakub Podlesak
 */
public class JsonXmlStreamReaderInvalidInputTest extends TestCase {

    String[] invalidInputs = new String [] {
        "{",
        "[",
        // only the above input made the reader stuck
        ",",
        "",
        "\"\"",
        "\"",
        "\'",
        "}",
        "{{",
        "{}",
        "}}",
        "}{",
        "]",
        "lojza",
        "12",
        "\"12"
    };

    public void testInvalidInput() throws Exception {

        final JAXBContext ctx = JAXBContext.newInstance(TwoListsWrapperBean.class);

        for (String input : invalidInputs) {
            assertTrue("input \"" + input + "\" caused an infinite loop",
                    terminatesBeforeTimeout(ctx,
                    JsonConfiguration.DEFAULT, input));
        }
    }

    public boolean terminatesBeforeTimeout(final JAXBContext jaxbContext, final JsonConfiguration config, final String input) throws Exception {
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final ExecutorService executor = Executors.newSingleThreadExecutor();
        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    unmarshaller.unmarshal(new JsonXmlStreamReader(new StringReader(input), config), TwoListsWrapperBean.class);
                } catch (Exception ex) {
                    System.out.println(ex);
                    // an exception does not hurt here
                }
            }
        });
        executor.shutdown();
        return executor.awaitTermination(5000, TimeUnit.MILLISECONDS);
    }
}
