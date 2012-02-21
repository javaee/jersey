/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal.inject;

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.message.internal.Requests;
import org.glassfish.jersey.message.internal.Responses;
import org.glassfish.jersey.internal.util.collection.Ref;

import org.glassfish.hk2.Factory;
import org.glassfish.hk2.HK2;
import org.glassfish.hk2.Services;
import org.glassfish.hk2.TypeLiteral;
import org.glassfish.hk2.inject.Injector;
import org.glassfish.hk2.scopes.PerLookup;
import org.glassfish.hk2.scopes.Singleton;

import org.jvnet.hk2.annotations.Inject;

import org.junit.Test;
import static org.junit.Assert.*;

/**
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ReferencingFactoryTest extends AbstractModule {

    private static class ValueInjected {

        @Inject(optional = true)
        Request request;
        @Inject(optional = true)
        List<Integer> ints;
        @Inject
        Response response;
        @Inject
        List<String> strings;
    }

    private static class RefInjected {

        @Inject
        Ref<Request> request;
        @Inject
        Ref<List<Integer>> ints;
        @Inject
        Ref<Response> response;
        @Inject
        Ref<List<String>> strings;
    }

    public ReferencingFactoryTest() {
    }
    //
    private Request expectedReqest = null;
    private List<Integer> expectedInts = null;
    private Response expectedResponse = Responses.empty().build();
    private List<String> expectedStrings = new LinkedList<String>();

    private static final class RequestReferencingFactory extends ReferencingFactory<Request> {

        public RequestReferencingFactory(@Inject Factory<Ref<Request>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static final class ResponseReferencingFactory extends ReferencingFactory<Response> {

        public ResponseReferencingFactory(@Inject Factory<Ref<Response>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static final class ListOfIntegerReferencingFactory extends ReferencingFactory<List<Integer>> {

        public ListOfIntegerReferencingFactory(@Inject Factory<Ref<List<Integer>>> referenceFactory) {
            super(referenceFactory);
        }
    }

    private static final class ListOfStringReferencingFactory extends ReferencingFactory<List<String>> {

        public ListOfStringReferencingFactory(@Inject Factory<Ref<List<String>>> referenceFactory) {
            super(referenceFactory);
        }
    }

    @Override
    protected void configure() {
        bind(Request.class).toFactory(RequestReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<Request>>() {}).toFactory(ReferencingFactory.<Request>referenceFactory()).in(Singleton.class);

        bind(new TypeLiteral<List<Integer>>() {}).toFactory(ListOfIntegerReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<List<Integer>>>() {}).toFactory(ReferencingFactory.<List<Integer>>referenceFactory()).in(Singleton.class);

        bind(Response.class).toFactory(ResponseReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<Response>>() {}).toFactory(ReferencingFactory.<Response>referenceFactory(expectedResponse)).in(Singleton.class);

        bind(new TypeLiteral<List<String>>() {}).toFactory(ListOfStringReferencingFactory.class).in(PerLookup.class);
        bind(new TypeLiteral<Ref<List<String>>>() {}).toFactory(ReferencingFactory.<List<String>>referenceFactory(expectedStrings)).in(Singleton.class);
    }

    @Test
    public void testReferecedBinding() {
        Services services = HK2.get().create(null, this);
        Injector injector = services.forContract(Injector.class).get();

        ValueInjected emptyValues = injector.inject(ValueInjected.class);
        assertSame(expectedReqest, emptyValues.request);
        assertSame(expectedInts, emptyValues.ints);
        assertSame(expectedResponse, emptyValues.response);
        assertSame(expectedStrings, emptyValues.strings);

        RefInjected refValues = injector.inject(RefInjected.class);
        expectedReqest = Requests.from("http://jersey.java.net/", "GET").build();
        refValues.request.set(expectedReqest);
        expectedInts = new LinkedList<Integer>();
        refValues.ints.set(expectedInts);
        expectedResponse = Responses.from(200, expectedReqest).build();
        refValues.response.set(expectedResponse);
        expectedStrings = new ArrayList<String>();
        refValues.strings.set(expectedStrings);

        ValueInjected updatedValues = injector.inject(ValueInjected.class);
        assertSame(expectedReqest, updatedValues.request);
        assertSame(expectedInts, updatedValues.ints);
        assertSame(expectedResponse, updatedValues.response);
        assertSame(expectedStrings, updatedValues.strings);
    }
}

/*

==== Referencing factory ====
There are sometimes pieces of information that may be injectable, but the injected instance may vary even in the request scope. Request and Response instances are good examples of such behavior. Some dynamically bound (see next section) components may also fall into this category.

<code>ReferencingFactory</code> is a core-common injection utility class that helps to solve the problem. First, the instance of the referencing factory is created in the binding module:

    ReferencingFactory<Request> requestReferencingFactory = new ReferencingFactory<Request>();

Then binding has to be defined for the instance reference as well as for the referenced instance itself:

    bind(Request.class).toFactory(requestReferencingFactory);
    bind(new TypeLiteral<Ref<Request>>(){}).toFactory(requestReferencingFactory.referenceFactory()).in(RequestScope.class);

Note that the <code>Request</code> is not bound to the

 */