/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.memleaks.testleak;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.test.memleak.common.AbstractMemoryLeakWebAppTest;
import org.glassfish.jersey.test.memleak.common.MemoryLeakSucceedingTimeout;

import org.junit.Assert;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.Timeout;
import static org.junit.Assert.fail;

/**
 * Performs the test of a memory leaking RESTful resource.
 * <p/>
 * This test class should be used to test the memory leaking test infrastructure.
 */
public class MemoryLeakingResourceITCase extends AbstractMemoryLeakWebAppTest {

    @Rule
    public Timeout globalTimeout = new MemoryLeakSucceedingTimeout(100_000);

    @Override
    protected Application configure() {
        return new TestApplication();
    }

    /**
     * This test runs i single request; as such, it may or may not cause a memory leak (and end with a failure).
     */
    @Test
    public void testTheLeak() {

        final WebTarget webTarget = target("invoke").queryParam("size", 1024 * 1024);
        System.out.println(webTarget.getUri());
        final Response response = webTarget.request().post(null);
        Assert.assertEquals(200, response.getStatus());
    }

    /**
     * This test should fail after certain period of time (depending on the JVM settings too).
     */
    @Test
    public void testTheLeakEndless() {

        while (true) {
            System.out.print(".");
            final Response response = target("invoke").queryParam("size", 1024 * 1024).request().post(null);

            if (response.getStatus() != 200) {
                fail("The server was unable to fulfill the request! This may indicate that OutOfMemory exception occurred.");
            }
        }
    }

}
