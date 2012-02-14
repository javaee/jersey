/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.routing;

import java.net.URI;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;
import java.util.regex.Pattern;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.internal.ServiceProvidersModule;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.FilterModule;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.ServerModule;
import org.glassfish.jersey.server.internal.routing.RouterModule.RootRouteBuilder;
import org.glassfish.jersey.server.testutil.AcceptorRootModule;
import org.glassfish.jersey.spi.ExceptionMappers;

import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.Parameterized;
import static org.junit.Assert.assertEquals;

/**
 * Tests synchronous application implementation
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@RunWith(Parameterized.class)
public class PatternRoutingTest {

    private static final URI BASE_URI = URI.create("http://localhost:8080/base/");

    @Parameterized.Parameters
    public static List<String[]> testUriSuffixes() {
        return Arrays.asList(new String[][]{
                    {"a/b/c", "B-c-b-a"},
                    {"a/b/c/", "B-c-b-a"},
                    {"a/d/e", "B-e-d-a"},
                    {"a/d/e/", "B-e-d-a"}
                });
    }
    @Inject
    private RootRouteBuilder<Pattern> routeBuilder;
    private RequestInvoker invoker; // will be manually injected in the setupApplication()
    private RequestScope requestScope; // will be manually injected in the setupApplication()
    private final String uriSuffix;
    private final String expectedResponse;

    public PatternRoutingTest(String uriSuffix, String expectedResponse) {
        this.uriSuffix = uriSuffix;
        this.expectedResponse = expectedResponse;
    }

    @Before
    public void setupApplication() {
        final AcceptorRootModule appRootModule = new AcceptorRootModule();
        Services services = HK2.get().create(null,
                new ServerModule(),
                appRootModule);

        final Ref<ServiceProviders> providers = services.forContract(new TypeLiteral<Ref<ServiceProviders>>(){}).get();
        providers.set(services.forContract(ServiceProviders.Builder.class).get().build());
        final Ref<MessageBodyWorkers> workers = services.forContract(new TypeLiteral<Ref<MessageBodyWorkers>>(){}).get();
        workers.set(new MessageBodyFactory(providers.get()));
        final Ref<ExceptionMappers> mappers = services.forContract(new TypeLiteral<Ref<ExceptionMappers>>(){}).get();
        mappers.set(new ExceptionMapperFactory(providers.get()));

        Injector injector = services.forContract(Injector.class).get();
        injector.inject(this);

        TreeAcceptor inflection = Stages.asTreeAcceptor(new Inflector<Request, Response>() {

            @Override
            public Response apply(final Request req) {
                // System.out.println("Inflector");
                return Responses.from(200, req).entity("B").build();
            }
        });

        appRootModule.setRoot(routeBuilder.root(
                routeBuilder.route("a(/.*)?").to(LastPathSegmentTracingFilter.class)
                    .to(routeBuilder.route("b(/.*)?").to(LastPathSegmentTracingFilter.class)
                        .to(routeBuilder.route("c(/)?").to(LastPathSegmentTracingFilter.class).to(inflection)))
                    .to(routeBuilder.route("d(/.*)?").to(LastPathSegmentTracingFilter.class)
                        .to(routeBuilder.route("e(/)?").to(LastPathSegmentTracingFilter.class).to(inflection)))
                .build()));

        invoker = injector.inject(RequestInvoker.class);
        requestScope = injector.inject(RequestScope.class);
    }

    @Test
    public void testPatternRouting() throws InterruptedException, ExecutionException {
        Request req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath() + uriSuffix), "GET").build();
        requestScope.enter();
        Future<Response> res = invoker.apply(req);
        requestScope.exit();

        assertEquals(expectedResponse, res.get().getEntity());
    }
}
