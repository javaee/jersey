/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.ArrayList;
import java.util.List;

import org.ops4j.pax.exam.Option;
import static org.ops4j.pax.exam.CoreOptions.systemProperty;

/**
 * Helper class to be used by individual tests.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class Helper {

    /**
     * Returns an integer value of given system property, or a default value
     * as defined by the other method parameter, if the system property can
     * not be used.
     *
     * @param varName name of the system variable.
     * @param defaultValue the default value to return if the system variable is missing or can not be parsed as an integer.
     * @return an integer value taken either from the system property or the default value as defined by the defaultValue parameter.
     */
    public static int getEnvVariable(final String varName, int defaultValue) {
        if (null == varName) {
            return defaultValue;
        }
        String varValue = System.getProperty(varName);
        if (null != varValue) {
            try {
                return Integer.parseInt(varValue);
            } catch (NumberFormatException e) {
                // will return default value bellow
            }
        }
        return defaultValue;
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
        final String localRepository = System.getProperty("localRepository");

        if (localRepository != null) {
            options = new ArrayList<Option>(options);
            options.add(systemProperty("org.ops4j.pax.url.mvn.localRepository").value(localRepository));
        }

        return options;
    }
}
