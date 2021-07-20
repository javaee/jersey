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
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.tests.integration.jersey2878;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientResponseContext;
import javax.ws.rs.client.ClientResponseFilter;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;

import org.junit.Test;
import static org.junit.Assert.fail;

public class Jersey2878ITCase extends JerseyTest {

    private List<InputStream> responseInputStreams = new ArrayList<>();

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new ExternalTestContainerFactory();
    }

    @Override
    protected Application configure() {
        return new TestApplication();
    }

    @Override
    protected void configureClient(ClientConfig config) {
        ClientResponseFilter trackInputStreams = new ClientResponseFilter() {
            @Override
            public void filter(ClientRequestContext requestContext, ClientResponseContext responseContext) throws IOException {
                responseInputStreams.add(responseContext.getEntityStream());
            }
        };

        config.register(trackInputStreams);
    }

    private static void consumeStreamFully(InputStream inputStream) throws IOException {
        while (inputStream.read() != -1) {
            //consume the stream fully
        }
    }

    @Test
    public void thisShouldWorkButFails() throws Exception {
        InputStream stream = target("string").request().get(InputStream.class);
        try {
            consumeStreamFully(stream);
        } finally {
            stream.close();
        }

        try {
            stream.read();
            fail("Exception was not thrown when read() was called on closed stream! Stream implementation: " + stream.getClass());
        } catch (IOException e) {
            // this is desired
        }

        assertThatAllInputStreamsAreClosed();
    }

    @Test
    public void thisWorksButIsReallyUgly() throws Exception {
        Response response = target("string").request().get();
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("We have to manually check that the response was successful");
        }
        InputStream stream = response.readEntity(InputStream.class);
        try {
            consumeStreamFully(stream);
        } finally {
            response.close();
        }

        try {
            stream.read();
            fail("Exception was not thrown when read() was called on closed stream! Stream implementation: " + stream.getClass());
        } catch (IOException e) {
            // this is desired
        }

        assertThatAllInputStreamsAreClosed();
    }

    @Test
    public void thisAlsoFails() throws Exception {
        Response response = target("string").request().get();
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("We have to manually check that the response was successful");
        }

        InputStream stream = response.readEntity(InputStream.class);
        try {
            consumeStreamFully(stream);
        } finally {
            stream.close();
        }

        try {
            stream.read();
            fail("Exception was not thrown when read() was called on closed stream! Stream implementation: " + stream.getClass());
        } catch (IOException e) {
            // this is desired
        }

        assertThatAllInputStreamsAreClosed();
    }

    @Test
    public void worksWithACast_ifYouKnowThatYouCanCast() throws Exception {
        Response response = target("string").request().get();
        if (response.getStatusInfo().getFamily() != Response.Status.Family.SUCCESSFUL) {
            throw new RuntimeException("We have to manually check that the response was successful");
        }

        InputStream stream = (InputStream) response.getEntity();
        try {
            consumeStreamFully(stream);
        } finally {
            stream.close();
        }

        try {
            stream.read();
            fail("Exception was not thrown when read() was called on closed stream! Stream implementation: " + stream.getClass());
        } catch (IOException e) {
            // this is desired
        }

        assertThatAllInputStreamsAreClosed();
    }

    private void assertThatAllInputStreamsAreClosed() {
        if (responseInputStreams.size() == 0) {
            fail("no input stream to check");
        }
        for (InputStream stream : responseInputStreams) {
            assertClosed(stream);
        }
    }

    private void assertClosed(InputStream stream) {
        try {
            byte[] buffer = new byte[256];
            stream.read(buffer); //it's not ignored — we're checking for the exception
            fail("Stream is not closed! Stream implementation: " + stream.getClass());
        } catch (IOException e) {
            // an exception is desired
        }
    }
}
