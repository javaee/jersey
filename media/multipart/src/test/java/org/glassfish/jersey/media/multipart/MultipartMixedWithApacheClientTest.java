/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.media.multipart;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.apache.connector.ApacheConnectorProvider;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 *
 * @author Paul Sandoz
 */
public class MultipartMixedWithApacheClientTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig(ProducesFormDataUsingMultiPart.class);
    }

    @Override
    protected void configureClient(ClientConfig config) {
        config.connectorProvider(new ApacheConnectorProvider());
        config.register(MultiPartFeature.class);
    }

    @Path("resource")
    public static class ProducesFormDataUsingMultiPart {

        @POST
        @Consumes("multipart/mixed")
        public void post(MultiPart mp) throws IOException {
            byte[] in = read(mp.getBodyParts().get(0).getEntityAs(InputStream.class));
            assertEquals(50, in.length);

            in = read(mp.getBodyParts().get(1).getEntityAs(InputStream.class));
            assertEquals(900 * 1024, in.length);
        }

        private byte[] read(InputStream in) throws IOException {
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            byte[] buffer = new byte[4096];
            int read = -1;
            while ((read = in.read(buffer)) != -1) {
                baos.write(buffer, 0, read);
            }

            return baos.toByteArray();
        }
    }

    // Test a response of type "multipart/form-data".  The example comes from
    // Section 6 of RFC 1867.
    @Test
    public void testProducesFormDataUsingMultiPart() {

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < 900 * 1024; i++) {
            baos.write(65);
        }

        MultiPart multiPartInput = new MultiPart()
                .bodyPart(new ByteArrayInputStream("01234567890123456789012345678901234567890123456789".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .bodyPart(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        target().path("resource").request().post(Entity.entity(multiPartInput,
                MultiPartMediaTypes.createMixed()));
    }

    @Test
    public void testChunkedEncodingUsingMultiPart() {
        final Client client = client();
        client.property(ClientProperties.CHUNKED_ENCODING_SIZE, 1024);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        for (int i = 0; i < 900 * 1024; i++) {
            baos.write(65);
        }

        MultiPart multiPartInput = new MultiPart()
                .bodyPart(new ByteArrayInputStream("01234567890123456789012345678901234567890123456789".getBytes()),
                        MediaType.APPLICATION_OCTET_STREAM_TYPE)
                .bodyPart(baos.toByteArray(), MediaType.APPLICATION_OCTET_STREAM_TYPE);

        client.target(getBaseUri()).path("resource").request()
                .post(Entity.entity(multiPartInput, MultiPartMediaTypes.createMixed()));
    }
}
