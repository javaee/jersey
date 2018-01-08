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
package org.glassfish.jersey.examples.opentracing;

import java.util.concurrent.Executors;

import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.container.AsyncResponse;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.Suspended;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.opentracing.OpenTracingFeature;
import org.glassfish.jersey.opentracing.OpenTracingUtils;
import org.glassfish.jersey.server.ManagedAsync;
import org.glassfish.jersey.server.Uri;

import io.opentracing.Span;


/**
 * OpenTracing example resource.
 * <p>
 * Jersey (with registered {@link OpenTracingFeature} will automatically
 * create and start span for each request ("root" span or "request" span) and a child span to be used in the resource method
 * ("resource" span). The root span is used for Jersey-level event logging (resource matching started, request filters applied,
 * etc). The resource span serves for application-level event logging purposes (used-defined). Both are automatically created
 * and also automatically finished.
 * <p>
 * Resource span is created right before the resource method invocation and finished right after resource method finishes. It
 * can be resolved by calling {@link OpenTracingUtils#getRequestSpan(ContainerRequestContext)}.
 * <p>
 * Application code can also create ad-hoc spans as child spans of the resource span. This can be achieved by calling one of the
 * convenience methods {@link OpenTracingUtils#getRequestChildSpan(ContainerRequestContext)}.
 * <p>
 * {@link ContainerRequestContext} can be obtained via injection.
 * <p>
 * All the ad-hoc created spans MUST be {@link Span#finish() finished} explicitly.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
@Path(value = "/resource")
public class TracedResource {

    /**
     * Resource method with no explicit tracing.
     * <p>
     * One span (jersey-server) will be created and finished automatically.
     *
     * @return dummy response
     */
    @GET
    @Path("defaultTrace")
    public Response defaultTrace() {
        return Response.ok("foo").build();
    }

    /**
     * Resource method with explicit logging into resource span.
     *
     * @param context injected request context with resource-level span reference
     * @return dummy response
     * @throws InterruptedException if interrupted
     */
    @GET
    @Path("appLevelLogging")
    public Response appLevelLogging(@Context ContainerRequestContext context) throws InterruptedException {
        final Span resourceSpan = OpenTracingUtils
                .getRequestSpan(context)
                .orElseThrow(() -> new RuntimeException("Tracing has failed"));

        resourceSpan.log("Starting expensive operation.");
        // Do the business
        Thread.sleep(200);
        resourceSpan.log("Expensive operation finished.");
        resourceSpan.setTag("expensiveOperationSuccess", true);

        return Response.ok("SUCCESS").build();
    }

    /**
     * Similar as {@link #appLevelLogging(ContainerRequestContext)}, just with {@code POST} method.
     *
     * @param entity  posted entity
     * @param context injected context
     * @return dummy response
     */
    @POST
    @Path("appLevelPost")
    public Response tracePost(String entity, @Context ContainerRequestContext context) {
        final Span resourceSpan = OpenTracingUtils
                .getRequestSpan(context)
                .orElseThrow(() -> new RuntimeException("Tracing has failed"));

        resourceSpan.setTag("result", "42");
        resourceSpan.setBaggageItem("entity", entity);
        return Response.ok("Done!").build();
    }

    /**
     * Resource method with explicit child span creation.
     *
     * @param context injected request context with resource-level (parent) span reference
     * @return dummy response
     * @throws InterruptedException if interrupted
     */
    @GET
    @Path("childSpan")
    public Response childSpan(@Context ContainerRequestContext context) throws InterruptedException {
        final Span childSpan = OpenTracingUtils.getRequestChildSpan(context, "AppCreatedSpan");
        childSpan.log("Starting expensive operation.");
        // Do the business
        Thread.sleep(200);
        childSpan.log("Expensive operation finished.");
        childSpan.setTag("expensiveOperationSuccess", true);

        childSpan.finish();
        return Response.ok("SUCCESS").build();
    }


    /**
     * Resource method with explicit span creation and propagation into injected managed client.
     * <p>
     * Shows how to propagate the server-side span into managed client (or any common Jersey client).
     * This way, the client span will be child of the resource span.
     *
     * @param context injected context
     * @param wt      injected web target
     * @return dummy response
     */
    @GET
    @Path("managedClient")
    public Response traceWithManagedClient(@Context ContainerRequestContext context,
                                           @Uri("resource/appLevelPost") WebTarget wt) {
        final Span providedSpan = OpenTracingUtils
                .getRequestSpan(context)
                .orElseThrow(() -> new RuntimeException("Tracing failed"));

        providedSpan.log("Resource method started.");

        final Response response = wt.request()
                .property(OpenTracingFeature.SPAN_CONTEXT_PROPERTY, providedSpan.context())  // <--- span propagation
                .post(Entity.text("Hello"));

        providedSpan.log("1st Response received from managed client");
        providedSpan.log("Firing 1st request from managed client");

        providedSpan.log("Creating child span");
        final Span childSpan = OpenTracingUtils.getRequestChildSpan(context, "jersey-resource-child-span");


        childSpan.log("Firing 2nd request from managed client");
        final Response response2 = wt.request()
                .property(OpenTracingFeature.SPAN_CONTEXT_PROPERTY, childSpan.context())  // <--- span propagation
                .post(Entity.text("World"));
        childSpan.log("2st Response received from managed client");

        childSpan.finish();
        return Response.ok("Result: " + response.getStatus() + ", " + response2.getStatus()).build();
    }

    @GET
    @Path("async")
    @ManagedAsync
    public void traceWithAsync(@Suspended final AsyncResponse asyncResponse, @Context ContainerRequestContext context) {
        final Span span = OpenTracingUtils.getRequestSpan(context).orElseThrow(() -> new RuntimeException("tracing failed"));
        span.log("In the resource method.");
        Executors.newSingleThreadExecutor().submit(() -> {
            try {
                Thread.sleep(200);
            } catch (InterruptedException e) {
                span.log("Interrupted");
                e.printStackTrace();
            }
            span.log("Resuming");
            asyncResponse.resume("OK");
        });
        span.log("Before exiting the resource method");
    }

    @GET
    @Path("error")
    public String failTrace(@Context ContainerRequestContext context) {
        throw new RuntimeException("Failing just for fun.");
    }
}