/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal;

import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.Variant;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.message.internal.MessagingBinders;

import org.junit.Assert;

/**
 * Test runtime delegate.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class TestRuntimeDelegate extends AbstractRuntimeDelegate {

    public TestRuntimeDelegate() {
        super(Injections.createLocator(new MessagingBinders.HeaderDelegateProviders()));
    }

    @Override
    public <T> T createEndpoint(Application application, Class<T> endpointType)
            throws IllegalArgumentException, UnsupportedOperationException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    public void testMediaType() {
        MediaType m = new MediaType("text", "plain");
        Assert.assertNotNull(m);
    }

    public void testUriBuilder() {
        UriBuilder ub = RuntimeDelegate.getInstance().createUriBuilder();
        Assert.assertNotNull(ub);
    }

    public void testResponseBuilder() {
        Response.ResponseBuilder rb = RuntimeDelegate.getInstance().createResponseBuilder();
        Assert.assertNotNull(rb);
    }

    public void testVariantListBuilder() {
        Variant.VariantListBuilder vlb = RuntimeDelegate.getInstance().createVariantListBuilder();
        Assert.assertNotNull(vlb);
    }

    public void testLinkBuilder() {
        final Link.Builder linkBuilder = RuntimeDelegate.getInstance().createLinkBuilder();
        Assert.assertNotNull(linkBuilder);
    }

    public void testWebApplicationException() {
        WebApplicationException wae = new WebApplicationException();
        Assert.assertNotNull(wae);
    }
}
