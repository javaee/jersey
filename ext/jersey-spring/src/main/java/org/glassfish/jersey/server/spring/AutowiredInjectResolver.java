package org.glassfish.jersey.server.spring;

import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

import javax.inject.Singleton;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;

/**
*InjectionResolver class for Spring framework Autowired annotation injection.
*
* @author Marko Asplund (marko.asplund at gmail.com)
*/
@Singleton
public class AutowiredInjectResolver implements InjectionResolver<Autowired> {
    private static final Logger LOGGER = Logger.getLogger(AutowiredInjectResolver.class.getName());
    private ApplicationContext ctx;

    public AutowiredInjectResolver(ApplicationContext ctx) {
        LOGGER.info("AutowiredInjectResolver()");
        this.ctx = ctx;
    }
    
    @Override
    public Object resolve(Injectee injectee, ServiceHandle<?> root) {
        LOGGER.info("resolve: "+injectee);
        Type t = injectee.getRequiredType();
        if(t instanceof Class) {
            Map<String, ?> beans = ctx.getBeansOfType((Class<?>)t);
            if(!beans.values().isEmpty()) {
                Object o = beans.values().iterator().next();
                LOGGER.info("resolve: "+o);
                return o;
            }
            LOGGER.info("no beans found, resolve failed");
        } else {
            LOGGER.info("unable to resolve, injectee type not class");
        }
        return null;
    }

    @Override
    public boolean isConstructorParameterIndicator() {
        LOGGER.info("isConstructorParameterIndicator");
        return false;
    }

    @Override
    public boolean isMethodParameterIndicator() {
        LOGGER.info("isMethodParameterIndicator");
        return false;
    }

}
