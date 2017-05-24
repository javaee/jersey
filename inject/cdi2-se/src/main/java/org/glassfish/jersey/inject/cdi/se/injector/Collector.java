/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.inject.cdi.se.injector;

import java.util.LinkedHashSet;
import java.util.LinkedList;

/**
 * This class collects errors, and can then also produce a MultiException from those errors if necessary.
 *
 * @author John Wells (john.wells at oracle.com)
 */
public class Collector {

    private LinkedHashSet<Throwable> throwables;

    /**
     * Merges {@link MultiException} with all {@code throwables} registered in it.
     *
     * @param me {@code MultiException} to merge.
     */
    public void addMultiException(MultiException me) {
        if (me == null) {
            return;
        }
        if (throwables == null) {
            throwables = new LinkedHashSet<>();
        }

        throwables.addAll(me.getErrors());
    }

    /**
     * Adds a throwable to the list of throwables in this collector.
     *
     * @param th The throwable to add to the list.
     */
    public void addThrowable(Throwable th) {
        if (th == null) {
            return;
        }
        if (throwables == null) {
            throwables = new LinkedHashSet<>();
        }

        if (th instanceof MultiException) {
            throwables.addAll(((MultiException) th).getErrors());
        } else {
            throwables.add(th);
        }
    }

    /**
     * This method will throw if the list of throwables associated with this collector is not empty.
     *
     * @throws MultiException An exception with all the throwables found in this collector.
     */
    public void throwIfErrors() throws MultiException {
        if (throwables == null || throwables.isEmpty()) {
            return;
        }

        throw new MultiException(new LinkedList<>(throwables));
    }

    /**
     * Returns true if this collector has errors.
     *
     * @return true if the collector has errors.
     */
    public boolean hasErrors() {
        return ((throwables != null) && (!throwables.isEmpty()));
    }
}
