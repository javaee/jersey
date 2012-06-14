/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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

import java.io.FilterInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.net.URL;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.server.ResourceFinder;

/**
 * A JBoss-based "vfsfile", "vfs" and "vfszip" scheme URI scanner.
 *
 * @author Paul Sandoz
 */
class VfsSchemeResourceFinderFactory implements UriSchemeResourceFinderFactory {

    public Set<String> getSchemes() {
        return new HashSet<String>(Arrays.asList("vfsfile", "vfszip", "vfs"));
    }

    VfsSchemeResourceFinderFactory() {
    }

    @Override
    public VfsSchemeScanner create(final URI uri) {
        ResourceFinderStack resourceFinderStack = new ResourceFinderStack();

        if(!uri.getScheme().equalsIgnoreCase("vfszip")) {
            resourceFinderStack.push(
                    new FileSchemeResourceFinderFactory().create(UriBuilder.fromUri(uri).scheme("file").build()));
        } else {


            final String su = uri.toString();
            final int webInfIndex = su.indexOf("/WEB-INF/classes");
            if (webInfIndex != -1) {
                final String war = su.substring(0, webInfIndex);
                final String path = su.substring(webInfIndex + 1);

                final int warParentIndex = war.lastIndexOf('/');
                final String warParent = su.substring(0, warParentIndex);

                // Check is there is a war within an ear
                // If so we need to load the ear then obtain the InputStream
                // of the entry to the war
                if (warParent.endsWith(".ear")) {
                    final String warName = su.substring(warParentIndex + 1, war.length());
                    try {

                        final JarFileScanner jarFileScanner = new JarFileScanner(new URL(warParent.replace("vfszip", "file")).openStream(), "");

                        while(jarFileScanner.hasNext()) {
                            if(jarFileScanner.next().equals(warName)) {

                                resourceFinderStack.push(new JarFileScanner(new FilterInputStream(jarFileScanner.open()) {
                                    // This is required so that the underlying ear
                                    // is not closed

                                    @Override
                                    public void close() throws IOException {
                                    }
                                }, ""));
                            }
                        }

                    } catch (IOException e) {
                        throw new ResourceFinderException("IO error when scanning war " + uri, e);
                    }
                } else {
                    try {
                        resourceFinderStack.push(new JarFileScanner(new URL(war.replace("vfszip", "file")).openStream(), path));
                    } catch (IOException e) {
                        throw new ResourceFinderException("IO error when scanning war " + uri, e);
                    }
                }
            } else {
                try {
                    resourceFinderStack.push(new JarFileScanner(new URL(su).openStream(), ""));
                } catch (IOException e) {
                    throw new ResourceFinderException("IO error when scanning jar " + uri, e);
                }
            }
        }

        return new VfsSchemeScanner(resourceFinderStack);
    }

    private class VfsSchemeScanner implements ResourceFinder {

        private final ResourceFinderStack resourceFinderStack;

        private VfsSchemeScanner(final ResourceFinderStack resourceFinderStack) {
            this.resourceFinderStack = resourceFinderStack;
        }

        @Override
        public boolean hasNext() {
            return resourceFinderStack.hasNext();
        }

        @Override
        public String next() {
            return resourceFinderStack.next();
        }

        @Override
        public void remove() {
            resourceFinderStack.next();
        }

        @Override
        public InputStream open() {
            return resourceFinderStack.open();
        }

        @Override
        public void reset() {
            resourceFinderStack.reset();
        }
    }
}
