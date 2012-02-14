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

import java.io.FilterWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import junit.framework.TestCase;

import javax.xml.bind.MarshalException;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;

import org.glassfish.jersey.media.json.JsonConfiguration;
import org.glassfish.jersey.media.json.JsonJaxbContext;
import org.glassfish.jersey.media.json.JsonMarshaller;

/**
 *
 * test case for issue#397
 *
 */
public class ExceptionFromWriterTest extends TestCase {

    @XmlRootElement()
    public static class Bean {

        private String a;
        private String b;
        private String c;

        public Bean() {
        }

        public Bean(String a, String b, String c) {
            this.a = a;
            this.b = b;
            this.c = c;
        }

        /**
         * @return the a
         */
        public String getA() {
            return a;
        }

        /**
         * @param a the a to set
         */
        public void setA(String a) {
            this.a = a;
        }

        /**
         * @return the b
         */
        public String getB() {
            return b;
        }

        /**
         * @param b the b to set
         */
        public void setB(String b) {
            this.b = b;
        }

        /**
         * @return the c
         */
        public String getC() {
            return c;
        }

        /**
         * @param c the c to set
         */
        public void setC(String c) {
            this.c = c;
        }
    }

    public void testException() throws Exception {
        _testException("{", "a", "A", "b", "B", "c", "C", "}");
    }

    public void _testException(String... values) {
        for (String value : values) {
            boolean caught = false;
            try {
                _testException(value);
            } catch (Exception ex) {
                caught = true;
                assertEquals(MarshalException.class, ex.getClass());
                assertEquals(XMLStreamException.class, ex.getCause().getClass());
            }
            assertTrue(caught);
        }
    }

    public void _testException(final String value) throws Exception {
        final JsonJaxbContext ctx = new JsonJaxbContext(
                JsonConfiguration.mapped().build(), Bean.class);
        final JsonMarshaller jm = ctx.createJSONMarshaller();
        final StringWriter sw = new StringWriter();
        final Writer w = new FilterWriter(sw) {

            @Override
            public void write(String str) throws IOException {
                if (str.contains(value)) {
                    throw new IOException();
                }
                super.write(str);
            }
        };

        Bean b = new Bean("A", "B", "C");
        jm.marshallToJSON(b, w);
    }
}
