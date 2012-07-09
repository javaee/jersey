package org.glassfish.jersey.internal.inject;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.HashSet;
import java.util.Set;

import org.glassfish.hk2.api.Descriptor;
import org.glassfish.hk2.api.DynamicConfiguration;
import org.glassfish.hk2.api.DynamicConfigurationService;
import org.glassfish.hk2.api.Factory;
import org.glassfish.hk2.api.FactoryDescriptors;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.ServiceLocatorFactory;
import org.glassfish.hk2.extension.ServiceLocatorGenerator;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.ActiveDescriptorBuilder;
import org.glassfish.hk2.utilities.BuilderHelper;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;
import org.glassfish.hk2.utilities.reflection.ParameterizedTypeImpl;

import org.jvnet.hk2.external.generator.ServiceLocatorGeneratorImpl;

/**
 * HK2 binding utilites.
 *
 * @author Tom Beerbower
 */
public class Utilities {
    private final static ServiceLocatorGenerator generator = new ServiceLocatorGeneratorImpl();
    private final static ServiceLocatorFactory factory = ServiceLocatorFactory.getInstance();

    /**
     * Create a {@link ServiceLocator}.
     *
     * @param name    the name of the service locator to create
     * @param parent  the parent locator this one should have; may be null
     * @param modules the modules
     * @return a service locator with all the bindings
     */
    public static ServiceLocator create(String name, ServiceLocator parent, Module... modules) {

        ServiceLocator locator = factory.create(name, parent, generator);

        ServiceLocatorUtilities.enablePerThreadScope(locator);

        for (Module module : modules) {
            bindModule(locator, module);
        }

        return locator;
    }

    private static void bindModule(ServiceLocator locator, Module module) {
        DynamicConfigurationService dcs = locator.getService(DynamicConfigurationService.class);
        DynamicConfiguration dc = dcs.createDynamicConfiguration();

        locator.inject(module);
        module.bind(dc);

        dc.commit();
    }

    public static <T> T getOrCreateComponent(ServiceLocator serviceLocator, Class<T> clazz) {
        T component = serviceLocator.getService(clazz);
        return component == null ? serviceLocator.createAndInitialize(clazz) : component;
    }

    public static <T> AbstractActiveDescriptor<T> createActiveDescriptor(
            Class<T> implClass,
            Class<? extends Annotation> scope,
            String name,
            Set<Annotation> qualifiers,
            Type... types) {
        if (qualifiers == null) {
            qualifiers = new HashSet<Annotation>();
        }
        ActiveDescriptorBuilder builder = BuilderHelper.activeLink(implClass).in(scope).named(name);

        for (Annotation annotation : qualifiers) {
            builder.qualifiedBy(annotation);
        }

        for (Type type : types) {
            builder.to(type);
        }

        return (AbstractActiveDescriptor<T>) builder.build();
    }

    public static <T> FactoryDescriptors createFactoryDescriptor(
            Class<T> implClass,
            Class<? extends Annotation> scope,
            Class<? extends Annotation> factoryScope,
            String name,
            Annotation annotation,
            Type... types) {

        ActiveDescriptorBuilder sBuilder = BuilderHelper.activeLink(implClass);
        ActiveDescriptorBuilder fBuilder = BuilderHelper.activeLink(implClass);

        if (scope != null) {
            sBuilder.in(scope);
        }

        if (factoryScope != null) {
            fBuilder.in(factoryScope);
        }

        if (name != null) {
            fBuilder.named(name);
            sBuilder.named(name);
        }

        if (annotation != null) {
            fBuilder.qualifiedBy(annotation);
        }

        sBuilder.to(implClass);

        for (Type type : types) {
            sBuilder.to(new ParameterizedTypeImpl(Factory.class, type));
            fBuilder.to(type);
        }

        return new FactoryDescriptorsImpl(
                sBuilder.build(),
                fBuilder.buildFactory());
    }

    public static <T> FactoryDescriptors createConstantFactoryDescriptor(
            Factory<?> factory,
            Class<? extends Annotation> scope,
            Class<? extends Annotation> factoryScope, String name, Annotation annotation,
            Type... types) {

        AbstractActiveDescriptor sDescriptor = BuilderHelper.createConstantDescriptor(factory);
        ActiveDescriptorBuilder fBuilder = BuilderHelper.activeLink(factory.getClass());

        if (scope != null) {
            fBuilder.in(scope);
        }

        if (factoryScope != null) {
            sDescriptor.setScope(factoryScope.getName());
        }

        if (name != null) {
            fBuilder.named(name);
            sDescriptor.setName(name);
        }

        if (annotation != null) {
            fBuilder.qualifiedBy(annotation);
        }

        sDescriptor.addContractType(factory.getClass());

        for (Type type : types) {
            fBuilder.to(type);
            sDescriptor.addContractType(new ParameterizedTypeImpl(Factory.class, type));
        }

        return new FactoryDescriptorsImpl(
                sDescriptor,
                fBuilder.buildFactory());
    }

    public static class FactoryDescriptorsImpl implements FactoryDescriptors {
        private final Descriptor serviceDescriptor;
        private final Descriptor factoryDescriptor;

        public FactoryDescriptorsImpl(Descriptor serviceDescriptor, Descriptor factoryDescriptor) {
            this.serviceDescriptor = serviceDescriptor;
            this.factoryDescriptor = factoryDescriptor;
        }

        @Override
        public Descriptor getFactoryAsAService() {
            return serviceDescriptor;
        }

        @Override
        public Descriptor getFactoryAsAFactory() {
            return factoryDescriptor;
        }

        public String toString() {
            return "FactoryDescriptorsImpl(\n" +
                    serviceDescriptor + ",\n" + factoryDescriptor + ",\n\t" + System.identityHashCode(this) + ")";
        }
    }

}
