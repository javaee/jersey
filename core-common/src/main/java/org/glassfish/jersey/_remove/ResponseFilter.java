/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey._remove;

import java.io.IOException;

/**
 * <p>Interface implemented by filters invoked at the <i>Post</i>
 * extension point. Filters implementing this interface MUST be
 * annotated with {@link javax.ws.rs.ext.Provider}.</p>
 *
 * <p>As part of the client API, these filters are executed after the
 * HTTP invocation returns. As part of the server API, these filters are
 * executed after the resource method returns.</p>
 *
 * @author Santiago Pericas-Geertsen
 * @author Bill Burke
 * @since 2.0
 */
public interface ResponseFilter {

    /**
     * Filter method called at the <i>Post</i> extension point.
     * I.e., after the invocation returns in the client API and after the
     * resource method returns in the server API. Filters in a chain are
     * ordered according to their binding priority (see
     * {@link javax.ws.rs.BindingPriority}).
     *
     * @param context invocation context
     * @throws IOException if an I/O exception occurs
     */
    void postFilter(FilterContext context) throws IOException;
}
