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
package org.glassfish.jersey.tests.integration.servlet_3_chunked_io;

import java.io.IOException;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ChunkedOutput;

/**
 * Test resource.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Path("/test")
public class TestResource {
    private static final Logger LOGGER = Logger.getLogger(TestResource.class.getName());

    /**
     * Get chunk stream of JSON data - from JSON POJOs.
     *
     * @return chunk stream.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("from-pojo")
    public ChunkedOutput<Message> getFromPojo() {
        final ChunkedOutput<Message> output = new ChunkedOutput<Message>(Message.class, "\r\n");

        new Thread() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 3; i++) {
                        output.write(new Message(i, "test"));
                        Thread.sleep(2000);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error writing chunk.", e);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Sleep interrupted.", e);
                    Thread.currentThread().interrupt();
                } finally {
                    try {
                        output.close();
                    } catch (IOException e) {
                        LOGGER.log(Level.INFO, "Error closing chunked output.", e);
                    }
                }
            }
        }.start();

        return output;
    }

    /**
     * Get chunk stream of JSON data - from string.
     *
     * @return chunk stream.
     */
    @GET
    @Produces(MediaType.APPLICATION_JSON)
    @Path("from-string")
    public ChunkedOutput<String> getFromText() {
        final ChunkedOutput<String> output = new ChunkedOutput<String>(String.class);

        new Thread() {
            @Override
            public void run() {
                try {
                    for (int i = 0; i < 3; i++) {
                        output.write(new Message(i, "test").toString() + "\r\n");
                        Thread.sleep(200);
                    }
                } catch (IOException e) {
                    LOGGER.log(Level.SEVERE, "Error writing chunk.", e);
                } catch (InterruptedException e) {
                    LOGGER.log(Level.SEVERE, "Sleep interrupted.", e);
                    Thread.currentThread().interrupt();
                } finally {
                    try {
                        output.close();
                    } catch (IOException e) {
                        LOGGER.log(Level.INFO, "Error closing chunked output.", e);
                    }
                }
            }
        }.start();

        return output;
    }
}
