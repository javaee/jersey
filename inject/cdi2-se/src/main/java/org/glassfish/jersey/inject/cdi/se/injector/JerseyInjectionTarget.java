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

package org.glassfish.jersey.inject.cdi.se.injector;

import java.lang.reflect.Modifier;
import java.util.Collection;
import java.util.List;

import javax.ws.rs.WebApplicationException;

import javax.enterprise.context.spi.CreationalContext;
import javax.enterprise.inject.InjectionException;
import javax.enterprise.inject.spi.Bean;
import javax.enterprise.inject.spi.Decorator;
import javax.enterprise.inject.spi.InjectionTarget;
import javax.enterprise.inject.spi.Interceptor;

import org.glassfish.jersey.inject.cdi.se.bean.BeanHelper;
import org.glassfish.jersey.internal.inject.InjectionResolver;
import org.glassfish.jersey.internal.util.collection.LazyValue;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;

import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedConstructor;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedMethod;
import org.jboss.weld.annotated.enhanced.EnhancedAnnotatedType;
import org.jboss.weld.bean.CustomDecoratorWrapper;
import org.jboss.weld.bean.DecoratorImpl;
import org.jboss.weld.bean.proxy.ProxyInstantiator;
import org.jboss.weld.injection.producer.AbstractInstantiator;
import org.jboss.weld.injection.producer.BasicInjectionTarget;
import org.jboss.weld.injection.producer.ConstructorInterceptionInstantiator;
import org.jboss.weld.injection.producer.DefaultInstantiator;
import org.jboss.weld.injection.producer.Instantiator;
import org.jboss.weld.injection.producer.InterceptionModelInitializer;
import org.jboss.weld.injection.producer.InterceptorApplyingInstantiator;
import org.jboss.weld.injection.producer.SubclassDecoratorApplyingInstantiator;
import org.jboss.weld.injection.producer.SubclassedComponentInstantiator;
import org.jboss.weld.interceptor.spi.model.InterceptionModel;
import org.jboss.weld.logging.BeanLogger;
import org.jboss.weld.resources.ClassTransformer;
import org.jboss.weld.util.reflection.Formats;

/**
 * Wrapper for {@link InjectionTarget} that implements the functionality of injecting using JAX-RS annotations into provided
 * instances. {@code Delegate} is a original {@code InjectionTarget} which is able to inject other fields/parameters which
 * are managed by CDI.
 * <p>
 * Implementation is also able create with custom {@code jerseyConstructor} if it is provided. This functionality allows override
 * default instantiator and use the Jersey-specific one.
 *
 * @author Petr Bouda (petr.bouda at oracle.com)
 */
public class JerseyInjectionTarget<T> extends BasicInjectionTarget<T> {

    private final Bean<T> bean;
    private final Class<T> clazz;
    private final LazyValue<JerseyInstanceInjector<T>> injector;
    private final EnhancedAnnotatedType<T> enhancedAnnotatedType;
    private Collection<InjectionResolver> resolvers;

    /**
     * Creates a new injection target which is able to delegate an injection to {@code delegate injection target} and inject
     * the fields that are Jersey-specific. The resolvers must be set later on. CDI will select its own constructor.
     *
     * @param delegate CDI specific injection target.
     * @param clazz    class that will be scanned and injected.
     */
    public JerseyInjectionTarget(BasicInjectionTarget<T> delegate, Class<T> clazz) {
        this(delegate, delegate.getBean(), clazz, null);
    }

    /**
     * Creates a new injection target which is able to delegate an injection to {@code delegate injection target} and inject
     * the fields that are Jersey-specific. CDI will select its own constructor.
     *
     * @param delegate  CDI specific injection target.
     * @param bean      bean which this injection target belongs to.
     * @param clazz     class that will be scanned and injected.
     * @param resolvers all resolvers that can provide a valued for Jersey-specific injection.
     */
    public JerseyInjectionTarget(BasicInjectionTarget<T> delegate, Bean<T> bean, Class<T> clazz,
            Collection<InjectionResolver> resolvers) {
        this(BeanHelper.createEnhancedAnnotatedType(delegate), delegate, bean, clazz, resolvers, null);
    }

