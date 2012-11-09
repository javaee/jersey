package org.glassfish.jersey.server.model;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.Set;

import javax.inject.Singleton;

import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.spi.Errors;

import org.glassfish.hk2.api.PerLookup;

/**
 * Validator ensuring that {@link Invocable invocable} and {@link HandlerConstructor constructor} is correctly defined (for
 * example correctly annotated with scope annotation). This validator is stateful and therefore new instance must be created
 * for each resource model validation.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
class InvocableValidator extends AbstractResourceModelVisitor {
    private static final Set<Class<?>> SCOPE_ANNOTATIONS = getScopeAnnotations();
    /**
     * Classes that have been checked already.
     */
    protected final Set<Class<?>> checkedClasses = new HashSet<Class<?>>();

    private static Set<Class<?>> getScopeAnnotations() {
        Set<Class<?>> scopeAnnotations = new HashSet<Class<?>>();
        scopeAnnotations.add(Singleton.class);
        scopeAnnotations.add(PerLookup.class);
        return scopeAnnotations;
    }

    @Override
    public void visitInvocable(final Invocable invocable) {
        // TODO: check invocable.
        Class resClass = invocable.getHandler().getHandlerClass();
        if (resClass != null && !checkedClasses.contains(resClass)) {
            checkedClasses.add(resClass);
            final boolean provider = Providers.isProvider(resClass);
            int counter = 0;
            for (Annotation annotation : resClass.getAnnotations()) {
                if (SCOPE_ANNOTATIONS.contains(annotation.annotationType())) {
                    counter++;
                }
            }
            if (counter == 0 && provider) {
                Errors.warning(resClass, LocalizationMessages.RESOURCE_IMPLEMENTS_PROVIDER(resClass,
                        Providers.getProviderContracts(resClass)));
            } else if (counter > 1) {
                Errors.fatal(resClass, LocalizationMessages.RESOURCE_MULTIPLE_SCOPE_ANNOTATIONS(resClass));
            }
        }


    }

    /**
     * Check if the resource class is declared to be a singleton.
     *
     * @param resourceClass resource class.
     * @return {@code true} if the resource class is a singleton, {@code false} otherwise.
     */
    public static boolean isSingleton(Class<?> resourceClass) {
        return resourceClass.isAnnotationPresent(Singleton.class)
                || (Providers.isProvider(resourceClass) && !resourceClass.isAnnotationPresent(PerLookup.class));
    }


    @Override
    public void visitResourceHandlerConstructor(final HandlerConstructor constructor) {
        Class<?> resClass = constructor.getConstructor().getDeclaringClass();
        boolean isSingleton = isSingleton(resClass);
        int paramCount = 0;
        for (Parameter p : constructor.getParameters()) {
            ResourceMethodValidator.validateParameter(p, constructor.getConstructor(), constructor.getConstructor()
                    .toGenericString(),
                    Integer.toString(++paramCount), isSingleton);
        }
    }


    // TODO: validate also method handler.


}
