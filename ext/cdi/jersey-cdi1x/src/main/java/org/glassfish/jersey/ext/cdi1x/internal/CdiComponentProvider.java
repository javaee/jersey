/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.ext.cdi1x.internal;

import java.lang.annotation.Annotation;
import java.lang.annotation.Retention;
import java.lang.annotation.Target;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Member;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.annotation.ManagedBean;
import javax.annotation.Priority;
import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.context.Dependent;
import javax.enterprise.context.RequestScoped;
import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.event.Observes;
import javax.enterprise.inject.Default;
import javax.enterprise.inject.spi.AfterBeanDiscovery;
import javax.enterprise.inject.spi.AfterTypeDiscovery;
import javax.enterprise.inject.spi.Annotated;
import javax.enterprise.inject.spi.AnnotatedCallable;
import javax.enterprise.inject.spi.AnnotatedConstructor;
import javax.enterprise.inject.spi.AnnotatedParameter;
import javax.enterprise.inject.spi.AnnotatedType;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.BeanManager;
import javax.enterprise.inject.spi.BeforeBeanDiscovery;
import javax.enterprise.inject.spi.Extension;
import javax.enterprise.inject.spi.InjectionPoint;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.ProcessAnnotatedType;
import javax.enterprise.inject.spi.ProcessInjectionTarget;
import javax.enterprise.util.AnnotationLiteral;
import javax.inject.Qualifier;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.Context;

import org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2InjectedTarget;
import org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2LocatorManager;
import org.glassfish.jersey.ext.cdi1x.internal.spi.InjectionTargetListener;
import org.glassfish.jersey.ext.cdi1x.spi.Hk2CustomBoundTypesProvider;
import org.glassfish.jersey.internal.inject.ForeignRequestScopeBridge;
import org.glassfish.jersey.internal.inject.Injections;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.internal.ValueFactoryProvider;

import org.glassfish.hk2.api.ClassAnalyzer;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.utilities.binding.ScopedBindingBuilder;
import org.glassfish.hk2.utilities.binding.ServiceBindingBuilder;
import org.glassfish.hk2.utilities.cache.Cache;
import org.glassfish.hk2.utilities.cache.Computable;

import static java.lang.annotation.ElementType.FIELD;
import static java.lang.annotation.ElementType.METHOD;
import static java.lang.annotation.ElementType.PARAMETER;
import static java.lang.annotation.ElementType.TYPE;
import static java.lang.annotation.RetentionPolicy.RUNTIME;

