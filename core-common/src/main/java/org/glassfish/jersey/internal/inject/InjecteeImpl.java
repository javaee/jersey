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
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.lang.reflect.Type;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import org.glassfish.jersey.internal.util.Pretty;

/**
 * An Injectee represents the point of injection. It can be used by injection resolvers to discover all of the information
 * available about the entity being injected into.
 */
public class InjecteeImpl implements Injectee {

    private Type requiredType;
    private Set<Annotation> qualifiers;
    private Class<? extends Annotation> parentClassScope;
    private int position = -1;
    private Class<?> injecteeClass;
    private AnnotatedElement parent;
    private boolean isOptional = false;
    private boolean isFactory = false;
    private boolean isProvider = false;
    private ForeignDescriptor injecteeDescriptor;

    @Override
    public Type getRequiredType() {
        return requiredType;
    }

    /**
     * Sets the required type of this Injectee.
     *
     * @param requiredType The required type of this injectee.
     */
    public void setRequiredType(Type requiredType) {
        this.requiredType = requiredType;
    }

    @Override
    public Set<Annotation> getRequiredQualifiers() {
        if (qualifiers == null) {
            return Collections.emptySet();
        }
        return qualifiers;
    }

    /**
     * Sets the required qualifiers for this Injectee.
     *
     * @param requiredQualifiers The non-null set of required qualifiers.
     */
    public void setRequiredQualifiers(Set<Annotation> requiredQualifiers) {
        qualifiers = Collections.unmodifiableSet(requiredQualifiers);

    }

    @Override
    public Class<? extends Annotation> getParentClassScope() {
        return parentClassScope;
    }


    /**
     * Sets the scope in which the parent class is registered.
     *
     * @return parent class scope.
     */
    public void setParentClassScope(Class<? extends Annotation> parentClassScope) {
        this.parentClassScope = parentClassScope;
    }

    @Override
    public boolean isFactory() {
        return isFactory;
    }

    /**
     * Sets a flag whether the injectee is a factory.
     *
     * @param factory {@code true} flag whether the injectee is factory.
     */
    public void setFactory(boolean factory) {
        isFactory = factory;
    }

    @Override
    public boolean isProvider() {
        return isProvider;
    }

    /**
     * Sets a flag whether the injectee is a provider.
     *
     * @param provider {@code true} flag whether the injectee is provider.
     */
    public void setProvider(boolean provider) {
        isProvider = provider;
    }

    @Override
    public int getPosition() {
        return position;
    }

    /**
     * Sets the position of this Injectee. The position represents the index of the parameter, or {@code -1} if this Injectee is
     * describing a field.
     *
     * @param position The index position of the parameter, or {@code -1} if describing a field.
     */
    public void setPosition(int position) {
        this.position = position;
    }

    @Override
    public Class<?> getInjecteeClass() {
        return injecteeClass;
    }

    /**
     * Sets type of the injectee.
     *
     * @param injecteeClass injectee type.
     */
    public void setInjecteeClass(final Class<?> injecteeClass) {
        this.injecteeClass = injecteeClass;
    }

    @Override
    public AnnotatedElement getParent() {
        return parent;
    }

    /**
     * This setter sets both the parent and the injecteeClass fields.
     *
     * @param parent The parent (Field, Constructor or Method) which is the parent of this Injectee.
     */
    public void setParent(AnnotatedElement parent) {
        this.parent = parent;

        if (parent instanceof Field) {
            injecteeClass = ((Field) parent).getDeclaringClass();
        } else if (parent instanceof Constructor) {
            injecteeClass = ((Constructor<?>) parent).getDeclaringClass();
        } else if (parent instanceof Method) {
            injecteeClass = ((Method) parent).getDeclaringClass();
        }
    }

    @Override
    public boolean isOptional() {
        return isOptional;
    }

    /**
     * Sets whether or not this Injectee should be considered optional.
     *
     * @param optional true if this injectee is optional, false if required.
     */
    public void setOptional(boolean optional) {
        this.isOptional = optional;
    }

    @Override
    public ForeignDescriptor getInjecteeDescriptor() {
        return injecteeDescriptor;
    }

    /**
     * Sets the descriptor for this Injectee.
     *
     * @param injecteeDescriptor injectee's descriptor.
     */
    public void setInjecteeDescriptor(ForeignDescriptor injecteeDescriptor) {
        this.injecteeDescriptor = injecteeDescriptor;
    }

    @Override
    public String toString() {
        return "InjecteeImpl(requiredType=" + Pretty.type(requiredType)
                + ",parent=" + Pretty.clazz(parent.getClass())
                + ",qualifiers=" + Pretty.collection(qualifiers)
                + ",position=" + position
                + ",factory=" + isFactory
                + ",provider=" + isProvider
                + ",optional=" + isOptional
                + "," + System.identityHashCode(this) + ")";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (!(o instanceof InjecteeImpl)) {
            return false;
        }
        InjecteeImpl injectee = (InjecteeImpl) o;
        return position == injectee.position
                && isOptional == injectee.isOptional
                && isFactory == injectee.isFactory
                && isProvider == injectee.isProvider
                && Objects.equals(requiredType, injectee.requiredType)
                && Objects.equals(qualifiers, injectee.qualifiers)
                && Objects.equals(injecteeClass, injectee.injecteeClass)
                && Objects.equals(parent, injectee.parent);
    }

    @Override
    public int hashCode() {
        return Objects.hash(requiredType, qualifiers, position, injecteeClass, parent, isOptional, isFactory);
    }
}
