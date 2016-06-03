/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.logging;

import javax.ws.rs.core.MediaType;

import org.junit.Test;
import static org.glassfish.jersey.logging.LoggingFeature.Verbosity.HEADERS_ONLY;
import static org.glassfish.jersey.logging.LoggingFeature.Verbosity.PAYLOAD_ANY;
import static org.glassfish.jersey.logging.LoggingFeature.Verbosity.PAYLOAD_TEXT;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import static javax.ws.rs.core.MediaType.APPLICATION_OCTET_STREAM_TYPE;
import static javax.ws.rs.core.MediaType.TEXT_HTML_TYPE;

/**
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 */
public class LoggingInterceptorTest {

    //
    // isReadable
    //

    @Test
    public void testReadableTypeTestSubWild() {
        assertTrue(LoggingInterceptor.isReadable(new MediaType("text", "*")));
    }

    @Test
    public void testReadableTypeTestSubSomething() {
        assertTrue(LoggingInterceptor.isReadable(new MediaType("text", "something")));
    }

    @Test
    public void testReadableTypeAppSubJson() {
        assertTrue(LoggingInterceptor.isReadable(new MediaType("application", "json")));
    }

    @Test
    public void testReadableTypeAppSubBinary() {
        assertFalse(LoggingInterceptor.isReadable(new MediaType("application", "octet-stream")));
    }

    @Test
    public void testReadableTypeAppSubUnknown() {
        assertFalse(LoggingInterceptor.isReadable(new MediaType("application", "unknown")));
    }

    @Test
    public void testReadableTypeUnknownSubUnknown() {
        assertFalse(LoggingInterceptor.isReadable(new MediaType("unknown", "unknown")));
    }

    //
    // printEntity
    //

    @Test
    public void testVerbosityTextPrintTextEntity() {
        assertTrue(LoggingInterceptor.printEntity(PAYLOAD_TEXT, TEXT_HTML_TYPE));
    }

    @Test
    public void testVerbosityTextPrintBinaryEntity() {
        assertFalse(LoggingInterceptor.printEntity(PAYLOAD_TEXT, APPLICATION_OCTET_STREAM_TYPE));
    }

    @Test
    public void testVerbosityAnyPrintTextEntity() {
        assertTrue(LoggingInterceptor.printEntity(PAYLOAD_ANY, TEXT_HTML_TYPE));
    }

    @Test
    public void testVerbosityAnyPrintBinaryEntity() {
        assertTrue(LoggingInterceptor.printEntity(PAYLOAD_ANY, APPLICATION_OCTET_STREAM_TYPE));
    }

    @Test
    public void testVerbosityHeadersPrintTextEntity() {
        assertFalse(LoggingInterceptor.printEntity(HEADERS_ONLY, TEXT_HTML_TYPE));
    }

    @Test
    public void testVerbosityHeadersPrintBinaryEntity() {
        assertFalse(LoggingInterceptor.printEntity(HEADERS_ONLY, APPLICATION_OCTET_STREAM_TYPE));
    }

}
