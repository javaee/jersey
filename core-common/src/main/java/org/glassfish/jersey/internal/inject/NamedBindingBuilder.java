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
package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;
import java.util.List;

import org.glassfish.hk2.api.HK2Loader;
import org.glassfish.hk2.api.TypeLiteral;

/**
 * Named service binding builder.
 *
 * @param <T> service type.
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface NamedBindingBuilder<T> extends BindingBuilder<T> {
    /**
     * Bind a new contract to a service.
     *
     * @param contract contract type.
     * @return updated binder.
     */
    public NamedBindingBuilder<T> to(Class<? super T> contract);

    /**
     * Bind a new contract to a service.
     *
     * @param contract contract type.
     * @return updated binder.
     */
    public NamedBindingBuilder<T> to(TypeLiteral<?> contract);

    /**
     * Custom HK2 loader to be used when service class is about to be loaded.
     *
     * @param loader custom service loader.
     * @return updated binder.
     */
    public NamedBindingBuilder<T> loadedBy(HK2Loader loader);

    /**
     * Add binding descriptor metadata.
     *
     * The metadata can be later used to e.g. {@link org.glassfish.hk2.api.Filter filter} binding
     * descriptors.
     *
     * @param key   metadata key.
     * @param value metadata value.
     * @return updated binder.
     */
    public NamedBindingBuilder<T> withMetadata(String key, String value);

    /**
     * Add binding descriptor metadata.
     *
     * The metadata can be later used to e.g. {@link org.glassfish.hk2.api.Filter filter} binding
     * descriptors.
     *
     * @param key    metadata key.
     * @param values metadata values.
     * @return updated binder.
     */
    public NamedBindingBuilder<T> withMetadata(String key, List<String> values);

    /**
     * Add a binging qualifier annotation.
     *
     * @param annotation qualifier annotation.
     * @return updated binder.
     */
    public NamedBindingBuilder<T> qualifiedBy(Annotation annotation);

    /**
     * Scope a binding.
     *
     * @param scopeAnnotation scope annotation.
     * @return updated binding.
     */
    public ScopedNamedBindingBuilder<T> in(Class<? extends Annotation> scopeAnnotation);

    /**
     * Rank the binding. The higher rank, the more prominent position in an injected
     * {@link org.glassfish.hk2.api.IterableProvider iterable provider} for a contract.
     *
     * @param rank binding rank to be used to resolve ordering in case of multiple services
     *             are bound to the same contract.
     */
    public void ranked(int rank);
}
