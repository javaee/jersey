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
package org.glassfish.jersey.server.internal.scanning;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.server.internal.AbstractResourceFinderAdapter;
import org.glassfish.jersey.uri.UriComponent;

/**
 * A "jar", "zip" and "wsjar" scheme URI scanner that recursively jar files.
 * Jar entries are reported to a {@link ResourceProcessor}.
 *
 * @author Paul Sandoz
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
final class JarZipSchemeResourceFinderFactory implements UriSchemeResourceFinderFactory {

    private static final Set<String> SCHEMES = Collections.unmodifiableSet(new HashSet<>(Arrays.asList("jar", "zip", "wsjar")));

    @Override
    public Set<String> getSchemes() {
        return SCHEMES;
    }

    /**
     * Create new "jar", "zip" and "wsjar" scheme URI scanner factory.
     */
    JarZipSchemeResourceFinderFactory() {
    }

    @Override
    public JarZipSchemeScanner create(final URI uri, final boolean recursive) {
        final String ssp = uri.getRawSchemeSpecificPart();
        final String jarUrlString = ssp.substring(0, ssp.lastIndexOf('!'));
        final String parent = ssp.substring(ssp.lastIndexOf('!') + 2);

        try {
            return new JarZipSchemeScanner(getInputStream(jarUrlString), parent, recursive);
        } catch (final IOException e) {
            throw new ResourceFinderException(e);
        }
    }

    private class JarZipSchemeScanner extends AbstractResourceFinderAdapter {

        private final InputStream inputStream;
        private final JarFileScanner jarFileScanner;

        private JarZipSchemeScanner(final InputStream inputStream, final String parent, final boolean recursive)
                throws IOException {
            this.inputStream = inputStream;
            this.jarFileScanner = new JarFileScanner(inputStream, parent, recursive);
        }

        @Override
        public boolean hasNext() {
            final boolean hasNext = jarFileScanner.hasNext();
            if (!hasNext) {
                try {
                    inputStream.close();
                } catch (final IOException e) {
                    Logger.getLogger(JarZipSchemeScanner.class.getName()).log(Level.FINE, "Unable to close jar file.", e);
                }
                return false;
            }

            return true;
        }

        @Override
        public String next() {
            return jarFileScanner.next();
        }

        @Override
        public InputStream open() {
            return jarFileScanner.open();
        }

        @Override
        public void close() {
            jarFileScanner.close();
        }

        @Override
        public void reset() {
            jarFileScanner.reset();
        }
    }

    /**
     * Obtain a {@link InputStream} of the jar file.
     * <p>
     * For most platforms the format for the zip or jar follows the form of
     * the <a href="http://docs.sun.com/source/819-0913/author/jar.html#jarprotocol"jar protcol.</a></p>
     * <ul>
     * <li><code>jar:file:///tmp/fishfingers.zip!/example.txt</code></li>
     * <li><code>zip:http://www.example.com/fishfingers.zip!/example.txt</code></li>
     * </ul>
     * <p>
     * On versions of the WebLogic application server a proprietary format is
     * supported of the following form, which assumes a zip file located on
     * the local file system:
     * </p>
     * <ul>
     * <li><code>zip:/tmp/fishfingers.zip!/example.txt</code></li>
     * <li><code>zip:d:/tempfishfingers.zip!/example.txt</code></li>
     * </ul>
     * <p/>
     * This method will first attempt to create a {@link InputStream} as follows:
     * <pre>
     *   new URL(jarUrlString).openStream();
     * </pre>
     * if that fails with a {@link java.net.MalformedURLException} then the method will
     * attempt to create a {@link InputStream} instance as follows:
     * <pre>
     *  return new new FileInputStream(
     *      UriComponent.decode(jarUrlString, UriComponent.Type.PATH)));
     * </pre>
     *
     * @param jarUrlString the raw scheme specific part of a URI minus the jar
     *                     entry
     * @return a {@link InputStream}.
     * @throws IOException if there is an error opening the stream.
     */
    private InputStream getInputStream(final String jarUrlString) throws IOException {
        try {
            return new URL(jarUrlString).openStream();
        } catch (final MalformedURLException e) {
            return new FileInputStream(
                    UriComponent.decode(jarUrlString, UriComponent.Type.PATH));
        }
    }
}
