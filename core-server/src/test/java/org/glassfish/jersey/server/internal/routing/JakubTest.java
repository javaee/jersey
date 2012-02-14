/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.concurrent.ExecutionException;
import java.util.concurrent.Future;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.ExceptionMapperFactory;
import org.glassfish.jersey.internal.ServiceProviders;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.process.Inflector;
import org.glassfish.jersey.process.internal.RequestInvoker;
import org.glassfish.jersey.process.internal.RequestScope;
import org.glassfish.jersey.process.internal.Stages;
import org.glassfish.jersey.process.internal.TreeAcceptor;
import org.glassfish.jersey.server.ServerModule;
import org.glassfish.jersey.server.internal.routing.RouterModule.RootRouteBuilder;
import org.glassfish.jersey.server.testutil.AcceptorRootModule;
import org.glassfish.jersey.spi.ExceptionMappers;
import org.glassfish.jersey.uri.PathPattern;

import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.inject.Injector;

import org.jvnet.hk2.annotations.Inject;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

public class JakubTest {

    private static final URI BASE_URI = URI.create("http://localhost:8080/base/");

    @Inject
    private RootRouteBuilder<PathPattern> routeBuilder;
    private RequestInvoker invoker; // will be manually injected in the setupApplication()
    private RequestScope requestScope; // will be manually injected in the setupApplication()

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

//        appRootModule.setRoot(
//                routeBuilder.route("{p1}").to(routeBuilder.route("parent").to(createInflector("child"))).build());
        appRootModule.setRoot(routeBuilder.root(
                routeBuilder.route("x/y")
                    .to(routeBuilder.route("b")
                        .to(routeBuilder.route(PathPattern.EMPTY_PATH).to(createInflector(injector, "B")))
                        .to(routeBuilder.route("a").to(routeBuilder.route(PathPattern.EMPTY_PATH).to(createInflector(injector, "A")))))
                    .to(routeBuilder.route("d")
                        .to(routeBuilder.route(PathPattern.EMPTY_PATH).to(createInflector(injector, "D"))))
                     .to(routeBuilder.route(PathPattern.EMPTY_PATH).to(createInflector(injector, "X")))
                .build()));

        this.invoker = injector.inject(RequestInvoker.class);
        this.requestScope = injector.inject(RequestScope.class);
    }

    private TreeAcceptor createInflector(final Injector injector , final String value) {
        return Stages.asTreeAcceptor(new Inflector<Request, Response>() {

            @Override
            public Response apply(final Request req) {
                return Responses.from(200, req).entity(value).build();
            }
        });
    }

    @Test
    public void testPathPatternRouting() throws InterruptedException, ExecutionException {
//        Request req = Requests.from("/parent", "GET").build();
//        Request req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath() + "x/d/y"), "GET").build();
        Request req;
        Future<Response> res;

        req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath() + "x/y"), "GET").build();
        requestScope.enter();
        res = invoker.apply(req);
        requestScope.exit();
        assertEquals("X", res.get().getEntity());

        req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath() + "x/y/d"), "GET").build();
        requestScope.enter();
        res = invoker.apply(req);
        requestScope.exit();
        assertEquals("D", res.get().getEntity());

        req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath() + "x/y/b"), "GET").build();
        requestScope.enter();
        res = invoker.apply(req);
        requestScope.exit();
        assertEquals("B", res.get().getEntity());

        req = Requests.from(BASE_URI, URI.create(BASE_URI.getPath() + "x/y/b/a"), "GET").build();
        requestScope.enter();
        res = invoker.apply(req);
        requestScope.exit();
        assertEquals("A", res.get().getEntity());
    }
}
