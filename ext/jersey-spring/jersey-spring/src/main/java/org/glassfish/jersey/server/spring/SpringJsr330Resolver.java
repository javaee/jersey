package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.*;
import org.springframework.context.ApplicationContext;

import javax.inject.Inject;
import javax.inject.Named;
import javax.inject.Singleton;
import java.util.logging.Logger;

/**
 * Custom JSR 330 InjectionResolver class for @Inject annotation injection.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
@Singleton
public class SpringJsr330Resolver implements InjectionResolver<Inject> {
    private static final Logger LOGGER = Logger.getLogger(SpringJsr330Resolver.class.getName());

    @Inject @Named(InjectionResolver.SYSTEM_RESOLVER_NAME)
    private InjectionResolver<Inject> systemResolver;

    private InjectResolverHelper injectResolverHelper;


    public SpringJsr330Resolver(ApplicationContext ctx) {
        injectResolverHelper = new InjectResolverHelper(ctx);
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        LOGGER.fine("resolve: "+injectee);

        Object o = null;
        try {
            o = systemResolver.resolve(injectee, root);
            if(o != null) {
                return o;
            }
        } catch (MultiException ex) {
            if(!(ex.getErrors().iterator().next() instanceof UnsatisfiedDependencyException)) {
                throw ex;
            }
        }

        return injectResolverHelper.getBeanFromSpringContext(injectee.getRequiredType());
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        return systemResolver.isConstructorParameterIndicator();
    }

    @Override
    public boolean isMethodParameterIndicator() {
        return systemResolver.isMethodParameterIndicator();
    }

}
