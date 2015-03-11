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

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;

import javax.inject.Inject;
import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper.DeclaringClassInterfacePair;
import org.glassfish.jersey.internal.util.collection.KeyComparatorHashMap;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.spi.ContextResolvers;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.AbstractBinder;

/**
 * A factory implementation for managing {@link ContextResolver} instances.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ContextResolverFactory implements ContextResolvers {

    /**
     * Injection binder defining {@link ContextResolverFactory} and
     * {@link ContextResolvers} bindings.
     */
    public static class Binder extends AbstractBinder {

        @Override
        protected void configure() {
            bindAsContract(ContextResolverFactory.class).to(ContextResolvers.class).in(Singleton.class);
        }
    }

    private final Map<Type, Map<MediaType, ContextResolver>> resolver =
            new HashMap<Type, Map<MediaType, ContextResolver>>(3);
    private final Map<Type, ConcurrentHashMap<MediaType, ContextResolver>> cache =
            new HashMap<Type, ConcurrentHashMap<MediaType, ContextResolver>>(3);

    /**
     * Create new context resolver factory backed by the supplied {@link ServiceLocator HK2 service locator}.
     *
     * @param locator HK2 service locator.
     */
    @Inject
    public ContextResolverFactory(final ServiceLocator locator) {
        final Map<Type, Map<MediaType, List<ContextResolver>>> rs =
                new HashMap<Type, Map<MediaType, List<ContextResolver>>>();

        final Iterable<ContextResolver> providers = Providers.getAllProviders(locator, ContextResolver.class);
        for (final ContextResolver provider : providers) {
            final List<MediaType> ms = MediaTypes.createFrom(provider.getClass().getAnnotation(Produces.class));

            final Type type = getParameterizedType(provider.getClass());

            Map<MediaType, List<ContextResolver>> mr = rs.get(type);
            if (mr == null) {
                mr = new HashMap<MediaType, List<ContextResolver>>();
                rs.put(type, mr);
            }
            for (final MediaType m : ms) {
                List<ContextResolver> crl = mr.get(m);
                if (crl == null) {
                    crl = new ArrayList<ContextResolver>();
                    mr.put(m, crl);
                }
                crl.add(provider);
            }
        }

        // Reduce set of two or more context resolvers for same type and
        // media type

        for (final Map.Entry<Type, Map<MediaType, List<ContextResolver>>> e : rs.entrySet()) {
            final Map<MediaType, ContextResolver> mr = new KeyComparatorHashMap<MediaType, ContextResolver>(
                    4, MessageBodyFactory.MEDIA_TYPE_KEY_COMPARATOR);
            resolver.put(e.getKey(), mr);

            cache.put(e.getKey(), new ConcurrentHashMap<MediaType, ContextResolver>(4));

            for (final Map.Entry<MediaType, List<ContextResolver>> f : e.getValue().entrySet()) {
                mr.put(f.getKey(), reduce(f.getValue()));
            }
        }
    }

    private Type getParameterizedType(final Class<?> c) {
        final DeclaringClassInterfacePair p = ReflectionHelper.getClass(
                c, ContextResolver.class);

        final Type[] as = ReflectionHelper.getParameterizedTypeArguments(p);

        return (as != null) ? as[0] : Object.class;
    }

    private static final NullContextResolverAdapter NULL_CONTEXT_RESOLVER =
            new NullContextResolverAdapter();

    private static final class NullContextResolverAdapter implements ContextResolver {

        @Override
        public Object getContext(final Class type) {
            throw new UnsupportedOperationException("Not supported yet.");
        }
    }

    private static final class ContextResolverAdapter implements ContextResolver {

        private final ContextResolver[] cra;

        ContextResolverAdapter(final ContextResolver... cra) {
            this(removeNull(cra));
        }

        ContextResolverAdapter(final List<ContextResolver> crl) {
            this.cra = crl.toArray(new ContextResolver[crl.size()]);
        }

        @Override
        public Object getContext(final Class objectType) {
            for (final ContextResolver cr : cra) {
                @SuppressWarnings("unchecked") final Object c = cr.getContext(objectType);
                if (c != null) {
                    return c;
                }
            }
            return null;
        }

        ContextResolver reduce() {
            if (cra.length == 0) {
                return NULL_CONTEXT_RESOLVER;
            }
            if (cra.length == 1) {
                return cra[0];
            } else {
                return this;
            }
        }

        private static List<ContextResolver> removeNull(final ContextResolver... cra) {
            final List<ContextResolver> crl = new ArrayList<ContextResolver>(cra.length);
            for (final ContextResolver cr : cra) {
                if (cr != null) {
                    crl.add(cr);
                }
            }
            return crl;
        }
    }

    private ContextResolver reduce(final List<ContextResolver> r) {
        if (r.size() == 1) {
            return r.iterator().next();
        } else {
            return new ContextResolverAdapter(r);
        }
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> ContextResolver<T> resolve(final Type t, MediaType m) {
        final ConcurrentHashMap<MediaType, ContextResolver> crMapCache = cache.get(t);
        if (crMapCache == null) {
            return null;
        }

        if (m == null) {
            m = MediaType.WILDCARD_TYPE;
        }

        ContextResolver<T> cr = crMapCache.get(m);
        if (cr == null) {
            final Map<MediaType, ContextResolver> crMap = resolver.get(t);

            if (m.isWildcardType()) {
                cr = crMap.get(MediaType.WILDCARD_TYPE);
                if (cr == null) {
                    cr = NULL_CONTEXT_RESOLVER;
                }
            } else if (m.isWildcardSubtype()) {
                // Include x, x/* and */*
                final ContextResolver<T> subTypeWildCard = crMap.get(m);
                final ContextResolver<T> wildCard = crMap.get(MediaType.WILDCARD_TYPE);

                cr = new ContextResolverAdapter(subTypeWildCard, wildCard).reduce();
            } else {
                // Include x, x/* and */*
                final ContextResolver<T> type = crMap.get(m);
                final ContextResolver<T> subTypeWildCard = crMap.get(new MediaType(m.getType(), "*"));
                final ContextResolver<T> wildCard = crMap.get(MediaType.WILDCARD_TYPE);

                cr = new ContextResolverAdapter(type, subTypeWildCard, wildCard).reduce();
            }

            final ContextResolver<T> _cr = crMapCache.putIfAbsent(m, cr);
            // If there is already a value in the cache use that
            // instance, and discard the new and redundant instance, to
            // ensure the same instance is always returned.
            // The cached instance and the new instance will have the same
            // functionality.
            if (_cr != null) {
                cr = _cr;
            }
        }

        return (cr != NULL_CONTEXT_RESOLVER) ? cr : null;
    }
}
