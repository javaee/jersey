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

import java.io.IOException;
import java.io.InputStream;
import java.net.URI;
import java.util.Collections;
import java.util.NoSuchElementException;
import java.util.Set;

import org.glassfish.jersey.server.internal.AbstractResourceFinderAdapter;

/**
 * Preparations for OSGi support.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
final class BundleSchemeResourceFinderFactory implements UriSchemeResourceFinderFactory {

    private static final Set<String> SCHEMES = Collections.singleton("bundle");

    @Override
    public Set<String> getSchemes() {
        return SCHEMES;
    }

    /**
     * Create new bundle scheme resource finder factory.
     */
    BundleSchemeResourceFinderFactory() {
    }

    @Override
    public BundleSchemeScanner create(final URI uri, final boolean recursive) {
        return new BundleSchemeScanner(uri);
    }

    private class BundleSchemeScanner extends AbstractResourceFinderAdapter {

        private BundleSchemeScanner(final URI uri) {
            this.uri = uri;
        }

        private final URI uri;

        /**
         * Marks this iterator as iterated after execution of {@link #open()} method.
         * Together with {@link #iterated}, this field determines a returned value of {@link #hasNext()}.
         */
        private boolean accessed = false;

        /**
         * Marks this iterator as iterated after execution of {@link #next()} method.
         * Together with {@link #accessed}, this field determines a returned value of {@link #hasNext()}.
         */
        private boolean iterated = false;

        @Override
        public boolean hasNext() {
            return !accessed && !iterated;
        }

        @Override
        public String next() {
            if (hasNext()) {
                iterated = true;
                return uri.getPath();
            }

            throw new NoSuchElementException();
        }

        @Override
        public InputStream open() {
            if (!accessed) {
                try {
                    accessed = true;
                    return uri.toURL().openStream();
                } catch (final IOException e) {
                    throw new ResourceFinderException(e);
                }
            }

            return null;
        }

        @Override
        public void reset() {
            throw new UnsupportedOperationException();
        }
    }

}
