package org.glassfish.jersey.server.spring.scope;

import org.glassfish.hk2.api.ServiceLocator;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.web.context.request.RequestContextHolder;

import javax.inject.Inject;
import javax.ws.rs.container.*;
import javax.ws.rs.ext.Provider;
import java.io.IOException;
import java.util.logging.Logger;

@Provider
@PreMatching
public class RequestContextFilter implements ContainerRequestFilter, ContainerResponseFilter {
    private static final Logger LOGGER = Logger.getLogger(RequestContextFilter.class.getName());
    private static final String REQUEST_ATTRIBUTES_PROPERTY =
            RequestContextFilter.class.getName() + ".REQUEST_ATTRIBUTES";
    private static boolean enabled;

    @Inject
    public RequestContextFilter(ServiceLocator locator) {
        // TODO: implement a better way to dynamically configure RequestContextFilter
        ApplicationContext ctx = locator.getService(ApplicationContext.class);
        if(ctx instanceof ClassPathXmlApplicationContext) {
            enabled = true;
        }
    }

    @Override
    public void filter(ContainerRequestContext requestContext) throws IOException {
        if(!enabled) {
            return;
        }
        JaxrsRequestAttributes attributes = new JaxrsRequestAttributes(requestContext);
        requestContext.setProperty(REQUEST_ATTRIBUTES_PROPERTY, attributes);
        RequestContextHolder.setRequestAttributes(attributes);
    }

    @Override
    public void filter(ContainerRequestContext requestContext, ContainerResponseContext responseContext) throws IOException {
        if(!enabled) {
            return;
        }
        JaxrsRequestAttributes attributes = (JaxrsRequestAttributes)requestContext.getProperty(REQUEST_ATTRIBUTES_PROPERTY);
        RequestContextHolder.resetRequestAttributes();
        attributes.requestCompleted();
    }
}
