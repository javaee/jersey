package org.glassfish.jersey.client.proxy;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;

/**
 * Default method invoker for {@link WebResourceFactory}
 */
public class JerseyResourceMethodInvoker implements ResourceMethodInvoker<Invocation.Builder> {

    @Override
    public <T> T method(Invocation.Builder builder, String name, GenericType<T> responseType) {
        return builder.method(name, responseType);
    }

    @Override
    public <T> T method(Invocation.Builder builder, String name, Entity<?> entity, GenericType<T> responseType) {
        return builder.method(name, entity, responseType);
    }

}
