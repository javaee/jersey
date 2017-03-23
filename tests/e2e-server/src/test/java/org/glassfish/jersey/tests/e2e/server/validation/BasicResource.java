/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.e2e.server.validation;

import javax.ws.rs.Consumes;
import javax.ws.rs.CookieParam;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.FormParam;
import javax.ws.rs.GET;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.container.ResourceContext;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import javax.validation.Valid;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Null;
import javax.validation.constraints.Size;

/**
 * @author Michal Gajdos
 */
@Path("beanvalidation")
public class BasicResource {

    @NotNull
    @Context
    private ResourceContext resourceContext;

    private String getInputParamsResponse(final String path,
                                          final String matrix,
                                          final String query,
                                          final String header,
                                          final String cookie,
                                          final String form) {

        return path + '_' + matrix + '_' + query + '_' + header + '_' + cookie + '_' + form;
    }

    @POST
    @Path("basicParam/{path: .*}")
    @Consumes("application/x-www-form-urlencoded")
    @SuppressWarnings("UnusedParameters")
    public String validateParametersBasicConstraint(
            @NotNull @PathParam("path") final String path,
            @NotNull @MatrixParam("matrix") final String matrix,
            @NotNull @QueryParam("query") final String query,
            @NotNull @HeaderParam("header") final String header,
            @NotNull @CookieParam("cookie") final String cookie,
            @NotNull @FormParam("form") final String form,
            @NotNull @Context final Request request) {

        return getInputParamsResponse(path, matrix, query, header, cookie, form);
    }

    @POST
    @Path("basicDefaultParam/{path: .*}")
    @Consumes("application/x-www-form-urlencoded")
    @SuppressWarnings("UnusedParameters")
    public String validateParametersDefaultBasicConstraint(
            @NotNull @DefaultValue("pathParam") @PathParam("path") final String path,
            @NotNull @DefaultValue("matrixParam") @MatrixParam("matrix") final String matrix,
            @NotNull @DefaultValue("queryParam") @QueryParam("query") final String query,
            @NotNull @DefaultValue("headerParam") @HeaderParam("header") final String header,
            @NotNull @DefaultValue("cookieParam") @CookieParam("cookie") final String cookie,
            @NotNull @DefaultValue("formParam") @FormParam("form") final String form,
            @NotNull @Context final Request request) {

        return getInputParamsResponse(path, matrix, query, header, cookie, form);
    }

    @POST
    @Path("customParam/{path: .*}")
    @Consumes("application/x-www-form-urlencoded")
    public String validateParametersCustomConstraint(
            @ParamConstraint @PathParam("path") final String path,
            @ParamConstraint @MatrixParam("matrix") final String matrix,
            @ParamConstraint @QueryParam("query") final String query,
            @ParamConstraint @HeaderParam("header") final String header,
            @ParamConstraint @CookieParam("cookie") final String cookie,
            @ParamConstraint @FormParam("form") final String form) {

        return getInputParamsResponse(path, matrix, query, header, cookie, form);
    }

    @POST
    @Path("mixedParam/{path: .*}")
    @Consumes("application/x-www-form-urlencoded")
    public String validateParametersMixedConstraint(
            @Size(max = 11) @ParamConstraint @PathParam("path") final String path,
            @Size(max = 11) @ParamConstraint @MatrixParam("matrix") final String matrix,
            @Size(max = 11) @ParamConstraint @QueryParam("query") final String query,
            @Size(max = 11) @ParamConstraint @HeaderParam("header") final String header,
            @Size(max = 11) @ParamConstraint @CookieParam("cookie") final String cookie,
            @Size(max = 11) @ParamConstraint @FormParam("form") final String form) {

        return getInputParamsResponse(path, matrix, query, header, cookie, form);
    }

    @POST
    @Path("multipleParam/{path: .*}")
    @Consumes("application/x-www-form-urlencoded")
    public String validateParametersMultipleConstraint(
            @MultipleParamConstraint @PathParam("path") final String path,
            @MultipleParamConstraint @MatrixParam("matrix") final String matrix,
            @MultipleParamConstraint @QueryParam("query") final String query,
            @MultipleParamConstraint @HeaderParam("header") final String header,
            @MultipleParamConstraint @CookieParam("cookie") final String cookie,
            @MultipleParamConstraint @FormParam("form") final String form) {

        return getInputParamsResponse(path, matrix, query, header, cookie, form);
    }

    @POST
    @Path("emptyBeanParam")
    @Consumes("application/contactBean")
    @Produces("application/contactBean")
    public ContactBean validateEmptyBeanParamConstraint(@NotNull final ContactBean bean) {
        return bean;
    }

    @POST
    @Path("validBeanParam")
    @Consumes("application/contactBean")
    @Produces("application/contactBean")
    public ContactBean validateValidBeanParamConstraint(@NotNull @Valid final ContactBean bean) {
        return bean;
    }

    @POST
    @Path("customBeanParam")
    @Consumes("application/contactBean")
    @Produces("application/contactBean")
    public ContactBean validateCustomBeanParamConstraint(@OneContact final ContactBean bean) {
        return bean;
    }

    @POST
    @Path("emptyBeanResponse")
    @Consumes("application/contactBean")
    @Produces("application/contactBean")
    @NotNull
    public ContactBean validateEmptyBeanResponseConstraint(final ContactBean bean) {
        return bean;
    }

    @POST
    @Path("validBeanResponse")
    @Consumes("application/contactBean")
    @Produces("application/contactBean")
    @NotNull
    @Valid
    public ContactBean validateValidBeanResponseConstraint(final ContactBean bean) {
        return bean;
    }

    @POST
    @Path("validBeanWrappedInResponse")
    @Consumes("application/contactBean")
    @Produces("application/contactBean")
    @NotNull
    @Valid
    public Response validateValidBeanWrappedInResponseConstraint(final ContactBean bean) {
        return Response.ok(bean).type("application/contactBean").build();
    }

    @POST
    @Path("customBeanResponse")
    @Consumes("application/contactBean")
    @Produces("application/contactBean")
    @OneContact
    public ContactBean validateCustomBeanResponseConstraint(final ContactBean bean) {
        return bean;
    }

    @GET
    @Path("invalidContext")
    @SuppressWarnings("UnusedParameters")
    public Response invalidContext(@Null @Context final Request request) {
        return Response.status(500).build();
    }

    @Path("sub/validResourceContextInstance")
    public BasicSubResource getSubResourceValidResourceContextInstance() {
        return resourceContext.initResource(new BasicSubResource(resourceContext));
    }

    @Path("sub/nullResourceContextInstance")
    public BasicSubResource getSubResourceNullResourceContextInstance() {
        return resourceContext.initResource(new BasicSubResource(null));
    }

    @Path("sub/nullResourceContextClass")
    public BasicSubResource getSubResourceNullResourceContextClass() {
        return resourceContext.getResource(BasicSubResource.class);
    }

    @Path("sub/wrong")
    public BasicBadSubResource getWrongSubResource() {
        return resourceContext.getResource(BasicBadSubResource.class);
    }
}
