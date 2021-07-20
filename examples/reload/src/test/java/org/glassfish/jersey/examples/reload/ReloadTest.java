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

package org.glassfish.jersey.examples.reload;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.AbstractContainerLifecycleListener;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;

import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * This is to test the reload feature without updating the resources text file.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class ReloadTest extends JerseyTest {

    private static Container container;

    @Override
    protected ResourceConfig configure() {
        enable(TestProperties.LOG_TRAFFIC);

        final ResourceConfig result = new ResourceConfig(ArrivalsResource.class);

        result.registerInstances(new AbstractContainerLifecycleListener() {
            @Override
            public void onStartup(Container container) {
                ReloadTest.container = container;
            }
        });

        return result;
    }

    @Test
    public void testReload() {

        // hit arrivals
        Response response = target().path("arrivals").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());

        // make sure stats resource is not found
        response = target().path("stats").request(MediaType.TEXT_PLAIN).get();
        assertEquals(404, response.getStatus());

        // add stats resource
        container.reload(new ResourceConfig(ArrivalsResource.class, StatsResource.class));

        // check stats
        response = target().path("stats").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());
        assertTrue("1 expected as number of arrivals hits in stats", response.readEntity(String.class).contains("1"));

        // another arrivals hit
        response = target().path("arrivals").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());

        // check updated stats
        response = target().path("stats").request(MediaType.TEXT_PLAIN).get();
        assertEquals(200, response.getStatus());
        assertTrue("2 expected as number of arrivals hits in stats", response.readEntity(String.class).contains("2"));

        // remove stats
        container.reload(new ResourceConfig(ArrivalsResource.class));

        // make sure stats resource is not found
        response = target().path("stats").request(MediaType.TEXT_PLAIN).get();
        assertEquals(404, response.getStatus());
    }
}
