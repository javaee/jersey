/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.performance.mbw.parametrized;

import java.lang.management.ManagementFactory;

import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import javax.management.AttributeNotFoundException;
import javax.management.InstanceNotFoundException;
import javax.management.MBeanException;
import javax.management.MBeanServer;
import javax.management.MalformedObjectNameException;
import javax.management.ObjectName;
import javax.management.ReflectionException;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.spi.TestContainerException;
import org.glassfish.jersey.test.spi.TestContainerFactory;
import org.glassfish.jersey.tests.performance.mbw.parametrized.container.JerseyAppTestContainerFactory;

import org.junit.Assert;
import org.junit.Test;

/**
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class TextPlainTest extends JerseyTest {

    @Override
    protected Application configure() {
        return new ResourceConfig();
    }

    @Override
    protected TestContainerFactory getTestContainerFactory() throws TestContainerException {
        return new JerseyAppTestContainerFactory(new String[]{"-t", "text/plain", "-j", "-u",
                getBaseUri().toString(), "--name", "VeryNiceMBean"});
    }

    @Test
    public void testGet() {
        final Response response = target().path("person").request(MediaType.TEXT_PLAIN_TYPE).get();
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("text", response.readEntity(String.class));
    }

    @Test
    public void testGetMBean() throws MalformedObjectNameException, AttributeNotFoundException, MBeanException,
            ReflectionException, InstanceNotFoundException {
        for (int i = 0; i < 10; i++) {
            final Response response = target().path("person").request(MediaType.TEXT_PLAIN_TYPE).get();
            Assert.assertEquals(200, response.getStatus());
            Assert.assertEquals("text", response.readEntity(String.class));
        }
        final MBeanServer mBeanServer = ManagementFactory.getPlatformMBeanServer();
        final ObjectName name = new ObjectName("JerseyPerformanceTests:type=VeryNiceMBean,name=gets");
        final Long count = (Long) mBeanServer.getAttribute(name, "Count");

        Assert.assertEquals(new Long(10), count);
    }

    @Test
    public void testPost() {
        final Response response = target().path("person").request(MediaType.TEXT_PLAIN_TYPE)
                .post(Entity.entity("hello", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(200, response.getStatus());
        Assert.assertEquals("hello", response.readEntity(String.class));
    }

    @Test
    public void testPut() {
        final Response response = target().path("person").request(MediaType.TEXT_PLAIN_TYPE)
                .put(Entity.entity("hello world", MediaType.TEXT_PLAIN_TYPE));
        Assert.assertEquals(204, response.getStatus());
    }

}