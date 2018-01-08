/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.spring.test;

import java.math.BigDecimal;

import javax.ws.rs.Consumes;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.MediaType;

import javax.inject.Inject;
import javax.inject.Named;
import javax.servlet.http.HttpServletRequest;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

/**
 * Jersey managed JAX-RS resource for testing jersey-spring.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
@Path("/jersey/account")
public class AccountJerseyResource {

    @Inject
    @Named("AccountService-singleton")
    private AccountService accountServiceInject;

    @Autowired
    @Qualifier("AccountService-singleton")
    private AccountService accountServiceAutowired;

    @Inject
    @Named("AccountService-request-1")
    private AccountService accountServiceRequest1;

    @Autowired
    @Qualifier("AccountService-request-1")
    private AccountService accountServiceRequest2;

    @Autowired
    @Qualifier("AccountService-prototype-1")
    private AccountService accountServicePrototype1;

    @Autowired
    @Qualifier("AccountService-prototype-1")
    private AccountService accountServicePrototype2;

    @Autowired
    private HttpServletRequest httpServletRequest;

    @Inject
    private HK2ServiceSingleton hk2Singleton;

    @Inject
    private HK2ServiceRequestScoped hk2RequestScoped;

    @Inject
    private HK2ServicePerLookup hk2PerLookup;

    private String message = "n/a";

    // resource methods for testing resource class scope
    @GET
    @Path("message")
    public String getMessage() {
        return message;
    }

    @PUT
    @Path("message")
    @Consumes(MediaType.TEXT_PLAIN)
    public String setMessage(final String message) {
        this.message = message;
        return message;
    }

    // JERSEY-2506 FIX VERIFICATION
    @GET
    @Path("server")
    public String verifyServletRequestInjection() {
        return "PASSED: " + httpServletRequest.getServerName();
    }

    @GET
    @Path("singleton/server")
    public String verifyServletRequestInjectionIntoSingleton() {
        return accountServiceInject.verifyServletRequestInjection();
    }

    @GET
    @Path("singleton/autowired/server")
    public String verifyServletRequestInjectionIntoAutowiredSingleton() {
        return accountServiceAutowired.verifyServletRequestInjection();
    }

    @GET
    @Path("request/server")
    public String verifyServletRequestInjectionIntoRequestScopedBean() {
        return accountServiceRequest1.verifyServletRequestInjection();
    }

    @GET
    @Path("prototype/server")
    public String verifyServletRequestInjectionIntoPrototypeScopedBean() {
        return accountServicePrototype1.verifyServletRequestInjection();
    }

    // resource methods for testing singleton scoped beans
    @GET
    @Path("singleton/inject/{accountId}")
    public BigDecimal getAccountBalanceSingletonInject(@PathParam("accountId") final String accountId) {
        return accountServiceInject.getAccountBalance(accountId);
    }

    @GET
    @Path("singleton/autowired/{accountId}")
    public BigDecimal getAccountBalanceSingletonAutowired(@PathParam("accountId") final String accountId) {
        return accountServiceAutowired.getAccountBalance(accountId);
    }

    @PUT
    @Path("singleton/{accountId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public void setAccountBalanceSingleton(@PathParam("accountId") final String accountId, final String balance) {
        accountServiceInject.setAccountBalance(accountId, new BigDecimal(balance));
    }

    // resource methods for testing request scoped beans
    @PUT
    @Path("request/{accountId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public BigDecimal setAccountBalanceRequest(@PathParam("accountId") final String accountId, final String balance) {
        accountServiceRequest1.setAccountBalance(accountId, new BigDecimal(balance));
        return accountServiceRequest2.getAccountBalance(accountId);
    }

    // resource methods for testing prototype scoped beans
    @PUT
    @Path("prototype/{accountId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public BigDecimal setAccountBalancePrototype(@PathParam("accountId") final String accountId, final String balance) {
        accountServicePrototype1.setAccountBalance(accountId, new BigDecimal(balance));
        return accountServicePrototype2.getAccountBalance(accountId);
    }
}
