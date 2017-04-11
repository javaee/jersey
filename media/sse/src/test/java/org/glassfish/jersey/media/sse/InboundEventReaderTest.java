/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.media.sse;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Collections;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.MessagingBinders;
import org.glassfish.jersey.model.internal.CommonConfig;
import org.glassfish.jersey.model.internal.ComponentBag;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class InboundEventReaderTest {

    private static final MultivaluedStringMap HEADERS;

    private InjectionManager injectionManager;

    static {
        HEADERS = new MultivaluedStringMap();
        HEADERS.put("Transfer-Encoding", Collections.singletonList("chunked"));
        HEADERS.put("Content-Type", Collections.singletonList("text/event-stream"));
    }

    @Before
    public void setup() {
        injectionManager = Injections.createInjectionManager();
        injectionManager.register(new TestBinder());

        MessageBodyFactory messageBodyFactory =
                new MessageBodyFactory(new CommonConfig(RuntimeType.SERVER, ComponentBag.EXCLUDE_EMPTY));

        injectionManager.register(Bindings.service(messageBodyFactory).to(MessageBodyWorkers.class));
        injectionManager.completeRegistration();

        messageBodyFactory.initialize(injectionManager);
    }

    @Test
    public void testReadWithStartsWithLF() throws Exception {
        InboundEvent event = parse(new ByteArrayInputStream("\nevent: custom-message".getBytes()));
        assertEquals("custom-message", event.getName());
        assertEquals(0, event.getRawData().length);
    }

    @Test
    public void testReadWithStartsWithCR() throws Exception {
        InboundEvent event = parse(new ByteArrayInputStream("\revent: custom-message".getBytes()));
        assertEquals("custom-message", event.getName());
        assertEquals(0, event.getRawData().length);
    }

    @Test
    public void testReadWithLF() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("event: custom-message\ndata: message 1".getBytes());
        InboundEvent event = parse(inputStream);
        assertEquals("custom-message", event.getName());
        assertDataEquals("message 1", event);
    }

    @Test
    public void testReadWithCRLF() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("event: custom-message\r\ndata: message 1".getBytes());
        InboundEvent event = parse(inputStream);
        assertEquals("custom-message", event.getName());
        assertDataEquals("message 1", event);
    }

    @Test
    public void testReadWithCR() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("event: custom-message\rdata: message 1".getBytes());
        InboundEvent event = parse(inputStream);
        assertEquals("custom-message", event.getName());
        assertDataEquals("message 1", event);
    }

    @Test
    public void testReadWithMultipleSpaces() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("event:     custom-message\rdata:   message 1".getBytes());
        InboundEvent event = parse(inputStream);
        assertEquals("custom-message", event.getName());
        assertDataEquals("message 1", event);
    }

    @Test
    public void testReadWithMultipleEndingDelimiter() throws Exception {
        ByteArrayInputStream inputStream = new ByteArrayInputStream("event: custom-message\rdata: message 1\r".getBytes());
        InboundEvent event = parse(inputStream);
        assertEquals("custom-message", event.getName());
        assertDataEquals("message 1", event);
    }

    private InboundEvent parse(InputStream stream) throws IOException {
        return injectionManager.getInstance(InboundEventReader.class)
                .readFrom(InboundEvent.class, InboundEvent.class, new Annotation[0],
                MediaType.valueOf(SseFeature.SERVER_SENT_EVENTS), HEADERS, stream);
    }

    private void assertDataEquals(final String expectedData, final InboundEvent event) {
        assertEquals(expectedData, event.readData());
        assertEquals(expectedData, new String(event.getRawData(), Charset.defaultCharset()));
    }

    private static class TestBinder extends AbstractBinder {

        @Override
        protected void configure() {
            install(new MessagingBinders.MessageBodyProviders(null, RuntimeType.SERVER));
            bindAsContract(InboundEventReader.class);
        }
    }
}
