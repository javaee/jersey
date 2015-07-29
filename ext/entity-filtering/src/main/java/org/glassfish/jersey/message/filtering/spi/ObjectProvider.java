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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import org.glassfish.jersey.spi.Contract;

/**
 * Entry point of Entity Data Filtering feature for providers working with custom entities and media types (reading/writing).
 * Exposed methods are supposed to provide an entity-filtering object of defined type (generic parameter type
 * {@code &lt;T&gt;}) for given types/classes that is requested by underlying provider (e.g. message body worker).
 * <p>
 * Methods are also accepting a list of entity annotations which entity-filtering scopes and then particular entity-filtering
 * object are determined from. Entity annotations can be passed to the runtime via:
 * <ul>
 * <li>{@link javax.ws.rs.client.Entity#entity(Object, javax.ws.rs.core.MediaType, java.lang.annotation.Annotation[])} on the
 * client, or</li>
 * <li>{@link javax.ws.rs.core.Response.ResponseBuilder#entity(Object, java.lang.annotation.Annotation[])} on the server</li>
 * </ul>
 * </p>
 * <p>
 * Custom implementations should, during processing, look up for available {@link EntityProcessor entity processors} to examine
 * given entity classes and {@link ScopeResolver scope providers} to determine the current entity-filtering scope. Entity class
 * and entity-filtering scopes determine the {@link ObjectGraph object graph} passed to {@link ObjectGraphTransformer object graph
 * transformer} and hence the resulting entity-filtering object.
 * </p>
 * <p>
 * Implementations should be registered into client/server runtime via
 * {@link org.glassfish.hk2.utilities.binding.AbstractBinder HK2 binder} (for more information and common implementation see
 * {@link AbstractObjectProvider}):
 * <pre>
 * bindAsContract(MyObjectProvider.class)
 *       // FilteringGraphTransformer.
 *       .to(new TypeLiteral&lt;ObjectGraphTransformer&lt;MyFilteringObject&gt;&gt;() {})
 *       // Scope.
 *       .in(Singleton.class);
 * </pre>
 * The custom provider can be then {@link javax.inject.Inject injected} as one these injection point types:
 * <ul>
 * <li>{@code MyObjectProvider}</li>
 * <li>{@code javax.inject.Provider&lt;ObjectProvider&lt;MyFilteringObject&gt;&gt;}</li>
 * </ul>
 * </p>
 * <p>
 * By default a {@code ObjectGraph} provider is available in the runtime. This object provider can be injected (via
 * {@link javax.inject.Inject @Inject}) into the following types:
 * <ul>
 * <li>{@code ObjectProvider}</li>
 * <li>{@code javax.inject.Provider&lt;ObjectProvider&lt;Object&gt;&gt;}</li>
 * <li>{@code javax.inject.Provider&lt;ObjectProvider&lt;ObjectGraph&gt;&gt;}</li>
 * </ul>
 * </p>
 * <p>
 * Note: For most of the cases it is sufficient that users implement {@link ObjectGraphTransformer object graph transformer} by
 * extending {@link AbstractObjectProvider} class.
 * </p>
 *
 * @param <T> representation of entity data filtering requested by provider.
 * @author Michal Gajdos
 * @see AbstractObjectProvider
 * @see ObjectGraphTransformer
 */
@Contract
public interface ObjectProvider<T> {

    /**
     * Get reader/writer entity-filtering object for given type.
     *
     * @param genericType type for which the object is requested.
     * @param forWriter flag to determine whether to create object for reading/writing purposes.
     * @param annotations entity annotations to determine the runtime scope.
     * @return entity-filtering object.
     */
    public T getFilteringObject(Type genericType, boolean forWriter, final Annotation... annotations);
}
