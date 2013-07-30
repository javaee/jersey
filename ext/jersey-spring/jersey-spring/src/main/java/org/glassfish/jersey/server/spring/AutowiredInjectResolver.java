package org.glassfish.jersey.server.spring;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.ApplicationContext;

import javax.inject.Singleton;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.logging.Logger;

/**
 * InjectionResolver class for Spring framework Autowired annotation injection.
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
        LOGGER.finer("resolve: " + injectee);
        AnnotatedElement parent = injectee.getParent();
        String beanName = null;
        if(parent != null) {
            Qualifier an = parent.getAnnotation(Qualifier.class);
            if(an != null) {
                beanName = an.value();
            }
        }
        return getBeanFromSpringContext(beanName, injectee.getRequiredType());
    }

    private Object getBeanFromSpringContext(String beanName, Type beanType) {
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
