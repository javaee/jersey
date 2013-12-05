/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceFinder;

/**
 * A utility class that scans entries in jar files.
 *
 * @author Paul Sandoz
 */
public final class JarFileScanner implements ResourceFinder {

    private final static Logger LOGGER = Logger.getLogger(JarFileScanner.class.getName());

    private final JarInputStream jarInputStream;
    private final String parent;
    private final boolean recursive;

    /**
     * Create new JAR file scanner.
     *
     * @param inputStream JAR file input stream
     * @param parent JAR file entry prefix.
     * @param recursive if ({@code true} the packages will be scanned recursively together with any nested packages, if
     * {@code false} only the explicitly listed packages will be scanned.
     * @throws IOException if wrapping given input stream into {@link JarInputStream} failed.
     */
    public JarFileScanner(InputStream inputStream, String parent, boolean recursive) throws IOException {
        this.jarInputStream = new JarInputStream(inputStream);
        this.parent = parent;
        this.recursive = recursive;
    }

    private JarEntry next = null;

    @Override
    public boolean hasNext() {
        if (next == null) {
            try {
                do {
                    this.next = jarInputStream.getNextJarEntry();
                    if (next == null) {
                        break;
                    }
                    if (!next.isDirectory() && next.getName().startsWith(parent)) {
                        if (recursive) {
                            // accept any entries with the prefix
                            break;
                        }
                        // accept only entries directly in the folder.
                        String suffix = next.getName().substring(parent.length());
                        if (suffix.lastIndexOf(File.separatorChar) <= 0) {
                            break;
                        }
                    }
                } while (true);
            } catch (IOException e) {
                LOGGER.log(Level.CONFIG, "Unable to read the next jar entry.", e);
                return false;
            } catch (SecurityException e) {
                LOGGER.log(Level.CONFIG, "Unable to read the next jar entry.", e);
                return false;
            }
        }

        if (next == null) {
            try {
                jarInputStream.close();
            } catch (IOException e) {
                LOGGER.log(Level.FINE, "Unable to close jar file.", e);
            }

            return false;
        }

        return true;
    }

    @Override
    public String next() {
        if (next != null || hasNext()) {
            final String name = next.getName();
            next = null;
            return name;
        }

        throw new NoSuchElementException();
    }

    @Override
    public void remove() {
        throw new UnsupportedOperationException();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    @Override
    public InputStream open() {
        return new InputStream() {

            @Override
            public int read() throws IOException {
                return jarInputStream.read();
            }

            @Override
            public int read(byte[] bytes) throws IOException {
                return jarInputStream.read(bytes);
            }

            @Override
            public int read(byte[] bytes, int i, int i2) throws IOException {
                return jarInputStream.read(bytes, i, i2);
            }

            @Override
            public long skip(long l) throws IOException {
                return jarInputStream.skip(l);
            }

            @Override
            public int available() throws IOException {
                return jarInputStream.available();
            }

            @Override
            public void close() throws IOException {
                jarInputStream.closeEntry();
            }

            @Override
            public synchronized void mark(int i) {
                jarInputStream.mark(i);
            }

            @Override
            public synchronized void reset() throws IOException {
                jarInputStream.reset();
            }

            @Override
            public boolean markSupported() {
                return jarInputStream.markSupported();
            }
        };
    }
}

