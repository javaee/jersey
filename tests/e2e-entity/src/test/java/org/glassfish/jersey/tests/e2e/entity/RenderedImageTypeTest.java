/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.entity;

import java.awt.image.BufferedImage;
import java.awt.image.RenderedImage;
import java.io.InputStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Test;
import static org.hamcrest.CoreMatchers.notNullValue;
import static org.hamcrest.Matchers.greaterThan;
import static org.junit.Assert.assertThat;

/**
 * @author Michal Gajdos
 */
public class RenderedImageTypeTest extends JerseyTest {

    @Path("/")
    public static class ImageResource {

        @Consumes("image/gif")
        @Produces("image/png")
        @POST
        public RenderedImage postGif(final RenderedImage image) {
            return image;
        }

        @Consumes("image/png")
        @Produces("image/png")
        @POST
        public RenderedImage postPng(final RenderedImage image) {
            return image;
        }

        @Path("sub")
        @Consumes("application/octet-stream")
        @Produces("image/png")
        @POST
        public RenderedImage postUndefined(final BufferedImage image) {
            return image;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(ImageResource.class);
    }

    @Test
    public void testPostPng() throws Exception {
        final InputStream stream = getClass().getResourceAsStream("Jersey_yellow.png");
        Response response = target().request().post(Entity.entity(stream, "image/png"));
        assertThat(Long.valueOf(response.getHeaderString("Content-Length")), greaterThan(0L));

        final RenderedImage image = response.readEntity(RenderedImage.class);
        assertThat(image, notNullValue());

        response = target().request().post(Entity.entity(image, "image/png"));
        assertThat(response.readEntity(RenderedImage.class), notNullValue());
        assertThat(Long.valueOf(response.getHeaderString("Content-Length")), greaterThan(0L));
    }

    @Test
    public void testPostGif() throws Exception {
        final InputStream stream = getClass().getResourceAsStream("duke_rocket.gif");
        Response response = target().request().post(Entity.entity(stream, "image/gif"));
        assertThat(Long.valueOf(response.getHeaderString("Content-Length")), greaterThan(0L));

        final RenderedImage image = response.readEntity(RenderedImage.class);
        assertThat(image, notNullValue());

        response = target().request().post(Entity.entity(image, "image/png"));
        assertThat(response.readEntity(RenderedImage.class), notNullValue());
        assertThat(Long.valueOf(response.getHeaderString("Content-Length")), greaterThan(0L));
    }

    @Test
    public void testPostUndefined() throws Exception {
        final InputStream stream = getClass().getResourceAsStream("duke_rocket.gif");
        Response response = target("sub").request().post(Entity.entity(stream, "application/octet-stream"));
        assertThat(Long.valueOf(response.getHeaderString("Content-Length")), greaterThan(0L));

        final RenderedImage image = response.readEntity(RenderedImage.class);
        assertThat(image, notNullValue());

        response = target().request().post(Entity.entity(image, "image/png"));
        assertThat(response.readEntity(RenderedImage.class), notNullValue());
        assertThat(Long.valueOf(response.getHeaderString("Content-Length")), greaterThan(0L));
    }
}
