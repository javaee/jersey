/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.servlet;

import java.io.IOException;
import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Set;

import javax.servlet.ServletContext;

import org.glassfish.jersey.server.internal.AbstractResourceFinderAdapter;
import org.glassfish.jersey.server.internal.scanning.JarFileScanner;
import org.glassfish.jersey.server.internal.scanning.ResourceFinderException;
import org.glassfish.jersey.server.internal.scanning.CompositeResourceFinder;

/**
 * A scanner that recursively scans resources within a Web application.
 *
 * @author Paul Sandoz
 */
final class WebAppResourcesScanner extends AbstractResourceFinderAdapter {

    private static final String[] paths = new String[] {"/WEB-INF/lib/", "/WEB-INF/classes/"};

    private final ServletContext sc;
    private CompositeResourceFinder compositeResourceFinder = new CompositeResourceFinder();

    /**
     * Scan from a set of web resource paths.
     * <p/>
     *
     * @param sc {@link ServletContext}.
     */
    WebAppResourcesScanner(final ServletContext sc) {
        this.sc = sc;

        processPaths(paths);
    }

    private void processPaths(final String... paths) {
        for (final String path : paths) {

            final Set<String> resourcePaths = sc.getResourcePaths(path);
            if (resourcePaths == null) {
                break;
            }

            compositeResourceFinder.push(new AbstractResourceFinderAdapter() {

                private final Deque<String> resourcePathsStack = new LinkedList<String>() {

                    private static final long serialVersionUID = 3109256773218160485L;

                    {
                        for (final String resourcePath : resourcePaths) {
                            push(resourcePath);
                        }
                    }
                };

                private String current;
                private String next;

                @Override
                public boolean hasNext() {
                    while (next == null && !resourcePathsStack.isEmpty()) {
                        next = resourcePathsStack.pop();

                        if (next.endsWith("/")) {
                            processPaths(next);
                            next = null;
                        } else if (next.endsWith(".jar")) {
                            try {
                                compositeResourceFinder.push(new JarFileScanner(sc.getResourceAsStream(next), "", true));
                            } catch (final IOException ioe) {
                                throw new ResourceFinderException(ioe);
                            }
                            next = null;
                        }
                    }

                    return next != null;
                }

                @Override
                public String next() {
                    if (next != null || hasNext()) {
                        current = next;
                        next = null;
                        return current;
                    }

                    throw new NoSuchElementException();
                }

                @Override
                public InputStream open() {
                    return sc.getResourceAsStream(current);
                }

                @Override
                public void reset() {
                    throw new UnsupportedOperationException();
                }
            });

        }
    }

    @Override
    public boolean hasNext() {
        return compositeResourceFinder.hasNext();
    }

    @Override
    public String next() {
        return compositeResourceFinder.next();
    }

    @Override
    public InputStream open() {
        return compositeResourceFinder.open();
    }

    @Override
    public void close() {
        compositeResourceFinder.close();
    }

    @Override
    public void reset() {
        compositeResourceFinder = new CompositeResourceFinder();
        processPaths(paths);
    }
}
