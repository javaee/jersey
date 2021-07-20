/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.helloworld;

import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.Suspended;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

/**
 * JAX-RS resource class backed by a request scoped CDI bean.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RequestScoped
@Path("req")
public class RequestScopedResource {

    @Inject
    AppScopedResource appResource;

    @Inject
    RequestScopedBean bean;

    @GET
    @Path("app/counter")
    public int getCounter() {
        return appResource.getCount();
    }

    @GET
    @Path("myself")
    public String getMyself() {
        return this.toString();
    }

    @GET
    @Path("parameterized")
    @ResponseBodyFromCdiBean
    public String interceptedParameterized(@QueryParam("q") String q) {
        bean.setRequestId(q);
        return "does not matter";
    }

    @GET
    @Path("straight")
    public String parameterizedStraight(@QueryParam("q") String q) {
        return "straight: " + q;
    }

    private static final Executor executor = Executors.newCachedThreadPool();

    @GET
    @Path("parameterized-async")
    @ResponseBodyFromCdiBean
    public void interceptedParameterizedAsync(@QueryParam("q") final String q, @Suspended final AsyncResponse response) {
        bean.setRequestId(q);
        executor.execute(new Runnable() {

            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException ex) {
                    Logger.getLogger(RequestScopedResource.class.getName()).log(Level.SEVERE, null, ex);
                }
                response.resume("this will never make it to the client");
            }
        });
    }

    @Context
    UriInfo uriInfo;

    @Inject App.JaxRsApplication jaxRsApplication;

    @GET
    @Path("ui/jax-rs-field/{d}")
    public String getJaxRsInjectedUIUri() {

        if (uriInfo == jaxRsApplication.uInfo) {
            throw new IllegalStateException("UriInfo injected into req scoped cdi bean should not get proxied.");
        }

        return uriInfo.getRequestUri().toString();
    }

    @GET
    @Path("ui/jax-rs-app-field/{d}")
    public String getCdiInjectedJaxRsAppUri() {
        return jaxRsApplication.uInfo.getRequestUri().toString();
    }
}
