package org.glassfish.jersey.server.spring.scope;

import org.springframework.util.StringUtils;
import org.springframework.web.context.request.AbstractRequestAttributes;

import javax.ws.rs.container.ContainerRequestContext;
import java.util.logging.Logger;

/**
 * JAX-RS based Spring RequestAttributes implementation.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class JaxrsRequestAttributes extends AbstractRequestAttributes {
    private static final Logger LOGGER = Logger.getLogger(JaxrsRequestAttributes.class.getName());
    private ContainerRequestContext requestContext;

    public JaxrsRequestAttributes(ContainerRequestContext requestContext) {
        this.requestContext = requestContext;
    }

    @Override
    protected void updateAccessedSessionAttributes() {
        // sessions not supported
    }

    @Override
    public Object getAttribute(String name, int scope) {
            return requestContext.getProperty(name);
    }

    @Override
    public void setAttribute(String name, Object value, int scope) {
        requestContext.setProperty(name, value);
    }

    @Override
    public void removeAttribute(String name, int scope) {
        requestContext.removeProperty(name);
    }

    @Override
    public String[] getAttributeNames(int scope) {
        if (!isRequestActive()) {
            throw new IllegalStateException("Cannot ask for request attributes - request is not active anymore!");
        }
        return StringUtils.toStringArray(requestContext.getPropertyNames());
    }

    @Override
    public void registerDestructionCallback(String name, Runnable callback, int scope) {
        registerRequestDestructionCallback(name, callback);
    }

    @Override
    public Object resolveReference(String key) {
        if (REFERENCE_REQUEST.equals(key)) {
            return requestContext;
        }
        return null;
    }

    @Override
    public String getSessionId() {
        return null;
    }

    @Override
    public Object getSessionMutex() {
        return null;
    }
}
