package org.glassfish.jersey.client.proxy;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.Invocation;
import javax.ws.rs.core.GenericType;

public interface ResourceMethodInvoker<B extends Invocation.Builder> {

    <T> T method(B builder, String name, GenericType<T> responseType);

    <T> T method(B builder, String name, Entity<?> entity, GenericType<T> responseType);
}


