/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal;

import java.io.InputStream;
import java.util.Properties;

/**
 * Utility class for reading build.properties file.
 *
 * @author Paul Sandoz
 */
public final class Version {

    private static String buildId;
    private static String version = null;

    static {
        _initiateProperties();
    }

    private Version() {
        throw new AssertionError("Instantiation not allowed.");
    }

    private static void _initiateProperties() {
        final InputStream in = getIntputStream();
        if (in != null) {
            try {
                final Properties p = new Properties();
                p.load(in);
                final String timestamp = p.getProperty("Build-Timestamp");
                version = p.getProperty("Build-Version");

                buildId = String.format("Jersey: %s %s", version, timestamp);
            } catch (final Exception e) {
                buildId = "Jersey";
            } finally {
                close(in);
            }
        }
    }

    private static void close(final InputStream in) {
        try {
            in.close();
        } catch (final Exception ex) {
            // Ignore
        }
    }

    private static InputStream getIntputStream() {
        try {
            return Version.class.getResourceAsStream("build.properties");
        } catch (final Exception ex) {
            return null;
        }
    }

    /**
     * Get build id.
     *
     * @return build id string. Contains version and build timestamp.
     */
    public static String getBuildId() {
        return buildId;
    }

    /**
     * Get Jersey version.
     *
     * @return Jersey version.
     */
    public static String getVersion() {
        return version;
    }
}
