package org.glassfish.jersey.client.proxy;

import javax.ws.rs.client.Invocation;
import java.lang.reflect.Method;

/**
 * WebResourceInvocationCallback provides a scope in which
 * both the request context and the dispatch class are available.
 *
 * This allows the developer to perform custom reflection processing at
 * dispatch time. i.e. you can set properties on the builder based on annotations
 * from the API interface.
 */
public interface WebResourceInvocationCallback {
    /**
     * beforeInvocation is called just prior to {@link javax.ws.rs.client.Invocation.Builder#method(String)}.
     * All headers, parameter parsing, cookie setting etc have been handled and can be referenced/modified.
     *
     * @param builder Builder for the invocation which is about to initiate the client side write filter chain.
     * @param proxy Proxy object created by {@link WebResourceFactory}
     * @param method Method invoked for this execution.
     * @param args Args passed to the invocation for this execution
     */
    void beforeInvocation(Invocation.Builder builder, final Object proxy, final Method method, final Object[] args);
}
