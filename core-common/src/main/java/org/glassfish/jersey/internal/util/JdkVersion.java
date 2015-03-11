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
package org.glassfish.jersey.internal.util;

/**
 * JDK Version related utilities. Ported from Grizzly project.
 *
 * @author Ryan Lubke (ryan.lubke at oracle.com)
 * @author Alexey Stashok (oleksiy.stashok at oracle.com)
 * @since 2.3
 */
public class JdkVersion implements Comparable<JdkVersion> {

    private static final boolean IS_UNSAFE_SUPPORTED;

    static {
        boolean unsafeSupported;

        try {
            // Look for sun.misc.Unsafe.
            unsafeSupported = Class.forName("sun.misc.Unsafe") != null;

            // Check environment availability.
            // Google App Engine (see https://developers.google.com/appengine/docs/java/#Java_The_environment).
            unsafeSupported &= System.getProperty("com.google.appengine.runtime.environment") == null;
        } catch (final Throwable t) {
            // Make Unsafe not supported if either:
            // - sun.misc.Unsafe not found.
            // - we're not granted to read the property (* is not enough).
            unsafeSupported = false;
        }

        IS_UNSAFE_SUPPORTED = unsafeSupported;
    }

    private static final JdkVersion UNKNOWN_VERSION = new JdkVersion(-1, -1, -1, -1);
    private static final JdkVersion JDK_VERSION = parseVersion(System.getProperty("java.version"));

    private final int major;
    private final int minor;
    private final int maintenance;
    private final int update;

    // ------------------------------------------------------------ Constructors

    private JdkVersion(final int major, final int minor, final int maintenance, final int update) {
        this.major = major;
        this.minor = minor;
        this.maintenance = maintenance;
        this.update = update;
    }

    // ---------------------------------------------------------- Public Methods

    public static JdkVersion parseVersion(String versionString) {
        try {
            final int dashIdx = versionString.indexOf('-');
            if (dashIdx != -1) {
                versionString = versionString.substring(0, dashIdx);
            }
            final String[] parts = versionString.split("\\.|_");
            if (parts.length == 3) {
                return new JdkVersion(Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        0);
            } else {
                return new JdkVersion(Integer.parseInt(parts[0]),
                        Integer.parseInt(parts[1]),
                        Integer.parseInt(parts[2]),
                        Integer.parseInt(parts[3]));
            }
        } catch (final Exception e) {
            return UNKNOWN_VERSION;
        }
    }

    public static JdkVersion getJdkVersion() {
        return JDK_VERSION;
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getMajor() {
        return major;
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getMinor() {
        return minor;
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getMaintenance() {
        return maintenance;
    }

    @SuppressWarnings("UnusedDeclaration")
    public int getUpdate() {
        return update;
    }

    /**
     * Returns <tt>true</tt> if {@code sun.misc.Unsafe} is present in the
     * current JDK version, or <tt>false</tt> otherwise.
     *
     * @since 2.3.6
     */
    public boolean isUnsafeSupported() {
        return IS_UNSAFE_SUPPORTED;
    }

    @Override
    public String toString() {
        return "JdkVersion" + "{major=" + major + ", minor=" + minor + ", maintenance=" + maintenance
                + ", update=" + update + '}';
    }

    // ------------------------------------------------- Methods from Comparable

    public int compareTo(final String versionString) {
        return compareTo(JdkVersion.parseVersion(versionString));
    }

    @Override
    public int compareTo(final JdkVersion otherVersion) {
        if (major < otherVersion.major) {
            return -1;
        }
        if (major > otherVersion.major) {
            return 1;
        }
        if (minor < otherVersion.minor) {
            return -1;
        }
        if (minor > otherVersion.minor) {
            return 1;
        }
        if (maintenance < otherVersion.maintenance) {
            return -1;
        }
        if (maintenance > otherVersion.maintenance) {
            return 1;
        }
        if (update < otherVersion.update) {
            return -1;
        }
        if (update > otherVersion.update) {
            return 1;
        }
        return 0;
    }
}
