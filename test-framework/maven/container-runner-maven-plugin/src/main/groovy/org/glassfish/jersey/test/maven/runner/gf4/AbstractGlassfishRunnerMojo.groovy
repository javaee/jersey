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
package org.glassfish.jersey.test.maven.runner.gf4

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Parameter
import org.codehaus.gmaven.mojo.GroovyMojo
import org.glassfish.jersey.test.maven.runner.RunnerMojo

import java.nio.file.Paths

/**
 * Abstract class for all Glassfish4 related mojos.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
abstract class AbstractGlassfishRunnerMojo extends GroovyMojo implements RunnerMojo {

    /**
     * The {@code AS_HOME} environment variable value. If relative directory is specified (which is the default), it is derived
     * from {@link #getDistDir()} and {@link #getDistSubdir()} together with the value of this property.
     */
    @Parameter(defaultValue = "glassfish")
    String asHome

    /**
     * The Glassfish admin port
     */
    @Parameter(defaultValue = "24848")
    int adminPort

    /**
     * The timeout value in milliseconds of {@code AS_ADMIN_READTIMEOUT} environmental variable.
     */
    @Parameter(defaultValue = "60000")
    int asAdminReadTimeout

    /**
     * The location of log file. If relative path specified, it is derived from <code>{@link #asHome}/domains/
     * {@link #getDomain()}/logs/&lt;the value of this property&gt;</code>
     */
    @Parameter(defaultValue = "server.log")
    String logFile

    @Override
    void execute() throws MojoExecutionException, MojoFailureException {
        asHome = Paths.get(asHome).isAbsolute() ? asHome : Paths.get(distDir, distSubdir, asHome)
        logFile = Paths.get(logFile).isAbsolute()? logFile : Paths.get(asHome, "domains", domain, "logs", logFile)
        executeRunner()
    }

    @Override
    Map containerEnvironment() {
        return [
                "AS_HOME"             : asHome,
                "ADMINPORT"           : adminPort as String,
                "AS_ADMIN_READTIMEOUT": asAdminReadTimeout as String
        ]
    }
}