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
import java.lang.reflect.Method;
import java.net.URI;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.NoSuchElementException;
import java.util.Set;

import org.glassfish.jersey.server.ResourceFinder;
import org.glassfish.jersey.server.internal.AbstractResourceFinderAdapter;

/**
 * A JBoss-based "vfsfile", "vfs" and "vfszip" scheme URI scanner.
 * <p/>
 * This approach uses reflection to allow for zero-deps and support
 * for both the v2 (EAP5, AS5) and v3 VFS APIs (AS6, AS7, EAP6 & WildFly)
 * which are not binary compatible.
 *
 * @author Jason T. Greene
 * @author Paul Sandoz
 */
final class VfsSchemeResourceFinderFactory implements UriSchemeResourceFinderFactory {

    private static final Set<String> SCHEMES = Collections
            .unmodifiableSet(new HashSet<>(Arrays.asList("vfsfile", "vfszip", "vfs")));

    public Set<String> getSchemes() {
        return SCHEMES;
    }

    VfsSchemeResourceFinderFactory() {
    }

    @Override
    public ResourceFinder create(final URI uri, final boolean recursive) {
        return new VfsResourceFinder(uri, recursive);
    }

    private static class VfsResourceFinder extends AbstractResourceFinderAdapter {

        private Object current;
        private Object next;
        private final Method openStream;
        private final Method getName;
        private final Method isLeaf;
        private final Iterator<?> iterator;

        public VfsResourceFinder(final URI uri, final boolean recursive) {
            final Object directory = bindDirectory(uri);
            this.openStream = bindMethod(directory, "openStream");
            this.getName = bindMethod(directory, "getName");
            this.isLeaf = bindMethod(directory, "isLeaf");
            this.iterator = getChildren(directory, recursive);
        }

        private Iterator<?> getChildren(final Object directory, final boolean recursive) {
            final Method getChildren = bindMethod(directory, recursive ? "getChildrenRecursively" : "getChildren");

            final List<?> list = invoke(directory, getChildren, List.class);
            if (list == null) {
                throw new ResourceFinderException("VFS object returned null when accessing children");
            }

            return list.iterator();
        }

        private Method bindMethod(final Object object, final String name) {
            if (System.getSecurityManager() != null) {
                AccessController.doPrivileged(new PrivilegedAction<Method>() {
                    public Method run() {
                        return bindMethod0(object, name);
                    }
                });
            }

            return bindMethod0(object, name);
        }

        private <T> T invoke(final Object instance, final Method method, final Class<T> type) {
            try {
                return type.cast(method.invoke(instance));
            } catch (final Exception e) {
                throw new ResourceFinderException("VFS object could not be invoked upon");
            }
        }

        private Method bindMethod0(final Object object, final String name) {
            final Class<?> clazz = object.getClass();

            try {
                return clazz.getMethod(name);
            } catch (final NoSuchMethodException e) {
                throw new ResourceFinderException("VFS object did not have a valid signature");
            }
        }

        private Object bindDirectory(final URI uri) {
            Object directory = null;
            try {
                directory = uri.toURL().getContent();
            } catch (final IOException e) {
                // Eat
            }

            if (directory == null || !directory.getClass().getSimpleName().equals("VirtualFile")) {
                throw new ResourceFinderException("VFS URL did not map to a valid VFS object");
            }

            return directory;
        }

        @Override
        public InputStream open() {
            final Object current = this.current;
            if (current == null) {
                throw new IllegalStateException("next() must be called before open()");
            }

            return invoke(current, openStream, InputStream.class);
        }

        @Override
        public void reset() {
            throw new UnsupportedOperationException();
        }

        public boolean advance() {
            while (iterator.hasNext()) {
                final Object next = iterator.next();
                if (invoke(next, isLeaf, Boolean.class)) {
                    this.next = next;
                    return true;
                }
            }

            return false;
        }

        @Override
        public boolean hasNext() {
            return next != null || advance();
        }

        @Override
        public String next() {
            if (!hasNext()) {
                throw new NoSuchElementException();
            }

            current = next;
            next = null;
            return invoke(current, getName, String.class);
        }
    }
}
