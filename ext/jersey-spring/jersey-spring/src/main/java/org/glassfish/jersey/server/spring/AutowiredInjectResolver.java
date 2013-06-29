package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

import javax.inject.Singleton;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

/**
 *InjectionResolver class for Spring framework Autowired annotation injection.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
@Singleton
public class AutowiredInjectResolver implements InjectionResolver<Autowired> {
    private static final Logger LOGGER = Logger.getLogger(AutowiredInjectResolver.class.getName());
    private ApplicationContext ctx;

    public AutowiredInjectResolver(ApplicationContext ctx) {
        LOGGER.fine("AutowiredInjectResolver()");
        this.ctx = ctx;
    }

    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        LOGGER.finer("resolve: "+injectee);
        Type t = injectee.getRequiredType();
        if(t instanceof Class) {
            Map<String, ?> beans = ctx.getBeansOfType((Class<?>)t);
            if(!beans.values().isEmpty()) {
                Object o = beans.values().iterator().next();
                LOGGER.finer("resolve: "+o);
                return o;
            }
            LOGGER.info("no beans found, resolve failed for type "+t);
        } else {
            LOGGER.warning("unable to resolve, injectee type not class");
        }
        return null;
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
