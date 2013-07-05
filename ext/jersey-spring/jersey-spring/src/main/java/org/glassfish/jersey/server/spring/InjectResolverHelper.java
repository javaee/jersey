package org.glassfish.jersey.server.spring;

import org.springframework.context.ApplicationContext;

import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

public class InjectResolverHelper {
    private static final Logger LOGGER = Logger.getLogger(InjectResolverHelper.class.getName());
    private ApplicationContext ctx;

    public InjectResolverHelper(ApplicationContext ctx) {
        this.ctx = ctx;
    }

    Object getBeanFromSpringContext(Type beanType) {
        Map<String, ?> beans = ctx.getBeansOfType(getClassFromType(beanType));
        if(!beans.values().isEmpty()) {
            Object o = beans.values().iterator().next();
            LOGGER.finer("resolve: "+o);
            return o;
        }
        LOGGER.info("no beans found, resolve failed for type "+beanType);
        return null;
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
