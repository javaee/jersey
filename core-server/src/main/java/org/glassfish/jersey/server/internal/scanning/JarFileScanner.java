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

import org.glassfish.jersey.server.ResourceFinder;

import java.io.IOException;
import java.io.InputStream;
import java.util.NoSuchElementException;
import java.util.jar.JarEntry;
import java.util.jar.JarInputStream;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * A utility class that scans entries in jar files.
 *
 * @author Paul Sandoz
 */
public final class JarFileScanner implements ResourceFinder {

    private final JarInputStream jarInputStream;
    private final String parent;

    public JarFileScanner(InputStream inputStream, String parent) throws IOException {
        this.jarInputStream = new JarInputStream(inputStream);
        this.parent = parent;
    }

    private JarEntry next = null;

    @Override
    public boolean hasNext() {
        if(next == null) {
            try {
                do {
                    this.next = jarInputStream.getNextJarEntry();
                } while(next != null && (next.isDirectory() || !next.getName().startsWith(parent)));
            } catch (IOException e) {
                Logger.getLogger(JarFileScanner.class.getName()).log(Level.CONFIG, "Unable to read the next jar entry.", e);
                return false;
            } catch (SecurityException e) {
                Logger.getLogger(JarFileScanner.class.getName()).log(Level.CONFIG, "Unable to read the next jar entry.", e);
                return false;
            }
        }

        if(next == null) {
            try {
                jarInputStream.close();
            } catch (IOException e) {
                Logger.getLogger(JarFileScanner.class.getName()).log(Level.FINE, "Unable to close jar file.", e);
            }

            return false;
        }

        return true;
    }

    @Override
    public String next() {
        if(next != null || hasNext()) {
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
    public InputStream open() {
        return jarInputStream;
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }
}

