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
package org.glassfish.jersey.server.spring.boot.test;

import static org.hamcrest.CoreMatchers.is;
import static org.junit.Assert.assertThat;

import java.io.File;
import java.io.FileInputStream;
import java.io.InputStream;
import java.util.Properties;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientBuilder;

import org.junit.After;
import org.junit.Before;
import org.junit.Test;

/**
 * This test case is spring-boot-integration.
 * 
 * @author Manabu Otake (manabu2783 at hotmail.com)
 */
public class IntegrationTest {

    private static final Logger L = Logger.getLogger(IntegrationTest.class.getName());
    
    private String[] mavenPackageCommand = {"mvn", "package",
            "-Dmaven.test.skip=true", "-Dsource.skip=true" };

    private Process process;

    @Before
    public void setup() throws Exception {
        executePackage();
        runServer();
    }

    @After
    public void teardown() throws Exception {
        if (process != null)
            process.destroy();
    }

    @Test
    public void testJerseyIntegration() throws Exception {
        Thread.sleep(5000);
        String response = ClientBuilder.newClient()
                .target("http://127.0.0.1:1234/test/").path("integration")
                .request().get(String.class);
        assertThat(response, is("success"));
    }

    protected void executePackage() throws Exception {
        read(builder(mavenPackageCommand).start().getInputStream());
    }

    protected void runServer() throws Exception {
        final ProcessBuilder builder = builder("java", "-jar",
                "target/spring-boot-" + getVersion() + ".jar");
        Runnable runner = new Runnable() {
            @Override
            public void run() {
                try {
                    process = builder.start();
                    read(process.getInputStream());
                } catch (Exception e) {
                    // ignore
                }
            }
        };
        Thread server = new Thread(runner);
        server.start();
    }

    protected ProcessBuilder builder(String... command) throws Exception {
        return new ProcessBuilder(command);
    }

    protected void read(InputStream stream) throws Exception {
        StringBuilder log = new StringBuilder();
        while (true) {
            int c = stream.read();
            if (c == -1) {
                stream.close();
                break;
            }
            log.append((char) c);
        }
        L.info(log.toString());
    }

    protected String getVersion() throws Exception {
        Properties p = new Properties();
        p.load(new FileInputStream(new File(
                "target/maven-archiver/pom.properties")));
        return p.getProperty("version", "");
    }

}
