package org.glassfish.jersey.server.spring;

import java.lang.reflect.AnnotatedElement;
import java.util.logging.Logger;

import javax.annotation.Resource;
import javax.inject.Singleton;

import org.glassfish.hk2.api.Injectee;
import org.glassfish.hk2.api.InjectionResolver;
import org.glassfish.hk2.api.ServiceHandle;
import org.springframework.beans.factory.NoSuchBeanDefinitionException;
import org.springframework.context.ApplicationContext;

/**
 * HK2 injection resolver for Spring framework {@link Resource} annotation injection.
 *
 * @author Olivier Billiard (obi at quartetfs.com)
 */
@Singleton
public class ResourceInjectResolver implements InjectionResolver<Resource> {
	private static final Logger LOGGER = Logger.getLogger(ResourceInjectResolver.class.getName());

	private volatile ApplicationContext ctx;

	/**
	 * Create a new instance.
	 *
	 * @param ctx
	 *            Spring application context.
	 */
	public ResourceInjectResolver(final ApplicationContext ctx) {
		this.ctx = ctx;
	}

	@Override
	public Object resolve(final Injectee injectee, final ServiceHandle<?> root) {
		final AnnotatedElement parent = injectee.getParent();
		String beanName = null;
		if (parent != null) {
			final Resource r = parent.getAnnotation(Resource.class);
			if (r != null) {
				beanName = r.name();
			}
		}

		try {
			return beanName != null ? ctx.getBean(beanName) : null;
		} catch (final NoSuchBeanDefinitionException e) {
			LOGGER.warning(e.getMessage());
			throw e;
		}
	}

	@Override
	public boolean isConstructorParameterIndicator() {
		return false;
	}

	@Override
	public boolean isMethodParameterIndicator() {
		return false;
	}
}
