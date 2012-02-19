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

import com.google.common.base.Optional;

/**
 * Stage applying context.
 * <p/>
 * Starting at a root, {@link Stage stages} are applied and the returned continuation
 * is resolved until a terminal stage, the one that does not return a continuation that
 * could be further followed, is reached. With each stage the registered staging
 * context callback methods are invoked
 * {@link #beforeStage(org.glassfish.jersey.process.internal.Stage, java.lang.Object) before}
 * and {@link #afterStage(org.glassfish.jersey.process.internal.Stage, java.lang.Object)  after}
 * a stage is {@link Stage#apply(java.lang.Object) applied}.
 *
 * @param <DATA> supported transformable data type.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see LinearRequestProcessor
 * @see HierarchicalRequestProcessor
 */
public interface StagingContext<DATA> {

    /**
     * Callback method invoked before each stage in the continuation of stages
     * is {@link Stage#apply(java.lang.Object) applied}.
     *
     * @param stage the stage to be applied.
     * @param data the data to be transformed by the stage.
     */
    public void beforeStage(Stage<DATA, ?> stage, DATA data);

    /**
     * Callback method invoked after each stage in the continuation of stages
     * is {@link Stage#apply(java.lang.Object) applied}.
     *
     * @param stage the stage recently applied.
     * @param data the stage transformation result.
     */
    public void afterStage(Stage<DATA, ?> stage, DATA data);

    /**
     * Get the last stage applied in the current staging context. The returned
     * stage can be {@link Optional#absent()} in case no stage has been applied yet.
     *
     * @return last stage applied in the current processing context.
     */
    public Optional<Stage<DATA, ?>> lastStage();

    /**
     * Get the processed data in the actual state in the current processing context.
     *
     * @return actual state of the processed data.
     */
    public Optional<DATA> data();
}
