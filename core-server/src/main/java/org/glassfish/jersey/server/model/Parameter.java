/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.model;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Type;

/**
 * Abstraction for a method parameter
 */
public class Parameter implements AnnotatedElement {

    public enum Source {ENTITY, QUERY, MATRIX, PATH, COOKIE, HEADER, CONTEXT, FORM, UNKNOWN};

    private final Annotation[] annotations;
    private final Annotation annotation;
    private final Parameter.Source source;
    private final String sourceName;
    private final boolean encoded;
    private final String defaultValue;
    private final Type type;
    private final Class<?> clazz;

    public Parameter(Annotation[] markers, Annotation marker, Source source, String sourceName, Type type, Class<?> clazz) {
        this(markers, marker, source, sourceName, type, clazz, false, null);
    }

    public Parameter(Annotation[] markers, Annotation marker, Source source, String sourceName, Type type, Class<?> clazz, boolean encoded) {
        this(markers, marker, source, sourceName, type, clazz, encoded, null);
    }

    public Parameter(Annotation[] markers, Annotation marker, Source source, String sourceName, Type type, Class<?> clazz, String defaultValue) {
        this(markers, marker, source, sourceName, type, clazz, false, defaultValue);
    }

    public Parameter(Annotation[] markers, Annotation marker, Source source, String sourceName, Type type, Class<?> clazz, boolean encoded, String defaultValue) {
        this.annotations = markers;
        this.annotation = marker;
        this.source = source;
        this.sourceName = sourceName;
        this.type = type;
        this.clazz = clazz;
        this.encoded = encoded;
        this.defaultValue = defaultValue;
    }

    public Annotation getAnnotation() {
        return annotation;
    }

    public Parameter.Source getSource() {
        return source;
    }

    public String getSourceName() {
        return sourceName;
    }

    public boolean isEncoded() {
        return encoded;
    }

    public boolean hasDefaultValue() {
        return defaultValue != null;
    }

    public String getDefaultValue() {
        return defaultValue;
    }

    public Class<?> getParameterClass() {
        return clazz;
    }

    public Type getParameterType() {
        return type;
    }

    public boolean isQualified() {
        for (Annotation a : getAnnotations()) {
            if (a.annotationType().isAnnotationPresent(ParamQualifier.class)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isAnnotationPresent(Class<? extends Annotation> annotationClass) {
        return getAnnotation(annotationClass) != null;
    }

     @Override
    public <T extends Annotation> T getAnnotation(Class<T> annotationClass) {
        if (annotationClass == null) {
             return null;
         }
        for (Annotation a : annotations) {
            if (a.annotationType() == annotationClass) {
                return annotationClass.cast(a);
            }
         }
         return null;
     }

     @Override
    public Annotation[] getAnnotations() {
        return annotations.clone();
     }

     @Override
    public Annotation[] getDeclaredAnnotations() {
        return annotations.clone();
     }
}