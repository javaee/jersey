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
package org.glassfish.jersey.tests.integration.j376;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.validation.Valid;
import javax.ws.rs.BeanParam;

/**
 * Resource to test CDI injection into JAX-RS resource via field.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@Path("field")
@RequestScoped
public class FieldInjectionResource {

    /** CDI injected request scoped field */
    @Inject
    @Valid
    @BeanParam
    private FormDataBean bean;

    /** CDI injected applciation scoped bean */
    @Inject
    private ApplicationScopedBean appScoped;

    /**
     * Return string containing of fields from the injected non JAX-RS request scoped bean,
     * path injected into it via {@code Context} annotation and another bean injected into it.
     *
     * Shows, that {@code Inject} and {@code Context} annotations can be used on one particular non JAX-RS class.
     **/
    @POST
    @Produces("text/plain")
    public String get() {
        return bean.getName() + ":" + bean.getAge() + ":"
                + bean.getInjectedBean().getMessage() + ":" + bean.getInjectedPath();
    }

    /** Return string from the {@code ApplicationScoped} non JAX_RS bean injected into this JAX-RS resource. */
    @GET
    @Path("appScoped")
    @Produces("text/plain")
    public String getMessage() {
        return appScoped.getMessage();
    }

    /**
     * Return path injected via {@code Context} annotation into {@code ApplicationScoped} non JAX-RS bean, that is
     * further injected into this JAX-RS resource via CDI.
     */
    @GET
    @Path("appScopedUri")
    public String getUri() {
        return appScoped.getUri();
    }
}
