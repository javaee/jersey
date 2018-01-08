/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.internal.scanning;

import java.io.IOException;
import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.ws.rs.Path;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.internal.OsgiRegistry;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.server.internal.LocalizationMessages;

import jersey.repackaged.org.objectweb.asm.AnnotationVisitor;
import jersey.repackaged.org.objectweb.asm.Attribute;
import jersey.repackaged.org.objectweb.asm.ClassReader;
import jersey.repackaged.org.objectweb.asm.ClassVisitor;
import jersey.repackaged.org.objectweb.asm.FieldVisitor;
import jersey.repackaged.org.objectweb.asm.MethodVisitor;
import jersey.repackaged.org.objectweb.asm.Opcodes;

/**
 * A scanner listener that processes Java class files (resource names
 * ending in ".class") annotated with one or more of a set of declared
 * annotations.
 * <p>
 * Java classes of a Java class file are processed, using ASM, to ascertain
 * if those classes are annotated with one or more of the set of declared
 * annotations.
 * <p>
 * Such an annotated Java class of a Java class file is loaded if the class
 * is public or is an inner class that is static and public.
 *
 * @author Paul Sandoz
 */
public final class AnnotationAcceptingListener implements ResourceProcessor {

    private final ClassLoader classloader;

    private final Set<Class<?>> classes;

    private final Set<String> annotations;

    private final AnnotatedClassVisitor classVisitor;

    /**
     * Create a scanning listener to check for Java classes in Java
     * class files annotated with {@link javax.ws.rs.Path} or {@link javax.ws.rs.ext.Provider}.
     *
     * @return new instance of {@link AnnotationAcceptingListener} which looks for
     * {@link javax.ws.rs.Path} or {@link javax.ws.rs.ext.Provider} annotated classes.
     *
     */
    @SuppressWarnings({"unchecked"})
    public static AnnotationAcceptingListener newJaxrsResourceAndProviderListener() {
        return new AnnotationAcceptingListener(Path.class, Provider.class);
    }

    /**
     * Create a scanning listener to check for Java classes in Java
     * class files annotated with {@link Path} or {@link Provider}.
     *
     * @param classLoader the class loader to use to load Java classes that
     *        are annotated with any one of the annotations.
     * @return new instance of {@link AnnotationAcceptingListener} which looks for
     * {@link javax.ws.rs.Path} or {@link javax.ws.rs.ext.Provider} annotated classes.
     */
    @SuppressWarnings({"unchecked"})
    public static AnnotationAcceptingListener newJaxrsResourceAndProviderListener(final ClassLoader classLoader) {
        return new AnnotationAcceptingListener(classLoader, Path.class, Provider.class);
    }

    /**
     * Create a scanner listener to check for annotated Java classes in Java
     * class files.
     *
     * @param annotations the set of annotation classes to check on Java class
     *        files.
     */
    public AnnotationAcceptingListener(final Class<? extends Annotation>... annotations) {
        this(AccessController.doPrivileged(ReflectionHelper.getContextClassLoaderPA()), annotations);
    }

    /**
     * Create a scanner listener to check for annotated Java classes in Java
     * class files.
     *
     * @param classloader the class loader to use to load Java classes that
     *        are annotated with any one of the annotations.
     * @param annotations the set of annotation classes to check on Java class
     *        files.
     */
    public AnnotationAcceptingListener(final ClassLoader classloader,
                                       final Class<? extends Annotation>... annotations) {
        this.classloader = classloader;
        this.classes = new LinkedHashSet<Class<?>>();
        this.annotations = getAnnotationSet(annotations);
        this.classVisitor = new AnnotatedClassVisitor();
    }

    /**
     * Get the set of annotated classes.
     *
     * @return the set of annotated classes.
     */
    public Set<Class<?>> getAnnotatedClasses() {
        return classes;
    }

    private Set<String> getAnnotationSet(final Class<? extends Annotation>... annotations) {
        final Set<String> a = new HashSet<String>();
        for (final Class c : annotations) {
            a.add("L" + c.getName().replaceAll("\\.", "/") + ";");
        }
        return a;
    }

    // ScannerListener
    public boolean accept(final String name) {
        return !(name == null || name.isEmpty()) && name.endsWith(".class");

    }

    public void process(final String name, final InputStream in) throws IOException {
        new ClassReader(in).accept(classVisitor, 0);
    }

    //

    private final class AnnotatedClassVisitor extends ClassVisitor {

        /**
         * The name of the visited class.
         */
        private String className;
        /**
         * True if the class has the correct scope
         */
        private boolean isScoped;
        /**
         * True if the class has the correct declared annotations
         */
        private boolean isAnnotated;

        private AnnotatedClassVisitor() {
            super(Opcodes.ASM5);
        }

        public void visit(final int version, final int access, final String name,
                          final String signature, final String superName, final String[] interfaces) {
            className = name;
            isScoped = (access & Opcodes.ACC_PUBLIC) != 0;
            isAnnotated = false;
        }

        public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
            isAnnotated |= annotations.contains(desc);
            return null;
        }

        public void visitInnerClass(final String name, final String outerName,
                                    final String innerName, final int access) {
            // If the name of the class that was visited is equal
            // to the name of this visited inner class then
            // this access field needs to be used for checking the scope
            // of the inner class
            if (className.equals(name)) {
                isScoped = (access & Opcodes.ACC_PUBLIC) != 0;

                // Inner classes need to be statically scoped
                isScoped &= (access & Opcodes.ACC_STATIC) == Opcodes.ACC_STATIC;
            }
        }

        public void visitEnd() {
            if (isScoped && isAnnotated) {
                // Correctly scoped and annotated
                // add to the set of matching classes.
                classes.add(getClassForName(className.replaceAll("/", ".")));
            }
        }

        public void visitOuterClass(final String string, final String string0,
                                    final String string1) {
            // Do nothing
        }

        public FieldVisitor visitField(final int i, final String string,
                                       final String string0, final String string1,
                                       final Object object) {
            // Do nothing
            return null;
        }

        public void visitSource(final String string, final String string0) {
            // Do nothing
        }

        public void visitAttribute(final Attribute attribute) {
            // Do nothing
        }

        public MethodVisitor visitMethod(final int i, final String string,
                                         final String string0, final String string1,
                                         final String[] string2) {
            // Do nothing
            return null;
        }

        private Class getClassForName(final String className) {
            try {
                final OsgiRegistry osgiRegistry = ReflectionHelper.getOsgiRegistryInstance();

                if (osgiRegistry != null) {
                    return osgiRegistry.classForNameWithException(className);
                } else {
                    return AccessController.doPrivileged(ReflectionHelper.classForNameWithExceptionPEA(className, classloader));
                }
            } catch (final ClassNotFoundException ex) {
                throw new RuntimeException(LocalizationMessages.ERROR_SCANNING_CLASS_NOT_FOUND(className), ex);
            } catch (final PrivilegedActionException pae) {
                final Throwable cause = pae.getCause();
                if (cause instanceof ClassNotFoundException) {
                    throw new RuntimeException(LocalizationMessages.ERROR_SCANNING_CLASS_NOT_FOUND(className), cause);
                } else if (cause instanceof RuntimeException) {
                    throw (RuntimeException) cause;
                } else {
                    throw new RuntimeException(cause);
                }
            }
        }

    }
}
