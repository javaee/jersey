/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.reload.compiler;

import java.io.File;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.tools.JavaCompiler;
import javax.tools.JavaFileObject;
import javax.tools.SimpleJavaFileObject;
import javax.tools.ToolProvider;

/**
 * Java compiler utility.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class Compiler {

    private static JavaCompiler javac = ToolProvider.getSystemJavaCompiler();

    /**
     * Compiler classpath.
     */
    public static String classpath;

    /**
     * Compiles a single class and loads the class using a new class loader.
     *
     * @param className class to compile.
     * @param sourceCode source code of the class to compile.
     * @return loaded class
     * @throws Exception
     */
    public static Class<?> compile(String className, SimpleJavaFileObject sourceCode) throws Exception {
        ClassFile classFile = new ClassFile(className);

        Iterable<? extends JavaFileObject> compilationUnits = Arrays.asList(sourceCode);
        AppClassLoader cl = new AppClassLoader(Thread.currentThread().getContextClassLoader());
        FileManager fileManager = new FileManager(javac.getStandardFileManager(null, null, null), Arrays.asList(classFile), cl);
        JavaCompiler.CompilationTask task = javac.getTask(null, fileManager, null, getClOptions(), null, compilationUnits);
        task.call();
        return cl.loadClass(className);
    }


    /**
     * Compiles multiple source files at once.
     *
     * @param appClassLoader common class loader for the classes.
     * @param javaFiles source files to compile.
     * @throws Exception in case something goes wrong.
     */
    public static void compile(AppClassLoader appClassLoader, List<JavaFile> javaFiles) throws Exception {

        List<ClassFile> classes = new LinkedList<>();

        for (JavaFile javaFile : javaFiles) {
            classes.add(new ClassFile(javaFile.getClassName()));
        }
        Iterable<? extends JavaFileObject> compilationUnits = javaFiles;

        FileManager fileManager = new FileManager(javac.getStandardFileManager(null, null, null), classes, appClassLoader);
        JavaCompiler.CompilationTask task = javac.getTask(null, fileManager, null, getClOptions(), null, compilationUnits);
        task.call();
    }

    private static List<String> getClOptions() {
        List<String> optionList = new ArrayList<>();
        optionList.addAll(Arrays.asList("-classpath", classpath + File.pathSeparator + "target/classes"));
        return optionList;
    }
}
