package org.glassfish.jersey.client.proxy;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;

public interface ResourceMethodInvoker<B extends Invocation.Builder> {

    /**
     * Call corresponding method in specified {@link Invocation.Builder}
     *
     * @param <T> generic response entity type.
     * @param builder invocation builder to trigger
     * @param name method name
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @return invocation response.
     *
     * @see javax.ws.rs.client.SyncInvoker#method(String, GenericType)
     */
    <T> T method(B builder, String name, GenericType<T> responseType);

    /**
     * Call corresponding method in specified {@link Invocation.Builder}
     *
     * @param <T> generic response entity type.
     * @param builder invocation builder to trigger
     * @param name method name
     * @param entity request entity.
     * @param responseType representation of a generic Java type the response entity will be converted to.
     * @return invocation response.
     *
     * @see javax.ws.rs.client.SyncInvoker#method(String, Entity, GenericType)
     */
    <T> T method(B builder, String name, Entity<?> entity, GenericType<T> responseType);
}


