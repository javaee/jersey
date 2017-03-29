/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

import org.glassfish.jersey.internal.inject.Bindings;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.InstanceBinding;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper.DeclaringClassInterfacePair;
import org.glassfish.jersey.internal.util.collection.KeyComparatorHashMap;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.message.internal.MessageBodyFactory;
import org.glassfish.jersey.spi.ContextResolvers;

/**
 * A factory implementation for managing {@link ContextResolver} instances.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ContextResolverFactory implements ContextResolvers {

    /**
     * Configurator which initializes and register {@link ContextResolvers} instance into {@link InjectionManager} and
     * {@link BootstrapBag}.
     *
     * @author Petr Bouda (petr.bouda at oracle.com)
     */
    public static class ContextResolversConfigurator implements BootstrapConfigurator {

        private ContextResolverFactory contextResolverFactory;

        @Override
        public void init(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
            contextResolverFactory = new ContextResolverFactory();
            InstanceBinding<ContextResolverFactory> binding =
                    Bindings.service(contextResolverFactory)
                            .to(ContextResolvers.class);
            injectionManager.register(binding);
        }

        @Override
        public void postInit(InjectionManager injectionManager, BootstrapBag bootstrapBag) {
            contextResolverFactory.initialize(injectionManager.getAllInstances(ContextResolver.class));
            bootstrapBag.setContextResolvers(contextResolverFactory);
        }
    }

    private final Map<Type, Map<MediaType, ContextResolver>> resolver = new HashMap<>(3);
    private final Map<Type, ConcurrentHashMap<MediaType, ContextResolver>> cache = new HashMap<>(3);

    /**
     * Private constructor to allow to create {@link ContextResolverFactory} only in {@link ContextResolversConfigurator}.
     */
    private ContextResolverFactory(){
    }

    private void initialize(List<ContextResolver> contextResolvers) {
        Map<Type, Map<MediaType, List<ContextResolver>>> rs = new HashMap<>();

        for (ContextResolver provider : contextResolvers) {
            List<MediaType> ms = MediaTypes.createFrom(provider.getClass().getAnnotation(Produces.class));
            Type type = getParameterizedType(provider.getClass());

            Map<MediaType, List<ContextResolver>> mr = rs.get(type);
            if (mr == null) {
                mr = new HashMap<>();
                rs.put(type, mr);
            }
            for (MediaType m : ms) {
                List<ContextResolver> crl = mr.get(m);
                if (crl == null) {
                    crl = new ArrayList<>();
                    mr.put(m, crl);
                }
                crl.add(provider);
            }
        }

        // Reduce set of two or more context resolvers for same type and
        // media type

        for (Map.Entry<Type, Map<MediaType, List<ContextResolver>>> e : rs.entrySet()) {
            Map<MediaType, ContextResolver> mr = new KeyComparatorHashMap<>(4, MessageBodyFactory.MEDIA_TYPE_KEY_COMPARATOR);
            resolver.put(e.getKey(), mr);
            cache.put(e.getKey(), new ConcurrentHashMap<>(4));

            for (Map.Entry<MediaType, List<ContextResolver>> f : e.getValue().entrySet()) {
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
            final List<ContextResolver> crl = new ArrayList<>(cra.length);
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
