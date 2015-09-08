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
package org.glassfish.jersey.test.maven.runner

import org.apache.maven.plugins.annotations.Parameter

/**
 * Common functionality of Start and Deploy Mojos.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
trait CommonStartAndDeploy implements RunnerMojo {

    /**
     * The archive location of an application to be deployed.
     */
    @Parameter(required = true, name = "warPath")
    File warPath

    /**
     * The maximum heap size. Must conform to java {@code Xmx} standards.
     */
    @Parameter(defaultValue = "256m", name = "maxHeap")
    String maxHeap

    /**
     * Additional JVM arguments to pass to the container jvm.
     */
    @Parameter(defaultValue = "", name = "jvmArgs", property = "jersey.runner.jvmArgs")
    String jvmArgs

    /**
     * Whether to skip deployment.
     */
    @Parameter(defaultValue = "false", name = "skipDeploy", property = "jersey.runner.skipDeploy")
    boolean skipDeploy

    /**
     * Whether to skip check for running java processes with a magic identifier
     * {@code jersey.config.test.memleak.*.magicRunnerIdentifier} which provides a way to prevent multiple containers
     * to run in parallel. By default, when another container instance is running, the startup fails in order to prevent
     * an uncontrolled explosion of number of running java containers that weren't supposed to run possibly.
     */
    @Parameter(defaultValue = "false", name = "skipCheck", property = "jersey.runner.skipCheck")
    boolean skipCheck

    Map commonEnvironment() {
        return [
                "WAR_PATH"   : warPath.absolutePath,
                "MAX_HEAP"   : maxHeap,
                "PORT"       : port as String,
                "SKIP_DEPLOY": skipDeploy as String,
                "JVM_ARGS"   : jvmArgs ?: "",
                "SKIP_CHECK" : skipCheck as String
        ]
    }

    void startAndDeployStopOnFailure(String shell, String stopShell) {
        startAndDeployStopOnFailure(shell, stopShell, null)
    }

    void startAndDeployStopOnFailure(String shell, String stopShell, Map env) {
        try {
            executeShell(shell, env)
        } catch (ShellMojoExecutionException e) {
            // regardless of the state we need to be sure, the container wasn't left in a running state
            try {
                executeShell(stopShell)
            } catch (ShellMojoExecutionException se) {
                log.warn("Container stop ended with error.", se)
                // not re-trowing
            }

            throw e
        }
    }

    void setSkipDeploy(final boolean skipDeploy) {
        this.skipDeploy = skipDeploy
    }

    void setMaxHeap(final String maxHeap) {
        this.maxHeap = maxHeap
    }

    void setWarPath(final File warPath) {
        this.warPath = warPath
    }

    void setJvmArgs(final String jvmArgs) {
        this.jvmArgs = jvmArgs
    }

    void setSkipCheck(final boolean skipCheck) {
        this.skipCheck = skipCheck
    }
}
