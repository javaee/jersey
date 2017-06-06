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

package org.glassfish.jersey.inject.cdi.se;

import javax.inject.Inject;

import org.glassfish.jersey.process.internal.RequestContext;
import org.glassfish.jersey.process.internal.RequestScope;

import org.jboss.weld.context.bound.BoundRequestContext;

/**
 * CDI Request scope implementation using Weld-specific {@link BoundRequestContext} which allows pass on storage for
 * request-scoped objects.
 */
public class CdiRequestScope extends RequestScope {

    @Inject
    private BoundRequestContext requestContextController;

    @Override
    public RequestContext createContext() {
        return new CdiRequestContext();
    }

    @Override
    protected void activate(RequestContext context, RequestContext oldContext) {
        super.activate(context, oldContext);

        if (oldContext != null) {
            CdiRequestContext oldRequestContext = (CdiRequestContext) oldContext;
            requestContextController.deactivate();
            requestContextController.dissociate(oldRequestContext.getStore());
        }

        CdiRequestContext cdiRequestContext = (CdiRequestContext) context;
        requestContextController.associate(cdiRequestContext.getStore());
        requestContextController.activate();
    }

    @Override
    protected void resume(RequestContext context) {
        super.resume(context);

        if (context != null) {
            CdiRequestContext cdiRequestContext = (CdiRequestContext) context;
            requestContextController.associate(cdiRequestContext.getStore());
            requestContextController.activate();
        }
    }

    @Override
    protected void release(RequestContext context) {
        super.release(context);

        CdiRequestContext cdiRequestContext = (CdiRequestContext) context;
        requestContextController.invalidate();
        requestContextController.deactivate();
        requestContextController.dissociate(cdiRequestContext.getStore());
    }

    @Override
    protected void suspend(RequestContext context) {
        if (context != null) {
            CdiRequestContext cdiRequestContext = (CdiRequestContext) context;
            requestContextController.deactivate();
            requestContextController.dissociate(cdiRequestContext.getStore());
        }
    }
}
