/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import javax.enterprise.context.RequestScoped;
import javax.inject.Inject;
import javax.inject.Provider;

import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.monitoring.MonitoringStatistics;
import org.glassfish.jersey.spi.ExceptionMappers;

/**
 * CDI backed, request scoped, JAX-RS resource to be injected
 * via it's constructor from both CDI and Jersey HK2.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@RequestScoped
@Path("request-ctor-injected")
public class RequestScopedCtorInjectedResource {

    // CDI injected
    EchoService echoService;

    // Jersey injected
    ContainerRequest request;
    ExceptionMappers mappers;
    Provider<MonitoringStatistics> stats;

    // Jersey/HK2 custom injected
    MyApplication.MyInjection customInjected;
    // Jersey/HK2 custom injected
    Hk2InjectedType hk2Injected;

    // to make weld happy
    public RequestScopedCtorInjectedResource() {
    }

    @Inject
    public RequestScopedCtorInjectedResource(@RequestSpecific final EchoService echoService,
                                             final ContainerRequest request,
                                             final ExceptionMappers mappers,
                                             final Provider<MonitoringStatistics> stats,
                                             final MyApplication.MyInjection customInjected,
                                             final Hk2InjectedType hk2Injected) {

        this.echoService = echoService;
        this.mappers = mappers;
        this.request = request;
        this.stats = stats;
        this.customInjected = customInjected;
        this.hk2Injected = hk2Injected;
    }

    @GET
    public String echo(@QueryParam("s") final String s) {
        return echoService.echo(s);
    }

    @GET
    @Path("path/{param}")
    public String getPath() {
        return request.getPath(true);
    }

    @GET
    @Path("mappers")
    public String getMappers() {
        return mappers.toString();
    }

    @GET
    @Path("requestCount")
    public String getStatisticsProperty() {
        return String.valueOf(stats.get().snapshot().getRequestStatistics().getTimeWindowStatistics().get(0L).getRequestCount());
    }

    @GET
    @Path("custom")
    public String getCustom() {
        return customInjected.getName();
    }

    @GET
    @Path("custom2")
    public String getCustom2() {
        return hk2Injected.getName();
    }
}
