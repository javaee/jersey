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

import java.io.InputStream;
import java.util.Deque;
import java.util.LinkedList;
import java.util.NoSuchElementException;
import java.util.Stack;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceFinder;
import org.glassfish.jersey.server.internal.AbstractResourceFinderAdapter;
import org.glassfish.jersey.server.internal.LocalizationMessages;

/**
 * {@link Stack} of {@link ResourceFinder} instances.
 * <p/>
 * Used to combine various finders into one instance.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public final class CompositeResourceFinder extends AbstractResourceFinderAdapter {

    private static final Logger LOGGER = Logger.getLogger(CompositeResourceFinder.class.getName());

    private final Deque<ResourceFinder> stack = new LinkedList<>();
    private ResourceFinder current = null;

    @Override
    public boolean hasNext() {
        if (current == null) {
            if (!stack.isEmpty()) {
                current = stack.pop();
            } else {
                return false;
            }
        }

        if (current.hasNext()) {
            return true;
        } else {
            if (!stack.isEmpty()) {
                current = stack.pop();
                return hasNext();
            } else {
                return false;
            }
        }
    }

    @Override
    public String next() {
        if (hasNext()) {
            return current.next();
        }

        throw new NoSuchElementException();
    }

    @Override
    public InputStream open() {
        return current.open();
    }

    @Override
    public void close() {
        if (current != null) {
            // Insert the currently processed resource finder at the top of the stack.
            stack.addFirst(current);
            current = null;
        }
        for (final ResourceFinder finder : stack) {
            try {
                finder.close();
            } catch (final RuntimeException e) {
                LOGGER.log(Level.CONFIG, LocalizationMessages.ERROR_CLOSING_FINDER(finder.getClass()), e);
            }
        }
        stack.clear();
    }

    @Override
    public void reset() {
        throw new UnsupportedOperationException();
    }

    public void push(final ResourceFinder iterator) {
        stack.push(iterator);
    }
}
