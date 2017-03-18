/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;

import org.glassfish.jersey.server.Broadcaster;
import org.glassfish.jersey.server.BroadcasterListener;
import org.glassfish.jersey.server.ChunkedOutput;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Martin Matula
 */
public class BroadcasterTest extends JerseyTest {
    static Broadcaster<String> broadcaster = new Broadcaster<String>() {
        @Override
        public void onClose(ChunkedOutput<String> stringChunkedOutput) {
            closedOutputs.add(stringChunkedOutput);
        }
    };

    static List<ChunkedOutput<String>> outputs = new ArrayList<>();
    static List<ChunkedOutput<String>> closedOutputs = new ArrayList<>();
    static int listenerClosed = 0;

    @Path("/test")
    public static class MyResource {
        @GET
        public ChunkedOutput<String> get() {
            ChunkedOutput<String> result = new ChunkedOutput<String>() {};

            // write something to ensure the client does not get blocked on waiting for the first byte
            try {
                result.write("firstChunk");
            } catch (IOException e) {
                e.printStackTrace();
            }

            outputs.add(result);
            broadcaster.add(result);
            return result;
        }

        @POST
        public String post(String text) {
            broadcaster.broadcast(text);
            return text;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(MyResource.class);
    }

    @Test
    public void testBroadcaster() throws IOException {
        InputStream is1 = getChunkStream();
        InputStream is2 = getChunkStream();
        InputStream is3 = getChunkStream();
        InputStream is4 = getChunkStream();

        target("test").request().post(Entity.text("text1"));
        checkClosed(0);
        checkStream("firstChunktext1", is1, is2, is3, is4);

        outputs.remove(0).close();

        target("test").request().post(Entity.text("text2"));
        checkStream("text2", is2, is3, is4);
        checkClosed(1);

        outputs.remove(0).close();

        BroadcasterListener<String> bl = new BroadcasterListener<String>() {
            @Override
            public void onException(ChunkedOutput<String> stringChunkedResponse, Exception exception) {
            }

            @Override
            public void onClose(ChunkedOutput<String> stringChunkedResponse) {
                listenerClosed++;
            }
        };

        broadcaster.add(bl);

        target("test").request().post(Entity.text("text3"));
        checkClosed(2);
        assertEquals(1, listenerClosed);

        broadcaster.remove(bl);
        broadcaster.closeAll();

        checkClosed(4);
        assertEquals(1, listenerClosed);

        checkStream("text3", is3, is4);
    }

    private InputStream getChunkStream() {
        return target("test").request().get(InputStream.class);
    }

    private void checkStream(String golden, InputStream... inputStreams) throws IOException {
        byte[] bytes = golden.getBytes();
        byte[] entity = new byte[bytes.length];
        for (InputStream is : inputStreams) {
            int bytesRead = 0;
            int previous = 0;
            while ((bytesRead += is.read(entity, bytesRead, entity.length - bytesRead)) < entity.length
                    && previous != bytesRead) {
                previous = bytesRead;
            }
            assertEquals(golden, new String(entity));
        }
    }

    private void checkClosed(int count) {
        assertEquals("Closed count does not match", count, closedOutputs.size());
    }
}
