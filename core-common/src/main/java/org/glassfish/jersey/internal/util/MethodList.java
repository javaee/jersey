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
package org.glassfish.jersey.internal.util;

import java.lang.annotation.Annotation;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;

/**
 *
 * @author Paul Sandoz
 */
public class MethodList implements Iterable<AnnotatedMethod> {

    private AnnotatedMethod[] methods;

    public MethodList(Class<?> c) {
        this(c, false);
    }

    public MethodList(Class<?> c, boolean declaredMethods) {
        this(declaredMethods ? getAllDeclaredMethods(c) : getMethods(c));
    }

    private static List<Method> getAllDeclaredMethods(Class c) {
        List<Method> l = new ArrayList<Method>();
        while (c != null && c != Object.class) {
            l.addAll(Arrays.asList(c.getDeclaredMethods()));
            c = c.getSuperclass();
        }
        return l;
    }

    private static List<Method> getMethods(Class<?> c) {
        return Arrays.asList(c.getMethods());
    }

    public MethodList(List<Method> methods) {
        List<AnnotatedMethod> l = new ArrayList<AnnotatedMethod>();
        for (Method m : methods) {
            if (!m.isBridge() && m.getDeclaringClass() != Object.class) {
                l.add(new AnnotatedMethod(m));
            }
        }

        this.methods = new AnnotatedMethod[l.size()];
        this.methods = l.toArray(this.methods);
    }

    public MethodList(Method... methods) {
        List<AnnotatedMethod> l = new ArrayList<AnnotatedMethod>();
        for (Method m : methods) {
            if (!m.isBridge() && m.getDeclaringClass() != Object.class) {
                l.add(new AnnotatedMethod(m));
            }
        }

        this.methods = new AnnotatedMethod[l.size()];
        this.methods = l.toArray(this.methods);
    }

    public MethodList(AnnotatedMethod... methods) {
        this.methods = methods;
    }

    @Override
    public Iterator<AnnotatedMethod> iterator() {
        return Arrays.asList(methods).iterator();
    }

    public <T extends Annotation> MethodList isNotPublic() {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return !Modifier.isPublic(m.getMethod().getModifiers());
            }
        });
    }

    public <T extends Annotation> MethodList hasNumParams(final int i) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getParameterTypes().length == i;
            }
        });
    }

    public <T extends Annotation> MethodList hasReturnType(final Class<?> r) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getMethod().getReturnType() == r;
            }
        });
    }

    public <T extends Annotation> MethodList nameStartsWith(final String s) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getMethod().getName().startsWith(s);
            }
        });
    }

    public <T extends Annotation> MethodList hasAnnotation(final Class<T> annotation) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getAnnotation(annotation) != null;
            }
        });
    }

    public <T extends Annotation> MethodList hasMetaAnnotation(final Class<T> annotation) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                for (Annotation a : m.getAnnotations()) {
                    if (a.annotationType().getAnnotation(annotation) != null) {
                        return true;
                    }
                }
                return false;
            }
        });
    }

    public <T extends Annotation> MethodList hasNotAnnotation(final Class<T> annotation) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                return m.getAnnotation(annotation) == null;
            }
        });
    }

    public <T extends Annotation> MethodList hasNotMetaAnnotation(final Class<T> annotation) {
        return filter(new Filter() {

            @Override
            public boolean keep(AnnotatedMethod m) {
                for (Annotation a : m.getAnnotations()) {
                    if (a.annotationType().getAnnotation(annotation) != null) {
                        return false;
                    }
                }
                return true;
            }
        });
    }

    public interface Filter {

        boolean keep(AnnotatedMethod m);
    }

    public MethodList filter(Filter f) {
        List<AnnotatedMethod> r = new ArrayList<AnnotatedMethod>();
        for (AnnotatedMethod m : methods) {
            if (f.keep(m)) {
                r.add(m);
            }
        }
        return new MethodList(r.toArray(new AnnotatedMethod[0]));
    }
}