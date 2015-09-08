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

import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugins.annotations.Parameter
import org.glassfish.jersey.client.ClientProperties

import javax.ws.rs.client.Client
import javax.ws.rs.client.ClientBuilder
import javax.ws.rs.client.WebTarget

/**
 * Common functionality of Redeploy Mojos.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
trait CommonRedeploy extends CommonStop {

    /**
     * The exit code that denotes a detected memory leak.
     */
    final static int MEMORY_LEAK_DETECTED_ERROR_EXIT_CODE = 68

    /**
     * The path and query to execute after each application redeploy
     */
    @Parameter(required = true, name = "requestPathQuery")
    String requestPathQuery

    /**
     * The number of redeploys to execute.
     */
    @Parameter(required = true, name = "redeployCount")
    int redeployCount

    /**
     * The http method to use while executing the http request to {@link #requestPathQuery}.
     */
    @Parameter(defaultValue = "GET", name = "method")
    String method

    /**
     * A http status to expect while queering the application between redeploys.
     */
    @Parameter(defaultValue = "200", name = "expectedStatus")
    int expectedStatus

    /**
     * The archive location of an application to be redeployed.
     */
    @Parameter(required = true, name = "warPath")
    File warPath

    @Parameter(defaultValue = "false", name = "skipRedeploy", property = "jersey.runner.skipRedeploy")
    boolean skipRedeploy

    def redeployAndSendRequest(String shell, String stopShell) {
        redeployAndSendRequest(shell, stopShell, null)
    }

    def redeployAndSendRequest(String shell, String stopShell, Map env) {
        final Client client = ClientBuilder.newClient()
                .property(ClientProperties.CONNECT_TIMEOUT, 30000)
                .property(ClientProperties.READ_TIMEOUT, 30000)
        final WebTarget target = client.target("http://localhost:${port}/${normalizedRequestPathQuery()}")

        getLog().info("WEB Target URI: " + target.getUri())

        try {
            int i = 1
            for (; i <= redeployCount; ++i) {
                getLog().info("Executing request $method to " + target.getUri() + " (iteration $i/$redeployCount)")
                def response = target.request().method(method)
                getLog().info("Received http status " + response.getStatus())

                if (expectedStatus != response?.getStatus()) {
                    throw new MojoExecutionException("After $i iterations, the http request ended with unexpected code! Expected: <$expectedStatus>, actual: <${response.getStatus()}>")
                }

                executeShell(shell, env)
            }
            log.info("The test ended after ${i - 1} iterations.")
        } catch (Exception e) {
            log.error("Exception encountered during the redeploy cycle! ", e)
            throw e
        } finally {
            try {
                executeShell(stopShell, env)
            } catch (MojoExecutionException e) {
                getLog().warn("Stop command threw an exception: " + e.getMessage())
                // not re-throwing so that the possible original exception is not masked and success exit is preserved
            } finally {
                try {
                    executeShell("/runner/verify.sh", ["ERROR_EXIT_CODE": MEMORY_LEAK_DETECTED_ERROR_EXIT_CODE as String])
                } catch (ShellMojoExecutionException e) {
                    if (e.errorCode == MEMORY_LEAK_DETECTED_ERROR_EXIT_CODE) {
                        // re-throw the exception and mask all the other exceptions because we verified that an ERROR (e.g. OutOfMemoryError) occurred
                        throw e
                    }
                }
            }
        }
    }

    Map commonEnvironment() {
        return [
                "WAR_PATH"          : warPath.absolutePath,
                "REQUEST_PATH_QUERY": normalizedRequestPathQuery(),
                "SKIP_REDEPLOY"     : skipRedeploy as String
        ] << super.commonEnvironment()
    }

    private String normalizedRequestPathQuery() {
        while (requestPathQuery.startsWith("/")) {
            requestPathQuery = requestPathQuery.substring(1, requestPathQuery.length())
        }
        return requestPathQuery
    }

    void setRequestPathQuery(final String requestPathQuery) {
        this.requestPathQuery = requestPathQuery
    }

    void setRedeployCount(final int redeployCount) {
        this.redeployCount = redeployCount
    }

    void setMethod(final String method) {
        this.method = method
    }

    void setExpectedStatus(final int expectedStatus) {
        this.expectedStatus = expectedStatus
    }

    void setWarPath(final File warPath) {
        this.warPath = warPath
    }

    void setSkipRedeploy(final boolean skipRedeploy) {
        this.skipRedeploy = skipRedeploy
    }
}
