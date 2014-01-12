/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;

import javax.ws.rs.core.GenericEntity;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.util.ReflectionHelper;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

/**
 * Basic set of unit tests for OutboundEvent creation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class OutboundEventTest {
    @Test
    public void testGetCommonFields() throws Exception {
        OutboundEvent event;

        event = new OutboundEvent.Builder().id("id").name("name").data("data").build();
        assertEquals("id", event.getId());
        assertEquals("name", event.getName());
        assertEquals("data", event.getData());
        assertEquals(MediaType.TEXT_PLAIN_TYPE, event.getMediaType());
        assertEquals(SseFeature.RECONNECT_NOT_SET, event.getReconnectDelay());
        assertFalse(event.isReconnectDelaySet());

        event = new OutboundEvent.Builder().mediaType(MediaType.APPLICATION_JSON_TYPE).data("data").build();
        assertEquals(MediaType.APPLICATION_JSON_TYPE, event.getMediaType());
        try {
            new OutboundEvent.Builder().mediaType(null);
            fail("NullPointerException expected when setting null mediaType.");
        } catch (NullPointerException ex) {
            // success
        }

        event = new OutboundEvent.Builder().reconnectDelay(-1000).data("data").build();
        assertEquals(SseFeature.RECONNECT_NOT_SET, event.getReconnectDelay());
        assertFalse(event.isReconnectDelaySet());

        event = new OutboundEvent.Builder().reconnectDelay(1000).data("data").build();
        assertEquals(1000, event.getReconnectDelay());
        assertTrue(event.isReconnectDelaySet());
    }

    @Test
    public void testGetCommentOrData() throws Exception {
        assertEquals("comment", new OutboundEvent.Builder().comment("comment").build().getComment());

        assertEquals("data", new OutboundEvent.Builder().data("data").build().getData());

        try {
            new OutboundEvent.Builder().data(null);
            fail("NullPointerException expected when setting null data or data type.");
        } catch (NullPointerException ex) {
            // success
        }
        try {
            new OutboundEvent.Builder().data((Class) null, null);
            fail("NullPointerException expected when setting null data or data type.");
        } catch (NullPointerException ex) {
            // success
        }
        try {
            new OutboundEvent.Builder().data((GenericType) null, null);
            fail("NullPointerException expected when setting null data or data type.");
        } catch (NullPointerException ex) {
            // success
        }

        try {
            new OutboundEvent.Builder().build();
            fail("IllegalStateException when building event with no comment or data.");
        } catch (IllegalStateException ex) {
            // success
        }
    }

    @Test
    public void testDataType() throws Exception {
        OutboundEvent event;

        event = new OutboundEvent.Builder().data("data").build();
        assertEquals(String.class, event.getType());
        assertEquals(String.class, event.getGenericType());

        final GenericEntity<ArrayList<String>> data = new GenericEntity<ArrayList<String>>(new ArrayList<String>()) {
        };
        event = new OutboundEvent.Builder().data(data).build();
        assertEquals(ArrayList.class, event.getType());
        assertEquals(ArrayList.class, ReflectionHelper.erasure(event.getGenericType()));
        assertEquals(data.getType(), event.getGenericType());


        // data part set to an arbitrary instance as it is irrelevant for the test
        event = new OutboundEvent.Builder().data(Integer.class, "data").build();
        assertEquals(Integer.class, event.getType());
        assertEquals(Integer.class, event.getGenericType());

        // data part set to an arbitrary instance as it is irrelevant for the test
        event = new OutboundEvent.Builder().data(new GenericType<ArrayList<String>>() {
        }, "data").build();
        assertEquals(ArrayList.class, event.getType());
        assertEquals(ArrayList.class, ReflectionHelper.erasure(event.getGenericType()));
        assertEquals(new GenericEntity<ArrayList<String>>(new ArrayList<String>()) {
        }.getType(), event.getGenericType());
    }
}
