package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.inject.Singleton;
import java.util.logging.Logger;

/**
 * InjectionResolver class for Spring framework Autowired annotation injection.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
@Singleton
public class AutowiredInjectResolver implements InjectionResolver<Autowired> {
    private static final Logger LOGGER = Logger.getLogger(AutowiredInjectResolver.class.getName());
    private InjectResolverHelper injectResolverHelper;

    public AutowiredInjectResolver(ApplicationContext ctx) {
        LOGGER.fine("AutowiredInjectResolver()");
        injectResolverHelper = new InjectResolverHelper(ctx);
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        LOGGER.finer("resolve: "+injectee);
        return injectResolverHelper.getBeanFromSpringContext(injectee.getRequiredType());
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        LOGGER.finer("isConstructorParameterIndicator");
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        LOGGER.finer("isMethodParameterIndicator");
        return false;
    }

}
