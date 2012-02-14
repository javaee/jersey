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
 * Default implementation of a {@link StagingContext staging context}.
 * The skeleton implementation provides support for
 * tracking last stage applied to the data in the current staging context
 * as well as default implementation of the
 * {@link StagingContext#beforeStage(Stage, Object) beforeStage} and
 * {@link StagingContext#afterStage(Stage, Object) afterStage} callback methods.
 *
 * @param <DATA> supported transformable data type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class DefaultStagingContext<DATA> implements StagingContext<DATA> {

    private Stage<DATA, ?> lastStage;
    private DATA data;

    /**
     * Default protected constructor that sets the initial value of the
     * {@link StagingContext#lastStage() last applied stage} as well as
     * {@link StagingContext#data() actual state of the transformed data} to
     * {@link Optional#absent() absent}.
     * <p/>
     * Note that once the stage applying starts and the staging context
     * callback methods are invoked, the initial value of the last applied
     * stage as well as actual state of the transformed data change.
     */
    protected DefaultStagingContext() {
        this(null, null);
    }

    /**
     * Protected constructor that sets the initial value of the
     * {@link StagingContext#lastStage() last applied stage} as well as
     * {@link StagingContext#data() actual state of the transformed data} to
     * a custom stage supplied in the constructor parameter.
     * <p/>
     * Note that once the stage applying starts and the staging context
     * callback methods are invoked, the initial value of the last applied
     * stage as well as actual state of the transformed data change.
     *
     * @param lastStage stage to be initially used as the last applied stage.
     *     Passing {@code null} into the constructor will result in an
     *     {@link Optional#absent() absent} initial last applied stage.
     * @param data actual state of the transformed data to be set initially in the
     *     processing context.
     */
    protected DefaultStagingContext(Stage<DATA, ?> lastStage, DATA data) {
        this.lastStage = lastStage;
        this.data = data;
    }

    @Override
    public final void beforeStage(Stage<DATA, ?> stage, DATA data) {
        this.data = data;
        before(stage, data);
    }

    /**
     * Protected callbacks to be overridden to provide custom implementation
     * executed as part of the the {@link StagingContext#beforeStage(Stage, Object)
     * before stage} callback method.
     *
     * @param stage the stage to be applied.
     * @param data the data to be transformed by the stage.
     */
    protected void before(Stage<DATA, ?> stage, DATA data) {
        // no op
    }

    @Override
    public final void afterStage(Stage<DATA, ?> stage, DATA data) {
        this.data = data;
        this.lastStage = stage;
        after(stage, data);
    }

    /**
     * Protected callbacks to be overridden to provide custom implementation
     * executed as part of the the {@link StagingContext#afterStage(Stage, Object)
     * after stage} callback method.
     *
     * @param stage the stage previously applied.
     * @param data the stage transformation result.
     */
    protected void after(Stage<DATA, ?> stage, DATA data) {
        // no op
    }

    @Override
    public final Optional<Stage<DATA, ?>> lastStage() {
        return Optional.<Stage<DATA, ?>>fromNullable(lastStage);
    }

    @Override
    public final Optional<DATA> data() {
        return Optional.fromNullable(data);
    }
}