/**
 * Jersey CDI integration implementation.
 * Implements {@link ComponentProvider Jersey component provider} to serve CDI beans
 * obtained from the actual CDI bean manager.
 * To properly inject JAX-RS/Jersey managed beans into CDI, it also
 * serves as a {@link Extension CDI Extension}, that intercepts CDI injection targets.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
@Priority(200)
public class CdiComponentProvider implements ComponentProvider, Extension {

    private static final Logger LOGGER = Logger.getLogger(CdiComponentProvider.class.getName());

    /**
     * annotation types that distinguish the classes to be added to {@link #jaxrsInjectableTypes}
     */
    private static final Set<Class<? extends Annotation>> JAX_RS_INJECT_ANNOTATIONS =
            new HashSet<Class<? extends Annotation>>() {{
                addAll(JaxRsParamProducer.JAX_RS_STRING_PARAM_ANNOTATIONS);
                add(Context.class);
            }};

    /**
     * Name to be used when binding CDI injectee skipping class analyzer to HK2 service locator.
     */
    public static final String CDI_CLASS_ANALYZER = "CdiInjecteeSkippingClassAnalyzer";

    /**
     * set of non JAX-RS components containing JAX-RS injection points
     */
    private final Set<Type> jaxrsInjectableTypes = new HashSet<>();
    private final Set<Type> hk2ProvidedTypes = Collections.synchronizedSet(new HashSet<Type>());
    private final Set<Type> jerseyVetoedTypes = Collections.synchronizedSet(new HashSet<Type>());

    /**
     * set of request scoped components
     */
    private final Set<Class<?>> requestScopedComponents = new HashSet<>();


    private final Cache<Class<?>, Boolean> jaxRsComponentCache = new Cache<>(new Computable<Class<?>, Boolean>() {
        @Override
        public Boolean compute(final Class<?> clazz) {
            return Application.class.isAssignableFrom(clazz)
                    || Providers.isJaxRsProvider(clazz)
                    || jaxRsResourceCache.compute(clazz);
        }
    });

    private final Cache<Class<?>, Boolean> jaxRsResourceCache = new Cache<>(new Computable<Class<?>, Boolean>() {
        @Override
        public Boolean compute(final Class<?> clazz) {
            return Resource.from(clazz) != null;
        }
    });

    private final Hk2CustomBoundTypesProvider customHk2TypesProvider;
    private final Hk2LocatorManager locatorManager;

    private volatile ServiceLocator locator;
    private volatile BeanManager beanManager;

    private volatile Map<Class<?>, Set<Method>> methodsToSkip = new HashMap<>();
    private volatile Map<Class<?>, Set<Field>> fieldsToSkip = new HashMap<>();

    public CdiComponentProvider() {
        customHk2TypesProvider = CdiUtil.lookupService(Hk2CustomBoundTypesProvider.class);
        locatorManager = CdiUtil.createHk2LocatorManager();
    }

    @Override
    public void initialize(final ServiceLocator locator) {
        this.locator = locator;
        this.beanManager = CdiUtil.getBeanManager();

        if (beanManager != null) {
            // Try to get CdiComponentProvider created by CDI.
            final CdiComponentProvider extension = beanManager.getExtension(CdiComponentProvider.class);

            if (extension != null) {
                extension.addLocator(this.locator);

                this.fieldsToSkip = extension.getFieldsToSkip();
                this.methodsToSkip = extension.getMethodsToSkip();

                bindHk2ClassAnalyzer();

                LOGGER.config(LocalizationMessages.CDI_PROVIDER_INITIALIZED());
            }
        }
    }

    /**
     * CDI producer for CDI bean constructor String parameters, that should be injected by JAX-RS.
     */
    @ApplicationScoped
    public static class JaxRsParamProducer {

        @Qualifier
        @Retention(RUNTIME)
        @Target({METHOD, FIELD, PARAMETER, TYPE})
        public static @interface JaxRsParamQualifier {
        }

        private static final JaxRsParamQualifier JaxRsParamQUALIFIER = new JaxRsParamQualifier() {

            @Override
            public Class<? extends Annotation> annotationType() {
                return JaxRsParamQualifier.class;
            }
        };

        static final Set<Class<? extends Annotation>> JAX_RS_STRING_PARAM_ANNOTATIONS =
                new HashSet<Class<? extends Annotation>>() {{
                    add(javax.ws.rs.PathParam.class);
                    add(javax.ws.rs.QueryParam.class);
                    add(javax.ws.rs.CookieParam.class);
                    add(javax.ws.rs.HeaderParam.class);
                    add(javax.ws.rs.MatrixParam.class);
                    add(javax.ws.rs.FormParam.class);
                }};

        /**
         * Internal cache to store CDI {@link InjectionPoint} to Jersey {@link Parameter} mapping.
         */
        final Cache<InjectionPoint, Parameter> parameterCache = new Cache<>(new Computable<InjectionPoint, Parameter>() {

            @Override
            public Parameter compute(final InjectionPoint injectionPoint) {
                final Annotated annotated = injectionPoint.getAnnotated();
                final Class<?> clazz = injectionPoint.getMember().getDeclaringClass();

                if (annotated instanceof AnnotatedParameter) {

                    final AnnotatedParameter annotatedParameter = (AnnotatedParameter) annotated;
                    final AnnotatedCallable callable = annotatedParameter.getDeclaringCallable();

                    if (callable instanceof AnnotatedConstructor) {

                        final AnnotatedConstructor ac = (AnnotatedConstructor) callable;
                        final int position = annotatedParameter.getPosition();
                        final List<Parameter> parameters = Parameter.create(clazz, clazz, ac.getJavaMember(), false);

                        return parameters.get(position);
                    }
                }

                return null;
            }
        });

        /**
         * Provide a value for given injection point. If the injection point does not refer
         * to a CDI bean constructor parameter, or the value could not be found, the method will return null.
         *
         * @param injectionPoint actual injection point.
         * @param beanManager    current application bean manager.
         * @return concrete JAX-RS parameter value for given injection point.
         */
        @javax.enterprise.inject.Produces
        @JaxRsParamQualifier
        public String getParameterValue(final InjectionPoint injectionPoint, final BeanManager beanManager) {
            final Parameter parameter = parameterCache.compute(injectionPoint);

            if (parameter != null) {
                final ServiceLocator locator = beanManager.getExtension(CdiComponentProvider.class).getEffectiveLocator();
                final Set<ValueFactoryProvider> providers = Providers.getProviders(locator, ValueFactoryProvider.class);

                for (final ValueFactoryProvider vfp : providers) {
                    final Factory<?> valueFactory = vfp.getValueFactory(parameter);
                    if (valueFactory != null) {
                        return (String) valueFactory.provide();
                    }
                }
            }

            return null;
        }
    }

    @Override
    public boolean bind(final Class<?> clazz, final Set<Class<?>> providerContracts) {
        if (LOGGER.isLoggable(Level.FINE)) {
            LOGGER.fine(LocalizationMessages.CDI_CLASS_BEING_CHECKED(clazz));
        }

        if (beanManager == null) {
            return false;
        }

        if (isJerseyOrDependencyType(clazz)) {
            return false;
        }

        final boolean isCdiManaged = isCdiComponent(clazz);
        final boolean isManagedBean = isManagedBean(clazz);
        final boolean isJaxRsComponent = isJaxRsComponentType(clazz);

        if (!isCdiManaged && !isManagedBean && !isJaxRsComponent) {
            return false;
        }

        final boolean isJaxRsResource = jaxRsResourceCache.compute(clazz);

        final DynamicConfiguration dc = Injections.getConfiguration(locator);

        final Class<? extends Annotation> beanScopeAnnotation = CdiUtil.getBeanScope(clazz, beanManager);
        final boolean isRequestScoped = beanScopeAnnotation == RequestScoped.class
                                        || (beanScopeAnnotation == Dependent.class && isJaxRsResource);

        Factory beanFactory = isRequestScoped
                ? new RequestScopedCdiBeanHk2Factory(clazz, locator, beanManager, isCdiManaged)
                : new GenericCdiBeanHk2Factory(clazz, locator, beanManager, isCdiManaged);

        final ServiceBindingBuilder bindingBuilder =
                    Injections.newFactoryBinder(beanFactory);

        bindingBuilder.to(clazz);
        for (final Class contract : providerContracts) {
            bindingBuilder.to(contract);
        }

        Injections.addBinding(bindingBuilder, dc);

        dc.commit();

        if (isRequestScoped) {
            requestScopedComponents.add(clazz);
        }

        if (LOGGER.isLoggable(Level.CONFIG)) {
            LOGGER.config(LocalizationMessages.CDI_CLASS_BOUND_WITH_CDI(clazz));
        }

        return true;
    }

    @Override
    public void done() {
        if (requestScopedComponents.size() > 0) {
            final DynamicConfiguration dc = Injections.getConfiguration(locator);
            Injections.addBinding(Injections.newBinder(new ForeignRequestScopeBridge() {
                @Override
                public Set<Class<?>> getRequestScopedComponents() {
                    return requestScopedComponents;
                }
            }).to(ForeignRequestScopeBridge.class), dc);
            dc.commit();
            if (LOGGER.isLoggable(Level.CONFIG)) {
                LOGGER.config(LocalizationMessages.CDI_REQUEST_SCOPED_COMPONENTS_RECOGNIZED(
                        listElements(new StringBuilder().append("\n"), requestScopedComponents).toString()));
            }
        }
    }

    private boolean isCdiComponent(final Class<?> component) {
        final Annotation[] qualifiers = CdiUtil.getQualifiers(component.getAnnotations());
        return !beanManager.getBeans(component, qualifiers).isEmpty();
    }

    private boolean isManagedBean(final Class<?> component) {
        return component.isAnnotationPresent(ManagedBean.class);
    }

    private static AnnotatedConstructor<?> enrichedConstructor(final AnnotatedConstructor<?> ctor) {
        return new AnnotatedConstructor(){

            @Override
            public Constructor getJavaMember() {
                return ctor.getJavaMember();
            }

            @Override
            public List<AnnotatedParameter> getParameters() {
                final List<AnnotatedParameter> parameters = new ArrayList<>(ctor.getParameters().size());

                for (final AnnotatedParameter<?> ap : ctor.getParameters()) {
                    parameters.add(new AnnotatedParameter() {

                        @Override
                        public int getPosition() {
                            return ap.getPosition();
                        }

                        @Override
                        public AnnotatedCallable getDeclaringCallable() {
                            return ap.getDeclaringCallable();
                        }

                        @Override
                        public Type getBaseType() {
                            return ap.getBaseType();
                        }

                        @Override
                        public Set<Type> getTypeClosure() {
                            return ap.getTypeClosure();
                        }

                        @Override
                        public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
                            if (annotationType == JaxRsParamProducer.JaxRsParamQualifier.class) {
                                return hasAnnotation(ap, JaxRsParamProducer.JAX_RS_STRING_PARAM_ANNOTATIONS)
                                        ? (T) JaxRsParamProducer.JaxRsParamQUALIFIER : null;
                            } else {
                                return ap.getAnnotation(annotationType);
                            }
                        }

                        @Override
                        public Set<Annotation> getAnnotations() {
                            final Set<Annotation> result = new HashSet<>();
                            for (final Annotation a : ap.getAnnotations()) {
                                result.add(a);
                                final Class<? extends Annotation> annotationType = a.annotationType();
                                if (JaxRsParamProducer.JAX_RS_STRING_PARAM_ANNOTATIONS.contains(annotationType)) {
                                    result.add(JaxRsParamProducer.JaxRsParamQUALIFIER);
                                }
                            }
                            return result;
                        }

                        @Override
                        public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
                            return (annotationType == JaxRsParamProducer.JaxRsParamQualifier.class
                                            && hasAnnotation(ap, JaxRsParamProducer.JAX_RS_STRING_PARAM_ANNOTATIONS))
                                    || ap.isAnnotationPresent(annotationType);
                        }
                    });
                }
                return parameters;
            }

            @Override
            public boolean isStatic() {
                return ctor.isStatic();
            }

            @Override
            public AnnotatedType getDeclaringType() {
                return ctor.getDeclaringType();
            }

            @Override
            public Type getBaseType() {
                return ctor.getBaseType();
            }

            @Override
            public Set<Type> getTypeClosure() {
                return ctor.getTypeClosure();
            }

            @Override
            public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
                return ctor.getAnnotation(annotationType);
            }

            @Override
            public Set<Annotation> getAnnotations() {
                return ctor.getAnnotations();
            }

            @Override
            public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
                return ctor.isAnnotationPresent(annotationType);
            }
        };
    }

    @SuppressWarnings("unused")
    private void processAnnotatedType(@Observes
    // We can not apply the following constraint
    // if we want to fully support {@link org.glassfish.jersey.ext.cdi1x.spi.Hk2CustomBoundTypesProvider}.
    // Covered by tests/integration/cdi-with-jersey-injection-custom-cfg-webapp test application:
//                                      @WithAnnotations({
//                                              Context.class,
//                                              ApplicationPath.class,
//                                              HeaderParam.class,
//                                              QueryParam.class,
//                                              FormParam.class,
//                                              MatrixParam.class,
//                                              BeanParam.class,
//                                              PathParam.class})
                                      final ProcessAnnotatedType processAnnotatedType) {
        final AnnotatedType<?> annotatedType = processAnnotatedType.getAnnotatedType();

        // if one of the JAX-RS annotations is present in the currently seen class, add it to the "whitelist"
        if (containsJaxRsConstructorInjection(annotatedType)
                || containsJaxRsFieldInjection(annotatedType)
                || containsJaxRsMethodInjection(annotatedType)) {
            jaxrsInjectableTypes.add(annotatedType.getBaseType());
        }

        if (customHk2TypesProvider != null) {
            final Type baseType = annotatedType.getBaseType();
            if (customHk2TypesProvider.getHk2Types().contains(baseType)) {
                processAnnotatedType.veto();
                jerseyVetoedTypes.add(baseType);
            }
        }

        if (containsJaxRsParameterizedCtor(annotatedType)) {
            processAnnotatedType.setAnnotatedType(new AnnotatedType() {

                @Override
                public Class getJavaClass() {
                    return annotatedType.getJavaClass();
                }

                @Override
                public Set<AnnotatedConstructor> getConstructors() {
                    final Set<AnnotatedConstructor> result = new HashSet<>();
                    for (final AnnotatedConstructor c : annotatedType.getConstructors()) {
                        result.add(enrichedConstructor(c));
                    }
                    return result;
                }

                @Override
                public Set getMethods() {
                    return annotatedType.getMethods();
                }

                @Override
                public Set getFields() {
                    return annotatedType.getFields();
                }

                @Override
                public Type getBaseType() {
                    return annotatedType.getBaseType();
                }

                @Override
                public Set<Type> getTypeClosure() {
                    return annotatedType.getTypeClosure();
                }

                @Override
                public <T extends Annotation> T getAnnotation(final Class<T> annotationType) {
                    return annotatedType.getAnnotation(annotationType);
                }

                @Override
                public Set<Annotation> getAnnotations() {
                    return annotatedType.getAnnotations();
                }

                @Override
                public boolean isAnnotationPresent(final Class<? extends Annotation> annotationType) {
                    return annotatedType.isAnnotationPresent(annotationType);
                }
            });
        }
    }

    private boolean containsJaxRsParameterizedCtor(final AnnotatedType annotatedType) {
        return containAnnotatedParameters(annotatedType.getConstructors(), JaxRsParamProducer.JAX_RS_STRING_PARAM_ANNOTATIONS);
    }

    private boolean containsJaxRsConstructorInjection(final AnnotatedType annotatedType) {
        return containAnnotatedParameters(annotatedType.getConstructors(), JAX_RS_INJECT_ANNOTATIONS);
    }

    private boolean containsJaxRsMethodInjection(final AnnotatedType annotatedType) {
        return containAnnotatedParameters(annotatedType.getMethods(), JAX_RS_INJECT_ANNOTATIONS);
    }

    private boolean containsJaxRsFieldInjection(final AnnotatedType annotatedType) {
        return containAnnotation(annotatedType.getFields(), JAX_RS_INJECT_ANNOTATIONS);
    }

    private boolean containAnnotatedParameters(final Collection<AnnotatedCallable> annotatedCallables,
                                               final Set<Class<? extends Annotation>> annotationSet) {
        for (final AnnotatedCallable c : annotatedCallables) {
            if (containAnnotation(c.getParameters(), annotationSet)) {
                return true;
            }
        }
        return false;
    }

    private boolean containAnnotation(final Collection<Annotated> elements,
                                      final Set<Class<? extends Annotation>> annotationSet) {
        for (final Annotated element : elements) {
            if (hasAnnotation(element, annotationSet)) {
                return true;
            }
        }
        return false;
    }

    private static boolean hasAnnotation(final Annotated element, final Set<Class<? extends Annotation>> annotations) {
        for (final Class<? extends Annotation> a : annotations) {
            if (element.isAnnotationPresent(a)) {
                return true;
            }
        }
        return false;
    }

    @SuppressWarnings("unused")
    private void afterTypeDiscovery(@Observes final AfterTypeDiscovery afterTypeDiscovery) {
        if (LOGGER.isLoggable(Level.CONFIG) && !jerseyVetoedTypes.isEmpty()) {
            LOGGER.config(LocalizationMessages.CDI_TYPE_VETOED(customHk2TypesProvider,
                    listElements(new StringBuilder().append("\n"), jerseyVetoedTypes).toString()));
        }
    }

    @SuppressWarnings("unused")
    private void beforeBeanDiscovery(@Observes final BeforeBeanDiscovery beforeBeanDiscovery, final BeanManager beanManager) {
        beforeBeanDiscovery.addAnnotatedType(beanManager.createAnnotatedType(JaxRsParamProducer.class));
    }

    @SuppressWarnings("unused")
    private void processInjectionTarget(@Observes final ProcessInjectionTarget event) {
        final InjectionTarget it = event.getInjectionTarget();
        final Class<?> componentClass = event.getAnnotatedType().getJavaClass();

        final Set<InjectionPoint> cdiInjectionPoints = filterHk2InjectionPointsOut(it.getInjectionPoints());

        for (final InjectionPoint injectionPoint : cdiInjectionPoints) {
            final Member member = injectionPoint.getMember();
            if (member instanceof Field) {
                addInjecteeToSkip(componentClass, fieldsToSkip, (Field) member);
            } else if (member instanceof Method) {
                addInjecteeToSkip(componentClass, methodsToSkip, (Method) member);
            }
        }

        Hk2InjectedCdiTarget target = null;
        if (isJerseyOrDependencyType(componentClass)) {
            target = new Hk2InjectedCdiTarget(it) {

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    // Tell CDI to ignore Jersey (or it's dependencies) classes when injecting.
                    // CDI will not treat these classes as CDI beans (as they are not).
                    return Collections.emptySet();
                }
            };
        } else if (isJaxRsComponentType(componentClass)
                || jaxrsInjectableTypes.contains(event.getAnnotatedType().getBaseType())) {
            target = new Hk2InjectedCdiTarget(it) {

                @Override
                public Set<InjectionPoint> getInjectionPoints() {
                    // Inject CDI beans into JAX-RS resources/providers/application.
                    return cdiInjectionPoints;
                }
            };
        }

        if (target != null) {
            notify(target);
            //noinspection unchecked
            event.setInjectionTarget(target);
        }
    }

    private Set<InjectionPoint> filterHk2InjectionPointsOut(final Set<InjectionPoint> originalInjectionPoints) {
        final Set<InjectionPoint> filteredInjectionPoints = new HashSet<>();
        for (final InjectionPoint ip : originalInjectionPoints) {
            final Type injectedType = ip.getType();
            if (customHk2TypesProvider != null && customHk2TypesProvider.getHk2Types().contains(injectedType)) {
                //remember the type, we would need to mock it's CDI binding at runtime
                hk2ProvidedTypes.add(injectedType);
            } else {
                if (injectedType instanceof Class<?>) {
                    final Class<?> injectedClass = (Class<?>) injectedType;
                    if (isJerseyOrDependencyType(injectedClass)) {
                        //remember the type, we would need to mock it's CDI binding at runtime
                        hk2ProvidedTypes.add(injectedType);
                    } else {
                        filteredInjectionPoints.add(ip);
                    }
                } else { // it is not a class, maybe provider type?:
                    if (isInjectionProvider(injectedType)
                            && (isProviderOfJerseyType((ParameterizedType) injectedType))) {
                        //remember the type, we would need to mock it's CDI binding at runtime
                        hk2ProvidedTypes.add(((ParameterizedType) injectedType).getActualTypeArguments()[0]);
                    } else {
                        filteredInjectionPoints.add(ip);
                    }
                }
            }
        }
        return filteredInjectionPoints;
    }

    private boolean isInjectionProvider(final Type injectedType) {
        return injectedType instanceof ParameterizedType
                && ((ParameterizedType) injectedType).getRawType() == javax.inject.Provider.class;
    }

    private boolean isProviderOfJerseyType(final ParameterizedType provider) {
        final Type firstArgumentType = provider.getActualTypeArguments()[0];
        if (firstArgumentType instanceof Class && isJerseyOrDependencyType((Class<?>) firstArgumentType)) {
            return true;
        }
        return (customHk2TypesProvider != null && customHk2TypesProvider.getHk2Types().contains(firstArgumentType));
    }

    private <T> void addInjecteeToSkip(final Class<?> componentClass, final Map<Class<?>, Set<T>> toSkip, final T member) {
        if (!toSkip.containsKey(componentClass)) {
            toSkip.put(componentClass, new HashSet<T>());
        }
        toSkip.get(componentClass).add(member);
    }

    /**
     * Auxiliary annotation for mocked beans used to cover Jersey/HK2 injected injection points.
     */
    @SuppressWarnings("serial")
    public static class CdiDefaultAnnotation extends AnnotationLiteral<Default> implements Default {

        private static final long serialVersionUID = 1L;
    }

    @SuppressWarnings({"unused", "unchecked", "rawtypes"})
    private void afterDiscoveryObserver(@Observes final AfterBeanDiscovery abd) {
        if (customHk2TypesProvider != null) {
            hk2ProvidedTypes.addAll(customHk2TypesProvider.getHk2Types());
        }

        for (final Type t : hk2ProvidedTypes) {
            abd.addBean(new Hk2Bean(t));
        }
    }

    /**
     * Gets you fields to skip from a proxied instance.
     * <p/>
     * Note: Do NOT lower the visibility of this method. CDI proxies need at least this visibility.
     *
     * @return fields to skip when injecting via HK2
     */
    /* package */ Map<Class<?>, Set<Field>> getFieldsToSkip() {
        return fieldsToSkip;
    }

    /**
     * Gets you methods to skip (from a proxied instance).
     * <p/>
     * Note: Do NOT lower the visibility of this method. CDI proxies need at least this visibility.
     *
     * @return methods to skip when injecting via HK2
     */
    /* package */ Map<Class<?>, Set<Method>> getMethodsToSkip() {
        return methodsToSkip;
    }

    /**
     * Gets you effective locator.
     * <p/>
     * Note: Do NOT lower the visibility of this method. CDI proxies need at least this visibility.
     *
     * @return HK2 locator
     */
    /* package */ ServiceLocator getEffectiveLocator() {
        return locatorManager.getEffectiveLocator();
    }

    /**
     * Add HK2 {@link org.glassfish.hk2.api.ServiceLocator locator} (to a proxied instance).
     * <p/>
     * Note: Do NOT lower the visibility of this method. CDI proxies need at least this visibility.
     *
     * @param locator HK2 locator
     */
    /* package */ void addLocator(final ServiceLocator locator) {
        locatorManager.registerLocator(locator);
    }

    /**
     * Notifies the {@code InjectionTargetListener injection target listener} about new
     * {@link org.glassfish.jersey.ext.cdi1x.internal.spi.Hk2InjectedTarget injected target}.
     * <p/>
     * Note: Do NOT lower the visibility of this method. CDI proxies need at least this visibility.
     *
     * @param target new injected target.
     */
    /* package */ void notify(final Hk2InjectedTarget target) {
        if (locatorManager instanceof InjectionTargetListener) {
            ((InjectionTargetListener) locatorManager).notify(target);
        }
    }

    /**
     * Introspect given type to determine if it represents a JAX-RS component.
     *
     * @param clazz type to be introspected.
     * @return true if the type represents a JAX-RS component type.
     */
    /* package */ boolean isJaxRsComponentType(final Class<?> clazz) {
        return jaxRsComponentCache.compute(clazz);
    }

    private static boolean isJerseyOrDependencyType(final Class<?> clazz) {
        if (clazz.isPrimitive() || clazz.isSynthetic()) {
            return false;
        }

        final Package pkg = clazz.getPackage();
        if (pkg == null) { // Class.getPackage() could return null
            LOGGER.warning(String.format("Class %s has null package", clazz));
            return false;
        }

        final String pkgName = pkg.getName();
        return !clazz.isAnnotationPresent(JerseyVetoed.class)
                && (pkgName.contains("org.glassfish.hk2")
                            || pkgName.contains("jersey.repackaged")
                            || pkgName.contains("org.jvnet.hk2")
                            || (pkgName.startsWith("org.glassfish.jersey")
                                        && !pkgName.startsWith("org.glassfish.jersey.examples")
                                        && !pkgName.startsWith("org.glassfish.jersey.tests"))
                            || (pkgName.startsWith("com.sun.jersey")
                                        && !pkgName.startsWith("com.sun.jersey.examples")
                                        && !pkgName.startsWith("com.sun.jersey.tests")));
    }

    private void bindHk2ClassAnalyzer() {
        final ClassAnalyzer defaultClassAnalyzer =
                locator.getService(ClassAnalyzer.class, ClassAnalyzer.DEFAULT_IMPLEMENTATION_NAME);

        final int skippedElements = methodsToSkip.size() + fieldsToSkip.size();

        final ClassAnalyzer customizedClassAnalyzer = skippedElements > 0
                ? new InjecteeSkippingAnalyzer(defaultClassAnalyzer, methodsToSkip, fieldsToSkip)
                : defaultClassAnalyzer;

        final DynamicConfiguration dc = Injections.getConfiguration(locator);

        final ScopedBindingBuilder bindingBuilder =
                Injections.newBinder(customizedClassAnalyzer);

        bindingBuilder.analyzeWith(ClassAnalyzer.DEFAULT_IMPLEMENTATION_NAME)
                .to(ClassAnalyzer.class)
                .named(CDI_CLASS_ANALYZER);

        Injections.addBinding(bindingBuilder, dc);

        dc.commit();
    }

    private StringBuilder listElements(final StringBuilder logMsgBuilder, final Collection<? extends Object> elements) {
        for (final Object t : elements) {
            logMsgBuilder.append(String.format(" - %s%n", t));
        }
        return logMsgBuilder;
    }

    @SuppressWarnings("unchecked")
    private abstract class Hk2InjectedCdiTarget implements Hk2InjectedTarget {

        private final InjectionTarget delegate;
        private volatile ServiceLocator effectiveLocator;

        public Hk2InjectedCdiTarget(InjectionTarget delegate) {
            this.delegate = delegate;
        }

        @Override
        public abstract Set<InjectionPoint> getInjectionPoints();

        @Override
        public void inject(final Object t, final CreationalContext cc) {
            delegate.inject(t, cc);

            ServiceLocator injectingLocator = getEffectiveLocator();
            if (injectingLocator == null) {
                injectingLocator = effectiveLocator;
            }

            if (injectingLocator != null) {
                injectingLocator.inject(t, CdiComponentProvider.CDI_CLASS_ANALYZER);
            }
        }

        @Override
        public void postConstruct(final Object t) {
            delegate.postConstruct(t);
        }

        @Override
        public void preDestroy(final Object t) {
            delegate.preDestroy(t);
        }

        @Override
        public Object produce(final CreationalContext cc) {
            return delegate.produce(cc);
        }

        @Override
        public void dispose(final Object t) {
            delegate.dispose(t);
        }

        @Override
        public void setLocator(final ServiceLocator effectiveLocator) {
            this.effectiveLocator = effectiveLocator;
        }
    }

    private class Hk2Bean implements Bean {

        private final Type t;

        public Hk2Bean(final Type t) {
            this.t = t;
        }

        @Override
        public Class getBeanClass() {
            return (Class) t;
        }

        @Override
        public Set getInjectionPoints() {
            return Collections.emptySet();
        }

        @Override
        public boolean isNullable() {
            return true;
        }

        @Override
        public Object create(final CreationalContext creationalContext) {
            return getEffectiveLocator().getService(t);
        }

        @Override
        public void destroy(final Object instance, final CreationalContext creationalContext) {
        }

        @Override
        public Set getTypes() {
            return Collections.singleton(t);
        }

        @Override
        public Set getQualifiers() {
            return Collections.singleton(new CdiDefaultAnnotation());
        }

        @Override
        public Class getScope() {
            return Dependent.class;
        }

        @Override
        public String getName() {
            return t.toString();
        }

        @Override
        public Set getStereotypes() {
            return Collections.emptySet();
        }

        @Override
        public boolean isAlternative() {
            return false;
        }
    }
}
