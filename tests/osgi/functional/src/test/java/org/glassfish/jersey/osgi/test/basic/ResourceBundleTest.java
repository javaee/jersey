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
package org.glassfish.jersey.osgi.test.basic;

import java.io.ByteArrayOutputStream;
import java.net.URI;
import java.util.Enumeration;
import java.util.List;
import java.util.logging.Handler;
import java.util.logging.LogManager;
import java.util.logging.SimpleFormatter;
import java.util.logging.StreamHandler;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.grizzly2.httpserver.GrizzlyHttpServerFactory;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.osgi.test.util.Helper;
import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.ops4j.pax.exam.Configuration;
import org.ops4j.pax.exam.Option;
import org.ops4j.pax.exam.junit.PaxExam;

/**
 * Ensures server localization resource bundle gets loaded fine in OSGi runtime.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RunWith(PaxExam.class)
public class ResourceBundleTest {

    private static final String CONTEXT = "/jersey";

    private static final URI baseUri = UriBuilder
            .fromUri("http://localhost")
            .port(Helper.getPort())
            .path(CONTEXT).build();

    @Configuration
    public static Option[] configuration() {
        List<Option> options = Helper.getCommonOsgiOptions();
        options.addAll(Helper.expandedList(
                // PaxRunnerOptions.vmOption("-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005")
        ));

        return Helper.asArray(options);
    }

    @Path("/non-deployable")
    public static class BadResource {

        @GET
        private String getMe() {
            return "no way";
        }
    }

    @Test
    public void testBadResource() throws Exception {
        final ResourceConfig resourceConfig = new ResourceConfig(BadResource.class);

        ByteArrayOutputStream logOutput = new ByteArrayOutputStream();
        Handler logHandler = new StreamHandler(logOutput, new SimpleFormatter());

        GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, false);

        // TODO: there should be a better way to get the log output!
        final Enumeration<String> loggerNames = LogManager.getLogManager().getLoggerNames();
        while (loggerNames.hasMoreElements()) {
            String name = loggerNames.nextElement();
            if (name.startsWith("org.glassfish")) {
                LogManager.getLogManager().getLogger(Errors.class.getName()).addHandler(logHandler);
            }
        }
        GrizzlyHttpServerFactory.createHttpServer(baseUri, resourceConfig, false);
        logOutput.flush();
        final String logOutputAsString = logOutput.toString();

        Assert.assertFalse(logOutputAsString.contains("[failed to localize]"));
        Assert.assertTrue(logOutputAsString.contains("BadResource"));
    }
}
