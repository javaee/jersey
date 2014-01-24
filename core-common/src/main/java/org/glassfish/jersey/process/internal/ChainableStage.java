/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2014 Oracle and/or its affiliates. All rights reserved.
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

/**
 * Linear acceptor that can be composed into a chain.
 *
 * The acceptor exposes a method for setting a value of the
 * {@link #setDefaultNext(Stage) next acceptor} in the chain that
 * should be returned from the chain by default.
 * <p>
 * The typical use case for implementing the acceptor is a logic that usually
 * needs to perform some logic, but unlike an {@link Stage.Builder#to(jersey.repackaged.com.google.common.base.Function)
 * acceptor created from a function} it also needs to be able to decide to override
 * the default next acceptor and return a different acceptor, effectively branching
 * away from the original linear acceptor chain. This technique can be e.g. used
 * to break the accepting chain by returning a custom {@link Inflecting inflecting}
 * acceptor, etc.
 * </p>
 *
 * @param <DATA> processed data type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface ChainableStage<DATA> extends Stage<DATA> {

    /**
     * Set the default next stage that should be returned from this
     * stage after it has been invoked by default.
     *
     * @param next the next default stage in the chain.
     */
    public void setDefaultNext(Stage<DATA> next);
}
