/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.security.PrivilegedAction;

import javax.ws.rs.core.SecurityContext;

/**
 * Security context that allows establishing a subject before a resource method
 * or a sub-resource locator is called. Container or filters should set an
 * implementation of this interface to the request context using
 * {@link ContainerRequest#setSecurityContext(javax.ws.rs.core.SecurityContext)}.
 *
 * When Jersey detects this kind of context is in the request scope,
 * it will use {@link #doAsSubject(java.security.PrivilegedAction)} method to
 * dispatch the request to a resource method (or to call a sub-resource locator).
 *
 * @author Martin Matula
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public interface SubjectSecurityContext extends SecurityContext {

    /**
     * Jersey wraps calls to resource methods and sub-resource locators in
     * {@link PrivilegedAction} instance and passes it to this method when
     * dispatching a request. Implementations should do the needful to establish
     * a {@link javax.security.auth.Subject} and invoke the {@link PrivilegedAction}
     * passed as the parameter using
     * {@link javax.security.auth.Subject#doAs(javax.security.auth.Subject, java.security.PrivilegedAction)}.
     * <p>
     * The privileged action passed into the method may, when invoked, fail with either
     * {@link javax.ws.rs.WebApplicationException} or {@link javax.ws.rs.ProcessingException}.
     * Both these exceptions must be propagated to the caller without a modification.
     * </p>
     *
     * @param action {@link PrivilegedAction} that represents a resource or sub-resource locator
     *               method invocation to be executed by this method after establishing a subject.
     * @return result of the action.
     * @throws NullPointerException if the {@code PrivilegedAction} is {@code null}.
     * @throws SecurityException    if the caller does not have permission to invoke the
     *                              {@code Subject#doAs(Subject, PrivilegedAction)} method.
     * @throws javax.ws.rs.WebApplicationException
     *                              propagated exception from the privileged action. May be thrown in case the invocation
     *                              of resource or sub-resource locator method in the privileged action results in
     *                              this exception.
     * @throws javax.ws.rs.ProcessingException
     *                              propagated exception from the privileged action. May be thrown in case the invocation
     *                              of resource or sub-resource locator method in the privileged action has failed
     *                              or resulted in a non-checked exception.
     */
    public Object doAsSubject(PrivilegedAction action);
}
