/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2011-2012 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */
package org.glassfish.jersey.server.model;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.UndeclaredThrowableException;
import javax.ws.rs.core.Request;
import javax.ws.rs.core.Response;

import org.glassfish.jersey.internal.MappableException;
import org.glassfish.jersey.internal.ProcessingException;
import org.glassfish.jersey.server.spi.internal.ResourceMethodDispatcher;

/**
 * Abstract resource method dispatcher that provides skeleton implementation of
 * dispatching requests to a particular {@link Method Java method} using supplied
 * {@link InvocationHandler Java method invocation handler}.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
abstract class AbstractJavaResourceMethodDispatcher implements ResourceMethodDispatcher {

    private final Method method;
    private final InvocationHandler methodHandler;

    /**
     * Initialize common java resource method dispatcher structures.
     *
     * @param resourceMethod invocable resource class Java method.
     * @param methodHandler method invocation handler.
     */
    AbstractJavaResourceMethodDispatcher(Invocable resourceMethod, InvocationHandler methodHandler) {
        this.method = resourceMethod.getHandlingMethod();
        this.methodHandler = methodHandler;
    }

    @Override
    public final Response dispatch(Object resource, Request request) throws ProcessingException {
        final Response response = doDispatch(resource, request);

        // TODO set the annotations if no exception is thrown and there is a response entity
        // if (response != null) {
        //     responseContext.setResponseAnnotations(method.getDeclaredAnnotations());
        // }
        return response;
    }

    /**
     * Dispatching functionality to be implemented by a concrete dispatcher
     * implementation sub-class.
     *
     * @param resource resource class instance.
     * @param request request to be dispatched.
     * @return response for the dispatched request.
     * @throws ProcessingException in case of a processing error.
     *
     * @see ResourceMethodDispatcher#dispatch(java.lang.Object, javax.ws.rs.core.Request)
     */
    protected abstract Response doDispatch(Object resource, Request request) throws ProcessingException;

    /**
     * Use the underlying invocation handler to invoke the underlying Java method
     * with the supplied input method argument values on a given resource instance.
     *
     * @param resource resource class instance.
     * @param args input argument values for the invoked Java method.
     * @return invocation result.
     * @throws ProcessingException (possibly {@link MappableException mappable})
     *     container exception in case the invocation failed.
     */
    final Object invoke(Object resource, Object... args) throws ProcessingException {
        try {
            return methodHandler.invoke(resource, method, args);
        } catch (IllegalAccessException ex) {
            throw new ProcessingException("Resource Java method invocation error.", ex);
        } catch (InvocationTargetException ex) {
            final Throwable cause = ex.getCause();
            if (cause instanceof ProcessingException) {
                throw (ProcessingException) cause;
            }
            // exception cause potentially mappable
            throw new MappableException(cause);
        } catch (UndeclaredThrowableException ex) {
            throw new ProcessingException("Resource Java method invocation error.", ex);
        } catch (ProcessingException ex) {
            throw ex;
        } catch (Exception ex) {
            // exception potentially mappable
            throw new MappableException(ex);
        } catch (Throwable t) {
            throw new ProcessingException(t);
        }
    }

    @Override
    public String toString() {
        return method.toString();
    }
}
