package org.glassfish.jersey.server.spring;

import org.springframework.context.ApplicationContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

/**
 * Helper class for injection resolvers.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class InjectResolverHelper {
    private static final Logger LOGGER = Logger.getLogger(InjectResolverHelper.class.getName());
    private ApplicationContext ctx;

    public InjectResolverHelper(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    Object getBeanFromSpringContext(String beanName, Type beanType) {
        Class<?> bt = getClassFromType(beanType);
        if(beanName != null) {
            return ctx.getBean(beanName, bt);
        }
        Map<String, ?> beans = ctx.getBeansOfType(bt);
        if(beans == null || beans.size() != 1) {
            LOGGER.warning("no beans found, resolve failed for type "+beanType);
            return null;
        }
        Object o = beans.values().iterator().next();
        LOGGER.finer("resolve: "+o);
        return o;
    }

    private Class<?> getClassFromType(Type type) {
        if (type instanceof Class) return (Class<?>) type;
        if (type instanceof ParameterizedType) {
            ParameterizedType pt = (ParameterizedType) type;

            return (Class<?>) pt.getRawType();
        }

        return null;
    }
}
