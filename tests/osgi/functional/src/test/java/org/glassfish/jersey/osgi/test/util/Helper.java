/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.osgi.test.util;

import java.security.AccessController;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.test.TestProperties;

import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.CoreOptions.junitBundles;
import static org.ops4j.pax.exam.CoreOptions.mavenBundle;
import static org.ops4j.pax.exam.CoreOptions.options;
import static org.ops4j.pax.exam.CoreOptions.systemPackage;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

/**
 * Helper class to be used by individual tests.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Michal Gajdos
 */
public class Helper {

    /**
     * Jersey HTTP port.
     */
    private static final int port = getEnvVariable(TestProperties.CONTAINER_PORT, 8080);

    /**
     * Returns an integer value of given system property, or a default value
     * as defined by the other method parameter, if the system property can
     * not be used.
     *
     * @param varName      name of the system variable.
     * @param defaultValue the default value to return if the system variable is missing or can not be parsed as an integer.
     * @return an integer value taken either from the system property or the default value as defined by the defaultValue parameter.
     */
    public static int getEnvVariable(final String varName, int defaultValue) {
        if (null == varName) {
            return defaultValue;
        }
        String varValue = AccessController.doPrivileged(PropertiesHelper.getSystemProperty(varName));
        if (null != varValue) {
            try {
                return Integer.parseInt(varValue);
            } catch (NumberFormatException e) {
                // will return default value below
            }
        }
        return defaultValue;
    }

    /**
     * Returns a value of {@value TestProperties#CONTAINER_PORT} property which should be used as port number for test container.
     *
     * @return port number.
     */
    public static int getPort() {
        return port;
    }

    /**
     * Adds a system property for Maven local repository location to the PaxExam OSGi runtime if a "localRepository" property
     * is present in the map of the system properties.
     *
     * @param options list of options to add the local repository property to.
     * @return list of options enhanced by the local repository property if this property is set or the given list if the
     *         previous condition is not met.
     */
    public static List<Option> addPaxExamMavenLocalRepositoryProperty(List<Option> options) {
        final String localRepository = AccessController.doPrivileged(PropertiesHelper.getSystemProperty("localRepository"));

        if (localRepository != null) {
            options.addAll(expandedList(systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepository)));
        }

        return options;
    }

    /**
     * Convert list of OSGi options to an array.
     *
     * @param options list of OSGi options.
     * @return array of OSGi options.
     */
    public static Option[] asArray(final List<Option> options) {
        return options.toArray(new Option[options.size()]);
    }

    /**
     * Create new list of common OSGi integration test options.
     *
     * @return list of common OSGi integration test options.
     */
    public static List<Option> getCommonOsgiOptions() {
        return getCommonOsgiOptions(true);
    }

    /**
     * Create new list of common OSGi integration test options.
     *
     * @param includeJerseyJaxRsLibs indicates whether JaxRs and Jersey bundles should be added into the resulting list of
     * options.
     * @return list of common OSGi integration test options.
     */
    public static List<Option> getCommonOsgiOptions(final boolean includeJerseyJaxRsLibs) {
        final List<Option> options = new LinkedList<Option>(expandedList(
                // systemProperty("org.ops4j.pax.logging.DefaultServiceLog.level").value("FINEST"),
                systemProperty("org.osgi.service.http.port").value(String.valueOf(port)),
                systemProperty(TestProperties.CONTAINER_PORT).value(String.valueOf(port)),
                systemProperty("org.osgi.framework.system.packages.extra").value("javax.annotation"),

                // javax.annotation has to go first!
                mavenBundle().groupId("javax.annotation").artifactId("javax.annotation-api").versionAsInProject(),

                junitBundles(),

                // HK2
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-api").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("osgi-resource-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-locator").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2").artifactId("hk2-utils").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("javax.inject").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.hk2.external").artifactId("aopalliance-repackaged").versionAsInProject(),
                mavenBundle().groupId("org.javassist").artifactId("javassist").versionAsInProject(),

                // Grizzly
                systemPackage("sun.misc"),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-framework").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-http").versionAsInProject(),
                mavenBundle().groupId("org.glassfish.grizzly").artifactId("grizzly-http-server").versionAsInProject(),

                // javax.validation
                mavenBundle().groupId("javax.validation").artifactId("validation-api").versionAsInProject(),

                // Jersey Grizzly
                mavenBundle().groupId("org.glassfish.jersey.containers").artifactId("jersey-container-grizzly2-http")
                        .versionAsInProject()
        ));

        if (includeJerseyJaxRsLibs) {
            options.addAll(expandedList(
                    // JAX-RS API
                    mavenBundle().groupId("javax.ws.rs").artifactId("javax.ws.rs-api").versionAsInProject(),

                    // Jersey bundles
                    mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-common").versionAsInProject(),
                    mavenBundle().groupId("org.glassfish.jersey.media").artifactId("jersey-media-jaxb").versionAsInProject(),
                    mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-server").versionAsInProject(),
                    mavenBundle().groupId("org.glassfish.jersey.core").artifactId("jersey-client").versionAsInProject(),

                    // Jersey Injection provider
                    mavenBundle().groupId("org.glassfish.jersey.inject").artifactId("jersey-hk2").versionAsInProject()
            ));
        }

        return addPaxExamMavenLocalRepositoryProperty(options);
    }

    /**
     * Create expanded options list from the supplied options.
     *
     * @param options options to be expanded into the option list.
     * @return expanded options list.
     */
    public static List<Option> expandedList(Option... options) {
        return Arrays.asList(options(options));
    }
}
