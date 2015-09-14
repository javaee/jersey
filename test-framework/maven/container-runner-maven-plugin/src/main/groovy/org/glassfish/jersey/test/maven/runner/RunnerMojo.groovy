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

import com.google.common.collect.EvictingQueue
import org.apache.maven.plugin.MojoExecutionException
import org.apache.maven.plugin.MojoFailureException
import org.apache.maven.plugins.annotations.Parameter
import org.apache.maven.project.MavenProject

import java.nio.file.Files
import java.nio.file.Paths
import java.util.regex.Pattern

/**
 * Abstract class for all Runner Mojos.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
trait RunnerMojo implements SuperRunnerMojo {

    /**
     * The Maven project to analyze.
     */
    @Parameter(defaultValue = "\${project}", readonly = true, name = "project")
    MavenProject project

    /**
     * The port where the container will listen for requests.
     */
    @Parameter(defaultValue = "18080", name = "port")
    int port

    /**
     * The name of the application to be identified with in the container
     */
    @Parameter(defaultValue = "jax-rs-memleak-test-app", name = "applicationName")
    String applicationName

    /**
     * The container domain name (must be a valid single directory name; that is, not a directory path)
     */
    @Parameter(defaultValue = "memleak_test_domain", name = "domain")
    String domain

    /**
     * The context root where the deployed application will be accessible
     */
    @Parameter(required = true, name = "contextRoot")
    String contextRoot

    /**
     * The name of directory where to container is located. The container archive distribution is unpacked to this destination.
     */
    @Parameter(defaultValue = "\${project.build.directory}", name = "distDir")
    String distDir

    /**
     * The name of the directory located in the root of the container distribution. In other words, when the
     * archive distribution is unpacked in directory {@link #distDir} the resulting directory name in the {@link #distDir} where the content of the
     * distribution is located is denoted by this property.
     */
    @Parameter(required = true, name = "distSubdir")
    String distSubdir

    /**
     * Whether to skip start and stop. This also skips the download. Useful when the container is already running.
     */
    @Parameter(defaultValue = "false", name = "skipStartAndStop", property = "jersey.runner.skipStartAndStop")
    boolean skipStartAndStop

    /**
     * The location of a directory where a content of executed shell scripts is dumped.
     */
    @Parameter(defaultValue = "\${project.build.directory}/tmp", name = "scriptsDirectory")
    String scriptsDirectory

    /**
     * Parse a hashbang from the first line of the script; if arguments are present, they're included
     * in the executable array (can be passed to {@link ProcessBuilder#command(java.lang.String ...)}).
     *
     * @param shellContent as a String (i.e., the whole script content)
     * @return a string array containing the shell executable and its arguments (defaults to ["sh"])
     */
    static String[] shellExecutable(String shellContent) {

        def string = Pattern.compile("#!([^\r\n]*).*", Pattern.DOTALL).matcher(shellContent).replaceFirst("\$1")

        def command = string.split(" +(?=((.*?(?<!\\\\)'){2})*[^']*\$)")

        return command?.length > 0 && Files.exists(Paths.get(command[0])) ? command : ["sh"]
    }

    abstract Map commonEnvironment()

    abstract Map containerEnvironment()

    abstract String getLogFile()

    abstract void executeRunner() throws MojoExecutionException, MojoFailureException

    private Map environment() {
        return [
                "PORT"            : port as String,
                "DEBUG"           : (getLog()?.isDebugEnabled() ?: "false") as String,
                "APPLICATION_NAME": applicationName,
                "DOMAIN"          : domain,
                "LOGFILE"         : logFile,
                "CONTEXT_ROOT"    : contextRoot,
                "SKIP_START_STOP" : skipStartAndStop as String,

                "DIST_DIR"        : distDir,
                "DIST_SUBDIR"     : distSubdir,
        ]
    }

    def executeShell(String shell) {
        executeShell(shell, null)
    }

    def executeShell(String shell, Map<String, String> mojoSpecificEnvironment) {
        executeShell(true, shell, mojoSpecificEnvironment)
    }

    int executeShell(boolean exceptionOnError, String shell, Map<String, String> mojoSpecificEnvironment) {
        def shellContent = getClass().getResourceAsStream(shell).getBytes()

        def builder = new ProcessBuilder(shellExecutable(new String(shellContent)))
        def map = [:]
        map.putAll(mojoSpecificEnvironment ?: Collections.EMPTY_MAP)
        map.putAll(commonEnvironment() ?: Collections.EMPTY_MAP)
        map.putAll(containerEnvironment() ?: Collections.EMPTY_MAP)
        map.putAll(environment() ?: Collections.EMPTY_MAP)
        builder.environment().putAll(map)

        outputScriptContent(shellContent, map, shell)

        // stderr and stdout redirected so that we don't have to allocated additional thread to consume a stream
        builder.redirectErrorStream(true)

        def process = builder.start()

        // pipe shell script into the shell
        def outputStream = new BufferedOutputStream(process.out)
        outputStream.write(shellContent)
        outputStream.close()

        def lastLinesQueue = EvictingQueue.<String>create(lastLinesCount)

        process.in.eachLine {
            lastLinesQueue.add(it)
            if (getLog()?.isDebugEnabled() && it.startsWith("+")) {
                getLog().debug(it)
            } else {
                getLog().info(it)
            }
        }

        process.waitFor()

        if (process.exitValue() != 0 && exceptionOnError) {
            throw new ShellMojoExecutionException("The shell script: '" + shell + "' ended with non-zero exit value!",
                    process.exitValue(),
                    lastLinesQueue)
        }

        return process.exitValue()
    }

    def outputScriptContent(byte[] shellContent, Map environment, String shell) {
        try {
            StringBuilder sb = new StringBuilder()
            for (Map.Entry entry : environment.entrySet()) {
                sb.append(System.lineSeparator()).append(entry.getKey()).append("=\"").append(entry.getValue()).append("\"")
            }
            sb.append(System.lineSeparator())
            def matcher = Pattern.compile("(#![^\r\n]*)(.*)", Pattern.DOTALL).matcher(new String(shellContent))
            def string = matcher.matches() ? matcher.replaceFirst("\$1${sb.toString()}\$2") : sb.append(shellContent).toString()
            Paths.get(scriptsDirectory).toFile().mkdirs()
            def reExecutableShell = Paths.get(scriptsDirectory, Paths.get(shell).fileName.toString())
            Files.write(reExecutableShell, string.bytes)
            getLog().info("Re-executable shell written to: $reExecutableShell")
        } catch (Exception e) {
            getLog().warn("Unable to output re-executable shell content of $shell", e)
        }
    }

    void setProject(final MavenProject project) {
        this.project = project
    }

    void setPort(final int port) {
        this.port = port
    }

    void setApplicationName(final String applicationName) {
        this.applicationName = applicationName
    }

    void setDomain(final String domain) {
        this.domain = domain
    }

    void setContextRoot(final String contextRoot) {
        this.contextRoot = contextRoot
    }

    void setDistDir(final String distDir) {
        this.distDir = distDir
    }

    void setDistSubdir(final String distSubdir) {
        this.distSubdir = distSubdir
    }

    void setSkipStartAndStop(final boolean skipStartAndStop) {
        this.skipStartAndStop = skipStartAndStop
    }

}
