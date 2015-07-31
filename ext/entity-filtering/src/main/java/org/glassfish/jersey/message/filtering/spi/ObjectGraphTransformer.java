/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.message.filtering.spi;

import org.glassfish.jersey.spi.Contract;

/**
 * This contract brings support for transforming an internal representation of entity data filtering feature into an object
 * familiar to underlying provider (e.g. message body worker).
 * <p>
 * This interface is supposed to be implemented by modules providing JAX-RS/Jersey providers / configuration object (e.g. message
 * body workers) that can directly affect reading/writing of an entity.
 * </p>
 * <p>
 * Implementations should be registered into client/server runtime via
 * {@link org.glassfish.hk2.utilities.binding.AbstractBinder HK2 binder} (for more information and common implementation see
 * {@link AbstractObjectProvider}):
 * <pre>
 * bindAsContract(MyObjectGraphTransformer.class)
 *       // FilteringGraphTransformer.
 *       .to(new TypeLiteral&lt;ObjectGraphTransformer&lt;MyFilteringObject&gt;&gt;() {})
 *       // Scope.
 *       .in(Singleton.class);
 * </pre>
 * The custom transformer can be then {@link javax.inject.Inject injected} as one these injection point types:
 * <ul>
 * <li>{@code MyObjectGraphTransformer}</li>
 * <li>{@code javax.inject.Provider&lt;ObjectGraphTransformer&lt;MyFilteringObject&gt;&gt;}</li>
 * </ul>
 * </p>
 * <p>
 * By default a {@code ObjectGraph} -&gt; {@code ObjectGraph} transformer is available in the runtime. This transformer can be
 * injected (via {@link javax.inject.Inject @Inject}) into the following types:
 * <ul>
 * <li>{@code ObjectGraphTransformer}</li>
 * <li>{@code javax.inject.Provider&lt;ObjectGraphTransformer&lt;Object&gt;&gt;}</li>
 * <li>{@code javax.inject.Provider&lt;ObjectGraphTransformer&lt;ObjectGraph&gt;&gt;}</li>
 * </ul>
 * </p>
 *
 * @param <T> representation of entity data filtering requested by provider.
 * @author Michal Gajdos
 * @see AbstractObjectProvider
 * @see ObjectProvider
 */
@Contract
public interface ObjectGraphTransformer<T> {

    /**
     * Transform a given graph into an entity-filtering object. The entire graph (incl. it's subgraphs) should be processed by
     * this method as this method is invoked only once for a root entity class.
     *
     * @param graph object graph to be transformed.
     * @return entity-filtering object requested by provider.
     */
    public T transform(final ObjectGraph graph);
}
