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

package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import javax.ws.rs.core.GenericType;

import javax.inject.Named;

/**
 * Abstract injection binding description of a bean.
 *
 * @param <T> type of the bean described by this injection binding.
 * @param <D> concrete injection binding implementation type.
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
@SuppressWarnings("unchecked")
public abstract class Binding<T, D extends Binding> {

    private final Set<Type> contracts = new HashSet<>();
    private final Set<Annotation> qualifiers = new HashSet<>();
    private final Set<AliasBinding> aliases = new HashSet<>();
    private Class<? extends Annotation> scope = null;
    private String name = null;
    private Class<T> implementationType = null;
    private String analyzer = null;
    private Boolean proxiable = null;
    private Boolean proxyForSameScope = null;
    private Integer ranked = null;

    /**
     * Gets information whether the service is proxiable.
     *
     * @return {@code true} if the service is proxiable.
     */
    public Boolean isProxiable() {
        return proxiable;
    }

    /**
     * Gets information whether the service creates the proxy for the same scope.
     *
     * @return {@code true} if the service creates the proxy for the same scop.
     */
    public Boolean isProxiedForSameScope() {
        return proxyForSameScope;
    }

    /**
     * Gets rank of the service.
     *
     * @return service's rank.
     */
    public Integer getRank() {
        return ranked;
    }

    /**
     * Gets service's contracts.
     *
     * @return service's contracts.
     */
    public Set<Type> getContracts() {
        return contracts;
    }

    /**
     * Gets service's qualifiers.
     *
     * @return service's qualifiers.
     */
    public Set<Annotation> getQualifiers() {
        return qualifiers;
    }

    /**
     * Gets service's scope.
     *
     * @return service's scope.
     */
    public Class<? extends Annotation> getScope() {
        return scope;
    }

    /**
     * Gets service's name.
     *
     * @return service's name.
     */
    public String getName() {
        return name;
    }

    /**
     * Gets service's type.
     *
     * @return service's type.
     */
    public Class<T> getImplementationType() {
        return implementationType;
    }

    /**
     * Gets service's analyzer.
     *
     * @return service's analyzer.
     */
    public String getAnalyzer() {
        return analyzer;
    }

    /**
     * Gets service's aliases.
     *
     * @return service's aliases.
     */
    public Set<AliasBinding> getAliases() {
        return aliases;
    }

    /**
     * Adds service's analyzer.
     *
     * @return current instance.
     */
    // TODO: Candidate to remove, used only in legacy CDI integration.
    public D analyzeWith(String analyzer) {
        this.analyzer = analyzer;
        return (D) this;
    }

    /**
     * Adds service's contracts.
     *
     * @return current instance.
     */
    public D to(Collection<Class<? super T>> contracts) {
        if (contracts != null) {
            this.contracts.addAll(contracts);
        }
        return (D) this;
    }

    /**
     * Adds service's contract.
     *
     * @return current instance.
     */
    public D to(Class<? super T> contract) {
        this.contracts.add(contract);
        return (D) this;
    }

    /**
     * Adds service's contract.
     *
     * @return current instance.
     */
    public D to(GenericType<?> contract) {
        this.contracts.add(contract.getType());
        return (D) this;
    }

    /**
     * Adds service's contract.
     *
     * @return current instance.
     */
    public D to(Type contract) {
        this.contracts.add(contract);
        return (D) this;
    }

    /**
     * Adds service's qualifier.
     *
     * @return current instance.
     */
    public D qualifiedBy(Annotation annotation) {
        if (Named.class.equals(annotation.annotationType())) {
            this.name = ((Named) annotation).value();
        }
        this.qualifiers.add(annotation);
        return (D) this;
    }

    /**
     * Adds service's scope.
     *
     * @return current instance.
     */
    public D in(Class<? extends Annotation> scopeAnnotation) {
        this.scope = scopeAnnotation;
        return (D) this;
    }

    /**
     * Adds service's name.
     *
     * @return current instance.
     */
    public D named(String name) {
        this.name = name;
        return (D) this;
    }

    /**
     * Adds service's alias.
     *
     * @param contract contract of the alias.
     * @return instance of a new alias for this binding descriptor that can be further specified.
     */
    public AliasBinding addAlias(Class<?> contract) {
        AliasBinding alias = new AliasBinding(contract);
        aliases.add(alias);
        return alias;
    }

    /**
     * Adds information about proxy creation.
     *
     * @return current instance.
     */
    public D proxy(boolean proxiable) {
        this.proxiable = proxiable;
        return (D) this;
    }

    /**
     * Adds information about proxy creation when the service is in the same scope.
     *
     * @return current instance.
     */
    public D proxyForSameScope(boolean proxyForSameScope) {
        this.proxyForSameScope = proxyForSameScope;
        return (D) this;
    }

    /**
     * Adds service's rank.
     *
     * @return current instance.
     */
    public void ranked(int rank) {
        this.ranked = rank;
    }

    /**
     * Adds service's type.
     *
     * @return current instance.
     */
    D asType(Class type) {
        this.implementationType = type;
        return (D) this;
    }
}
