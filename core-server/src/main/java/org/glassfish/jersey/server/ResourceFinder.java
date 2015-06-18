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
package org.glassfish.jersey.server;

import java.io.InputStream;
import java.util.Iterator;

/**
 * An interface used for finding and opening (loading) new resources.
 * <p/>
 * {@link ResourceConfig} will use all registered finders to obtain classes
 * to be used as resource classes and/or providers. Method {@link #open()} doesn't
 * need to be called on all returned resource names, {@link ResourceConfig} can ignore
 * some of them.
 * <p/>
 * Currently, all resource names ending with ".class" will be accepted and processed (opened).
 * <p/>
 * Extends {@link AutoCloseable} since version 2.19. The {@link #close()} method is used to release
 * allocated/opened resources (such as streams). When a resource finder is closed no other method should be
 * invoked on it.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public interface ResourceFinder extends Iterator<String>, AutoCloseable {

    /**
     * Open current resource.
     *
     * @return input stream from which current resource can be loaded.
     */
    public InputStream open();

    /**
     * {@inheritDoc}
     * <p/>
     * Release allocated/opened resources (such as streams). When the resource finder is closed
     * no other method should be invoked on it.
     *
     * @since 2.19
     */
    public void close();

    /**
     * Reset the {@link ResourceFinder} instance.
     * <p/>
     * Upon calling this method the implementing class MUST reset its internal state to the initial state.
     */
    public void reset();

    /**
     * {@inheritDoc}
     * <p/>
     * This operation is not supported by {@link ResourceFinder} & throws {@link UnsupportedOperationException}
     * when invoked.
     */
    @Override
    public void remove();
}
