/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.cdi.resources;

import java.util.logging.Logger;

import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import javax.annotation.ManagedBean;
import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;
import javax.annotation.Resource;
import javax.enterprise.context.ApplicationScoped;

/**
 * Application scoped JAX-RS resource registered as CDI managed bean.
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Path("/jcdibean/dependent/singleton/{p}")
@ApplicationScoped
@ManagedBean
public class JCDIBeanDependentSingletonResource {

    private static final Logger LOGGER = Logger.getLogger(JCDIBeanDependentSingletonResource.class.getName());

    @Resource(name = "injectedResource")
    private int counter = 0;

    @Context
    private UriInfo uiFieldinject;

    @Context
    private ResourceContext resourceContext;

    private UriInfo uiMethodInject;

    @Context
    public void set(UriInfo ui) {
        this.uiMethodInject = ui;
    }

    @PostConstruct
    public void postConstruct() {
        LOGGER.info(String.format("In post construct of %s", this));
        ensureInjected();
    }

    @GET
    @Produces("text/plain")
    public String getMessage(@PathParam("p") String p) {
        LOGGER.info(String.format(
                "In getMessage in %s; uiFieldInject: %s; uiMethodInject: %s", this, uiFieldinject, uiMethodInject));
        ensureInjected();

        return String.format("%s: p=%s, queryParam=%s",
                uiFieldinject.getRequestUri().toString(), p, uiMethodInject.getQueryParameters().getFirst("x"));
    }

    @Path("exception")
    public String getException() {
        throw new JDCIBeanDependentException();
    }

    @Path("counter")
    @GET
    public synchronized String getCounter() {
        return Integer.toString(counter++);
    }

    @Path("counter")
    @PUT
    public synchronized void setCounter(String counter) {
        this.counter = Integer.decode(counter);
    }

    @PreDestroy
    public void preDestroy() {
        LOGGER.info(String.format("In pre destroy of %s", this));
    }

    private void ensureInjected() throws IllegalStateException {
        if (uiFieldinject == null || uiMethodInject == null || resourceContext == null) {
            throw new IllegalStateException();
        }
    }
}
