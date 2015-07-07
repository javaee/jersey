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
package org.glassfish.jersey.internal;

import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.Set;
import java.util.WeakHashMap;

import javax.ws.rs.core.CacheControl;
import javax.ws.rs.core.Cookie;
import javax.ws.rs.core.EntityTag;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response.ResponseBuilder;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.ext.RuntimeDelegate;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.message.internal.JerseyLink;
import org.glassfish.jersey.message.internal.OutboundJaxrsResponse;
import org.glassfish.jersey.message.internal.OutboundMessageContext;
import org.glassfish.jersey.message.internal.VariantListBuilder;
import org.glassfish.jersey.spi.HeaderDelegateProvider;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * An abstract implementation of {@link RuntimeDelegate} that
 * provides support common to the client and server.
 *
 * @author Paul Sandoz
 */
public abstract class AbstractRuntimeDelegate extends RuntimeDelegate {

    private final Set<HeaderDelegateProvider> hps;
    private final Map<Class<?>, HeaderDelegate<?>> map;

    /**
     * Initialization constructor. The service locator will be shut down.
     *
     * @param serviceLocator HK2 service locator.
     */
    protected AbstractRuntimeDelegate(final ServiceLocator serviceLocator) {
        try {
            hps = Providers.getProviders(serviceLocator, HeaderDelegateProvider.class);

            /**
             * Construct a map for quick look up of known header classes
             */
            map = new WeakHashMap<Class<?>, HeaderDelegate<?>>();
            map.put(EntityTag.class, _createHeaderDelegate(EntityTag.class));
            map.put(MediaType.class, _createHeaderDelegate(MediaType.class));
            map.put(CacheControl.class, _createHeaderDelegate(CacheControl.class));
            map.put(NewCookie.class, _createHeaderDelegate(NewCookie.class));
            map.put(Cookie.class, _createHeaderDelegate(Cookie.class));
            map.put(URI.class, _createHeaderDelegate(URI.class));
            map.put(Date.class, _createHeaderDelegate(Date.class));
            map.put(String.class, _createHeaderDelegate(String.class));
        } finally {
            serviceLocator.shutdown();
        }
    }

    @Override
    public javax.ws.rs.core.Variant.VariantListBuilder createVariantListBuilder() {
        return new VariantListBuilder();
    }

    @Override
    public ResponseBuilder createResponseBuilder() {
        return new OutboundJaxrsResponse.Builder(new OutboundMessageContext());
    }

    @Override
    public UriBuilder createUriBuilder() {
        return new JerseyUriBuilder();
    }

    @Override
    public Link.Builder createLinkBuilder() {
        return new JerseyLink.Builder();
    }

    @Override
    public <T> HeaderDelegate<T> createHeaderDelegate(final Class<T> type) {
        if (type == null) {
            throw new IllegalArgumentException("type parameter cannot be null");
        }

        @SuppressWarnings("unchecked") final HeaderDelegate<T> delegate = (HeaderDelegate<T>) map.get(type);
        if (delegate != null) {
            return delegate;
        }

        return _createHeaderDelegate(type);
    }

    @SuppressWarnings("unchecked")
    private <T> HeaderDelegate<T> _createHeaderDelegate(final Class<T> type) {
        for (final HeaderDelegateProvider hp : hps) {
            if (hp.supports(type)) {
                return hp;
            }
        }

        return null;
    }
}
