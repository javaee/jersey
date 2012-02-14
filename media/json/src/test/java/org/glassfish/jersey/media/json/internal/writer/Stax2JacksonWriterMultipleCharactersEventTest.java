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

package org.glassfish.jersey.media.json.internal.writer;

import java.io.OutputStreamWriter;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import javax.xml.stream.XMLStreamException;
import junit.framework.TestCase;
import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;

/**
 *
 * @author Jakub Podlesak
 */
public class Stax2JacksonWriterMultipleCharactersEventTest extends TestCase {


    public Stax2JacksonWriterMultipleCharactersEventTest(String testName) {
        super(testName);
    }


    public void testMultipleCharactersWithinSimpleTagEvent() throws Exception {
        Map<String, Object> props = new HashMap<String, Object>();

        JsonFactory factory = new JsonFactory();
        Writer osWriter = new OutputStreamWriter(System.out);
        JsonGenerator g;

        g = factory.createJsonGenerator(osWriter);
        final Stax2JacksonWriter s2jWriter = new Stax2JacksonWriter(g);

        try {
            s2jWriter.writeStartDocument();
            s2jWriter.writeStartElement("simpleTag");
            s2jWriter.writeCharacters("text1\n");
            s2jWriter.writeCharacters("text2\n");
            s2jWriter.writeEndElement();
            s2jWriter.writeEndDocument();
        } catch (XMLStreamException e) {
            fail();
        } finally {
            g.flush();
            System.out.println("");
        }
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
}
