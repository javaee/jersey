/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.memleaks.threadlocal;

import java.io.IOException;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

/**
 * A servlet that introduces a memory leak with a single call. All the classes loaded by current classloader won't get GCed even
 * after this application is undeployed.
 * <p/>
 * This servlet demonstrates that fix HK2-247 when {@link ThreadLocal} reference is changed from a static to an instance, it does
 * not always solve memory leak issues as long as a static reference (in this case {@link StaticReferenceClass#STATIC_HOLDER})
 * holds (even transitively) the {@link ThreadLocal} instance.
 * <p/>
 * To revert this case to the simple one (the original one), change the {@link #threadLocal} instance should be static and than no
 * {@link StaticReferenceClass#STATIC_HOLDER} is needed. The memory leak occurs without any other special actions.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class ThreadLocalMemoryLeakServlet extends HttpServlet {

    /**
     * This {@link ThreadLocal} reference is held by instance reference and might be GCed; however, the class {@link SomeClass}
     * has a reference to its classloader which has a reference to {@link StaticReferenceClass} class which has a static reference
     * to this instance. As a result, this {@link ThreadLocal} instance is never GCed in a thread pool environment.
     * <p/>
     * If this field was changed to a static reference, the memory leak would occur even without the {@link
     * StaticReferenceClass#STATIC_HOLDER} holding this instance (see {@link #doGet(HttpServletRequest, HttpServletResponse)}
     * method bellow).
     */
    final ThreadLocal<Class> threadLocal = new ThreadLocal<Class>() {
        @Override
        protected Class initialValue() {
            return SomeClass.class;
        }
    };

    @Override
    protected void doGet(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
        StaticReferenceClass.STATIC_HOLDER.add(threadLocal);
        response.getWriter().write("Greeting: " + SomeClass.hello() + "\n");
        response.getWriter().write("Thread Locals Content: " + threadLocal.get().getCanonicalName() + "\n");
        response.getWriter().write("Holder size: " + StaticReferenceClass.STATIC_HOLDER.size());
    }
}
