/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.process.internal;

import java.util.Iterator;

import javax.ws.rs.core.Request;

import org.glassfish.jersey.internal.util.collection.Pair;

/**
 * Hierarchical request acceptor.
 * <p/>
 * A continuation of a hierarchical acceptor is represented by an ordered collection
 * of next level of hierarchical acceptors resulting in a hierarchical depth-first
 * request transformation processing.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface TreeAcceptor extends Stage<Request, Iterator<TreeAcceptor>> {

    /**
     * A {@link TreeAcceptor} builder.
     */
    public static interface Builder {

        /**
         * Add new child node into the {@link TreeAcceptor hierarchical request acceptor}
         * being built.
         *
         * @param child new child node to be added.
         * @return updated builder instance.
         */
        public Builder child(TreeAcceptor child);

        /**
         * Build a {@link TreeAcceptor hierarchical acceptor} for the transformation of
         * a given data type.
         *
         * @return hierarchical stage.
         */
        public TreeAcceptor build();
    }

    /**
     * {@inheritDoc}
     * <p/>
     * The returned continuation is an {@link Iterator iterator} over the next level
     * of the tree acceptors that should be invoked. A non-empty iterator
     * typically indicates that the processing is expected to continue further, while
     * an empty iterator returned as a continuation indicates that the unidirectional
     * hierarchical data transformation previously reached a leaf node and the depth-first
     * processing algorithm needs to determine whether the processing is finished or
     * whether it should back-up, move to a next branch and continue.
     */
    @Override
    Pair<Request, Iterator<TreeAcceptor>> apply(Request data);
}