    /**
     * Creates a new injection target which is able to delegate an injection to {@code delegate injection target} and inject
     * the fields that are Jersey-specific. This method accepts custom instantiator, if the instantiator is {@code null}
     * default one is created.
     *
     * @param annotatedType resolved type of the registered bean.
     * @param delegate      CDI specific injection target.
     * @param bean          bean which this injection target belongs to.
     * @param clazz         class that will be scanned and injected.
     * @param resolvers     all resolvers that can provide a valued for Jersey-specific injection.
     * @param instantiator  default instantiator.
     */
    public JerseyInjectionTarget(EnhancedAnnotatedType<T> annotatedType, BasicInjectionTarget<T> delegate, Bean<T> bean,
            Class<T> clazz, Collection<InjectionResolver> resolvers, Instantiator<T> instantiator) {
        super(annotatedType,
                bean,
                delegate.getBeanManager(),
                delegate.getInjector(),
                delegate.getLifecycleCallbackInvoker(),
                instantiator);

        this.bean = bean;
        this.enhancedAnnotatedType = annotatedType;
        this.clazz = clazz;
        this.resolvers = resolvers;
        this.injector = Values.lazy((Value<JerseyInstanceInjector<T>>) () -> new JerseyInstanceInjector<>(bean, this.resolvers));
    }

    @Override
    public void inject(T instance, CreationalContext<T> ctx) {
        /*
         * If an instance contains any fields which be injected by Jersey then Jersey attempts to inject them using annotations
         * retrieves from registered InjectionResolvers.
         */
        try {
            injector.get().inject(instance);
        } catch (WebApplicationException wae) {
            throw wae;
        } catch (Throwable cause) {
            throw new InjectionException(
                    "Exception occurred during Jersey/JAX-RS annotations processing in the class: " + clazz, cause);
        }

        /*
         * The rest of the fields (annotated by @Inject) are injected using CDI.
         */
        super.inject(instance, ctx);
    }

    /**
     * Copied method from the parent class because of a custom type of {@link Instantiator} is used in this implementation.
     *
     * @param annotatedType processed class.
     */
    @Override
    public void initializeAfterBeanDiscovery(EnhancedAnnotatedType<T> annotatedType) {
        initializeInterceptionModel(annotatedType);

        InterceptionModel interceptionModel = null;
        if (isInterceptionCandidate()) {
            interceptionModel = beanManager.getInterceptorModelRegistry().get(getType());
        }
        boolean hasNonConstructorInterceptors = interceptionModel != null
                && (interceptionModel.hasExternalNonConstructorInterceptors()
                            || interceptionModel.hasTargetClassInterceptors());

        List<Decorator<?>> decorators = null;
        if (getBean() != null && isInterceptionCandidate()) {
            decorators = beanManager.resolveDecorators(getBean().getTypes(), getBean().getQualifiers());
        }
        boolean hasDecorators = decorators != null && !decorators.isEmpty();
        if (hasDecorators) {
            checkDecoratedMethods(annotatedType, decorators);
        }

        if (hasNonConstructorInterceptors || hasDecorators) {
            if (!(getInstantiator() instanceof DefaultInstantiator<?>)) {
                throw new IllegalStateException("Unexpected instantiator " + getInstantiator());
            }

            /*
             * Casting changed from DefaultInstantiator to a more abstract one because of using our custom JerseyInstantiator.
             */
            AbstractInstantiator<T> delegate = (AbstractInstantiator<T>) getInstantiator();
            setInstantiator(
                    SubclassedComponentInstantiator.forInterceptedDecoratedBean(annotatedType, getBean(), delegate, beanManager));

            if (hasDecorators) {
                setInstantiator(new SubclassDecoratorApplyingInstantiator<>(
                        getBeanManager().getContextId(), getInstantiator(), getBean(), decorators));
            }

            if (hasNonConstructorInterceptors) {
                setInstantiator(new InterceptorApplyingInstantiator<>(
                        getInstantiator(), interceptionModel, getType()));
            }
        }

        if (isInterceptionCandidate()) {
            setupConstructorInterceptionInstantiator(interceptionModel);
        }
    }

