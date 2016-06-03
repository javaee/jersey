/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.inject;

import java.lang.annotation.Annotation;
import java.security.AccessController;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;

import javax.inject.Inject;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.util.Producer;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.server.ClientBinding;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.Uri;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.uri.internal.JerseyUriBuilder;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.base.Predicate;
import jersey.repackaged.com.google.common.collect.Collections2;
import jersey.repackaged.com.google.common.collect.Maps;

/**
 * Value factory provider supporting the {@link Uri} injection annotation.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
final class WebTargetValueFactoryProvider extends AbstractValueFactoryProvider {

    private final Configuration serverConfig;
    private final ConcurrentMap<BindingModel, Value<ManagedClient>> managedClients;

    private static class ManagedClient {

        private final Client instance;
        private final String customBaseUri;

        private ManagedClient(Client instance, String customBaseUri) {
            this.instance = instance;
            this.customBaseUri = customBaseUri;
        }
    }

    private static class BindingModel {

        public static final BindingModel EMPTY = new BindingModel(null);

        /**
         * Create a client binding model from a {@link ClientBinding client binding} annotation.
         *
         * @param binding client binding annotation.
         * @return binding model representing a single client binding annotation.
         */
        public static BindingModel create(Annotation binding) {
            if (binding == null || binding.annotationType().getAnnotation(ClientBinding.class) == null) {
                return EMPTY;
            } else {
                return new BindingModel(binding);
            }
        }

        /**
         * Create a client binding model from a set of {@link ClientBinding client binding}
         * annotation candidates.
         * <p>
         * A {@code ClientBinding} marker meta-annotation is used to select the set of binding
         * annotations. Only those annotations that are annotated with the binding marker
         * meta-annotation are considered as binding annotations. All other annotations are filtered
         * out and ignored.
         * </p>
         *
         * @param bindingCandidates candidate binding annotations.
         * @return composite binding representing the union of the individual binding annotations
         *         found among the binding candidates.
         */
        public static BindingModel create(final Collection<Annotation> bindingCandidates) {
            final Collection<Annotation> filtered =
                    Collections2.filter(bindingCandidates, new Predicate<Annotation>() {
                        @Override
                        public boolean apply(Annotation input) {
                            return input != null && input.annotationType().getAnnotation(ClientBinding.class) != null;
                        }
                    });

            if (filtered.isEmpty()) {
                return EMPTY;
            } else if (filtered.size() > 1) {
                throw new ProcessingException("Too many client binding annotations.");
            } else {
                return new BindingModel(filtered.iterator().next());
            }
        }

        private final Annotation annotation;
        private final Class<? extends Configuration> configClass;
        private final boolean inheritProviders;
        private final String baseUri;

        private BindingModel(Annotation annotation) {
            if (annotation == null) {
                this.annotation = null;
                this.configClass = ClientConfig.class;
                this.inheritProviders = true;
                this.baseUri = "";
            } else {
                this.annotation = annotation;
                final ClientBinding cba = annotation.annotationType().getAnnotation(ClientBinding.class);
                this.configClass = cba.configClass();
                this.inheritProviders = cba.inheritServerProviders();
                this.baseUri = cba.baseUri();
            }
        }

        /**
         * Get the client binding annotation this model represents.
         *
         * @return client binding annotation.
         */
        public Annotation getAnnotation() {
            return annotation;
        }

        /**
         * Get the configuration class to be used.
         *
         * @return client configuration class to be used.
         */
        public Class<? extends Configuration> getConfigClass() {
            return configClass;
        }

        /**
         * Check if the server-side providers should be inherited.
         *
         * @return {@code true} if server-side providers should be inherited, {@code false} otherwise.
         */
        public boolean inheritProviders() {
            return inheritProviders;
        }

        /**
         * Get the client base URI.
         *
         * @return client base URI.
         */
        public String baseUri() {
            return baseUri;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            BindingModel that = (BindingModel) o;
            return annotation != null ? annotation.equals(that.annotation) : that.annotation == null;
        }

        @Override
        public int hashCode() {
            return annotation != null ? annotation.hashCode() : 0;
        }

        @Override
        public String toString() {
            return "BindingModel{"
                    + "binding=" + annotation
                    + ", configClass=" + configClass
                    + ", inheritProviders=" + inheritProviders
                    + ", baseUri=" + baseUri
                    + '}';
        }
    }

    /**
     * {@link Uri} injection resolver.
     */
    static final class InjectionResolver extends ParamInjectionResolver<Uri> {

        /**
         * Create new injection resolver.
         */
        public InjectionResolver() {
            super(WebTargetValueFactoryProvider.class);
        }
    }

    private static final class WebTargetValueFactory extends AbstractContainerRequestValueFactory<WebTarget> {

        private final String uri;
        private final Value<ManagedClient> client;

        WebTargetValueFactory(String uri, Value<ManagedClient> client) {
            this.uri = uri;
            this.client = client;
        }

        @Override
        public WebTarget provide() {
            // no need for try-catch - unlike for @*Param annotations, any issues with @Uri would usually be caused
            // by incorrect server code, so the default runtime exception mapping to 500 is appropriate
            final ExtendedUriInfo uriInfo = getContainerRequest().getUriInfo();

            final Map<String, Object> pathParamValues = Maps.transformValues(uriInfo.getPathParameters(),
                    new Function<List<String>, Object>() {

                        @Override
                        public Object apply(List<String> input) {
                            return input.isEmpty() ? null : input.get(0);
                        }
                    });
            JerseyUriBuilder uriBuilder = new JerseyUriBuilder().uri(this.uri).resolveTemplates(pathParamValues);

            final ManagedClient managedClient = client.get();

            if (!uriBuilder.isAbsolute()) {
                final String customBaseUri = managedClient.customBaseUri;
                final String rootUri = customBaseUri.isEmpty() ? uriInfo.getBaseUri().toString() : customBaseUri;

                uriBuilder = new JerseyUriBuilder().uri(rootUri).path(uriBuilder.toTemplate());
            }

            return managedClient.instance.target(uriBuilder);
        }
    }

    /**
     * Initialize the provider.
     *
     * @param locator      service locator to be used for injecting into the values factory.
     * @param serverConfig server-side configuration.
     */
    @Inject
    public WebTargetValueFactoryProvider(final ServiceLocator locator, @Context final Configuration serverConfig) {
        super(null, locator, Parameter.Source.URI);

        this.serverConfig = serverConfig;

        this.managedClients = new ConcurrentHashMap<BindingModel, Value<ManagedClient>>();
        // init default client
        this.managedClients.put(BindingModel.EMPTY, Values.lazy(new Value<ManagedClient>() {
            @Override
            public ManagedClient get() {
                final Client client;
                if (serverConfig == null) {
                    client = ClientBuilder.newClient();
                } else {
                    ClientConfig clientConfig = new ClientConfig();
                    copyProviders(serverConfig, clientConfig);
                    client = ClientBuilder.newClient(clientConfig);
                }
                return new ManagedClient(client, "");
            }
        }));
    }

    private void copyProviders(Configuration source, Configurable<?> target) {
        final Configuration targetConfig = target.getConfiguration();
        for (Class<?> c : source.getClasses()) {
            if (!targetConfig.isRegistered(c)) {
                target.register(c, source.getContracts(c));
            }
        }

        for (Object o : source.getInstances()) {
            Class<?> c = o.getClass();
            if (!targetConfig.isRegistered(o)) {
                target.register(c, source.getContracts(c));
            }
        }
    }

    @Override
    protected AbstractContainerRequestValueFactory<?> createValueFactory(final Parameter parameter) {
        return Errors.processWithException(new Producer<AbstractContainerRequestValueFactory<?>>() {

            @Override
            public AbstractContainerRequestValueFactory<?> call() {
                String targetUriTemplate = parameter.getSourceName();
                if (targetUriTemplate == null || targetUriTemplate.length() == 0) {
                    // Invalid URI parameter name
                    Errors.warning(this, LocalizationMessages.INJECTED_WEBTARGET_URI_INVALID(targetUriTemplate));
                    return null;
                }

                final Class<?> rawParameterType = parameter.getRawType();
                if (rawParameterType == WebTarget.class) {
                    final BindingModel binding = BindingModel.create(Arrays.<Annotation>asList(parameter.getAnnotations()));

                    Value<ManagedClient> client = managedClients.get(binding);
                    if (client == null) {
                        client = Values.lazy(new Value<ManagedClient>() {
                            @Override
                            public ManagedClient get() {
                                final String prefix = binding.getAnnotation().annotationType().getName() + ".";
                                final String baseUriProperty = prefix + "baseUri";
                                final Object bu = serverConfig.getProperty(baseUriProperty);
                                final String customBaseUri = (bu != null) ? bu.toString() : binding.baseUri();

                                final String configClassProperty = prefix + "configClass";
                                final ClientConfig cfg = resolveConfig(configClassProperty, binding);

                                final String inheritProvidersProperty = prefix + "inheritServerProviders";
                                if (PropertiesHelper.isProperty(serverConfig.getProperty(inheritProvidersProperty))
                                        || binding.inheritProviders()) {
                                    copyProviders(serverConfig, cfg);
                                }

                                final String propertyPrefix = prefix + "property.";
                                Collection<String> clientProperties =
                                        Collections2.filter(serverConfig.getPropertyNames(), new Predicate<String>() {
                                            @Override
                                            public boolean apply(String property) {
                                                return property.startsWith(propertyPrefix);
                                            }
                                        });

                                for (String property : clientProperties) {
                                    cfg.property(property.substring(propertyPrefix.length()),
                                            serverConfig.getProperty(property));
                                }

                                return new ManagedClient(ClientBuilder.newClient(cfg), customBaseUri);
                            }
                        });
                        final Value<ManagedClient> previous = managedClients.putIfAbsent(binding, client);
                        if (previous != null) {
                            client = previous;
                        }
                    }
                    return new WebTargetValueFactory(targetUriTemplate, client);
                } else {
                    Errors.warning(this, LocalizationMessages.UNSUPPORTED_URI_INJECTION_TYPE(rawParameterType));
                    return null;
                }
            }
        });
    }

    private ClientConfig resolveConfig(final String configClassProperty, final BindingModel binding) {
        Class<? extends Configuration> configClass = binding.getConfigClass();
        final Object _cc = serverConfig.getProperty(configClassProperty);
        if (_cc != null) {
            Class<?> cc;
            if (_cc instanceof String) {
                cc = AccessController.doPrivileged(ReflectionHelper.classForNamePA((String) _cc));
            } else if (_cc instanceof Class) {
                cc = (Class<?>) _cc;
            } else {
                cc = null; // will cause a warning
            }

            if (cc != null && Configuration.class.isAssignableFrom(cc)) {
                configClass = cc.asSubclass(Configuration.class);
            } else {
                Errors.warning(this, LocalizationMessages.ILLEGAL_CLIENT_CONFIG_CLASS_PROPERTY_VALUE(
                        configClassProperty,
                        _cc,
                        configClass.getName()
                ));
            }
        }

        final Configuration cfg = Injections.getOrCreate(getLocator(), configClass);

        return (cfg instanceof ClientConfig) ? (ClientConfig) cfg : new ClientConfig().loadFrom(cfg);
    }
}
