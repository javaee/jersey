/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal;

import java.io.IOException;
import java.lang.reflect.Method;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.glassfish.jersey.internal.ServiceFinder;
import org.glassfish.jersey.server.JarUtils;

import org.junit.Test;
import static org.glassfish.jersey.server.JarUtils.createJarFile;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 * @author Michal Gajdos
 */
public class ServiceFinderTest {

    @Test
    public void testJarTopLevel() throws Exception {
        final Map<String, String> map = new HashMap<>();
        map.put("org/glassfish/jersey/server/config/jaxrs-components", "META-INF/services/jaxrs-components");
        map.put("org/glassfish/jersey/server/config/toplevel/PublicRootResourceClass.class",
                "org/glassfish/jersey/server/config/toplevel/PublicRootResourceClass.class");

        final String path = ServiceFinderTest.class.getResource("").getPath();
        final ClassLoader classLoader = createClassLoader(path.substring(0, path.indexOf("org")), map);

        final ServiceFinder<?> finder = createServiceFinder(classLoader, "jaxrs-components");

        final Set<Class<?>> s = new HashSet<>();
        Collections.addAll(s, finder.toClassArray());

        assertTrue(s.contains(classLoader.loadClass("org.glassfish.jersey.server.config.toplevel.PublicRootResourceClass")));
        assertEquals(1, s.size());
    }

    private ServiceFinder<?> createServiceFinder(final ClassLoader cl, final String serviceName) throws IOException {
        final ClassLoader ocl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(cl);
        try {
            final Class<?> prc = cl.loadClass("org.glassfish.jersey.internal.ServiceFinder");
            final Method m = prc.getMethod("find", String.class);

            return (ServiceFinder<?>) m.invoke(null, serviceName);
            // return new PackagesResourceConfig(packages);
        } catch (final Exception e) {
            throw new RuntimeException(e);
        } finally {
            Thread.currentThread().setContextClassLoader(ocl);
        }
    }

    private ClassLoader createClassLoader(final String base, final Map<String, String> entries) throws IOException {
        return createClassLoader(JarUtils.Suffix.jar, base, entries);
    }

    private ClassLoader createClassLoader(final JarUtils.Suffix s, final String base, final Map<String,
            String> entries) throws IOException {

        final URL[] us = new URL[1];
        us[0] = createJarFile(s, base, entries).toURI().toURL();
        return new PackageClassLoader(us);
    }

    private static class PackageClassLoader extends URLClassLoader {

        PackageClassLoader(final URL[] urls) {
            super(urls, null);
        }

        public Class<?> findClass(final String name) throws ClassNotFoundException {
            try {
                return super.findClass(name);
            } catch (final ClassNotFoundException e) {
                return getSystemClassLoader().loadClass(name);
            }
        }
    }
}