    private void setupConstructorInterceptionInstantiator(InterceptionModel interceptionModel) {
        if (interceptionModel != null && interceptionModel.hasExternalConstructorInterceptors()) {
            setInstantiator(new ConstructorInterceptionInstantiator<>(getInstantiator(), interceptionModel, getType()));
        }
    }

    private void checkNoArgsConstructor(EnhancedAnnotatedType<T> type) {
        if (!beanManager.getServices().get(ProxyInstantiator.class).isUsingConstructor()) {
            return;
        }
        EnhancedAnnotatedConstructor<T> constructor = type.getNoArgsEnhancedConstructor();
        if (constructor == null) {
            throw BeanLogger.LOG.decoratedHasNoNoargsConstructor(this);
        } else if (constructor.isPrivate()) {
            throw BeanLogger.LOG
                    .decoratedNoargsConstructorIsPrivate(this, Formats.formatAsStackTraceElement(constructor.getJavaMember()));
        }
    }

    private void checkDecoratedMethods(EnhancedAnnotatedType<T> type, List<Decorator<?>> decorators) {
        if (type.isFinal()) {
            throw BeanLogger.LOG.finalBeanClassWithDecoratorsNotAllowed(this);
        }
        checkNoArgsConstructor(type);
        for (Decorator<?> decorator : decorators) {
            EnhancedAnnotatedType<?> decoratorClass;
            if (decorator instanceof DecoratorImpl<?>) {
                DecoratorImpl<?> decoratorBean = (DecoratorImpl<?>) decorator;
                decoratorClass = decoratorBean.getBeanManager().getServices().get(ClassTransformer.class)
                        .getEnhancedAnnotatedType(decoratorBean.getAnnotated());
            } else if (decorator instanceof CustomDecoratorWrapper<?>) {
                decoratorClass = ((CustomDecoratorWrapper<?>) decorator).getEnhancedAnnotated();
            } else {
                throw BeanLogger.LOG.nonContainerDecorator(decorator);
            }

            for (EnhancedAnnotatedMethod<?, ?> decoratorMethod : decoratorClass.getEnhancedMethods()) {
                EnhancedAnnotatedMethod<?, ?> method = type.getEnhancedMethod(decoratorMethod.getSignature());
                if (method != null && !method.isStatic() && !method.isPrivate() && method.isFinal()) {
                    throw BeanLogger.LOG.finalBeanClassWithInterceptorsNotAllowed(this);
                }
            }
        }
    }

    private void initializeInterceptionModel(EnhancedAnnotatedType<T> annotatedType) {
        AbstractInstantiator<T> instantiator = (AbstractInstantiator<T>) getInstantiator();
        if (instantiator.getConstructorInjectionPoint() == null) {
            return; // this is a non-producible InjectionTarget (only created to inject existing instances)
        }
        if (isInterceptionCandidate() && !beanManager.getInterceptorModelRegistry().containsKey(getType())) {
            buildInterceptionModel(annotatedType, instantiator);
        }
    }

    private void buildInterceptionModel(EnhancedAnnotatedType<T> annotatedType, AbstractInstantiator<T> instantiator) {
        new InterceptionModelInitializer<>(beanManager, annotatedType, annotatedType.getDeclaredEnhancedConstructor(
                instantiator.getConstructorInjectionPoint().getSignature()), getBean()).init();
    }

    private boolean isInterceptor() {
        return (getBean() instanceof Interceptor<?>) || getType().isAnnotationPresent(javax.interceptor.Interceptor.class);
    }

    private boolean isDecorator() {
        return (getBean() instanceof Decorator<?>) || getType().isAnnotationPresent(javax.decorator.Decorator.class);
    }

    private boolean isInterceptionCandidate() {
        return !isInterceptor() && !isDecorator() && !Modifier.isAbstract(getType().getJavaClass().getModifiers());
    }

    @Override
    public Bean<T> getBean() {
        return this.bean;
    }

    /**
     * In some cases Injection Resolvers cannot be provided during th creation of the object therefore must be set later on.
     *
     * @param resolvers all registered injection resolvers.
     */
    public void setInjectionResolvers(Collection<InjectionResolver> resolvers) {
        this.resolvers = resolvers;
    }

    public EnhancedAnnotatedType<T> getEnhancedAnnotatedType() {
        return enhancedAnnotatedType;
    }
}
