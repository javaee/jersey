/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.integration.servlet_request_wrapper_binding2;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.UnsupportedEncodingException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.security.Principal;
import java.util.Collection;
import java.util.Enumeration;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import javax.ws.rs.core.GenericType;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.AsyncContext;
import javax.servlet.DispatcherType;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletOutputStream;
import javax.servlet.ServletRequest;
import javax.servlet.ServletResponse;
import javax.servlet.http.Cookie;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletRequestWrapper;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.http.HttpSession;
import javax.servlet.http.Part;

import org.glassfish.jersey.inject.hk2.DelayedHk2InjectionManager;
import org.glassfish.jersey.inject.hk2.ImmediateHk2InjectionManager;
import org.glassfish.jersey.internal.inject.AbstractBinder;
import org.glassfish.jersey.internal.inject.InjectionManager;
import org.glassfish.jersey.internal.inject.ReferencingFactory;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.process.internal.RequestScoped;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.ComponentProvider;
import org.glassfish.jersey.server.spi.RequestScopedInitializer;
import org.glassfish.jersey.servlet.internal.spi.NoOpServletContainerProvider;
import org.glassfish.jersey.servlet.internal.spi.RequestContextProvider;
import org.glassfish.jersey.servlet.internal.spi.RequestScopedInitializerProvider;

import org.glassfish.hk2.api.DescriptorType;
import org.glassfish.hk2.api.DescriptorVisibility;
import org.glassfish.hk2.api.PerLookup;
import org.glassfish.hk2.api.ServiceHandle;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.hk2.api.TypeLiteral;
import org.glassfish.hk2.utilities.AbstractActiveDescriptor;
import org.glassfish.hk2.utilities.ServiceLocatorUtilities;

import org.jvnet.hk2.internal.ServiceHandleImpl;

/**
 * Servlet container provider that wraps the original Servlet request/response.
 * The request wrapper contains a direct reference to the underlying container request
 * in case it gets injected into a request scoped component.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class RequestResponseWrapperProvider extends NoOpServletContainerProvider {

    private final Type REQUEST_TYPE = (new TypeLiteral<Ref<HttpServletRequestWrapper>>() {
    }).getType();
    private final Type RESPONSE_TYPE = (new TypeLiteral<Ref<HttpServletResponseWrapper>>() {
    }).getType();

    public static class DescriptorProvider implements ComponentProvider {

        @Override
        public void initialize(InjectionManager injectionManager) {
            ServiceLocator locator = getServiceLocator(injectionManager);
            ServiceLocatorUtilities.addOneDescriptor(locator, new HttpServletRequestDescriptor(locator));
        }

        @Override
        public boolean bind(Class<?> component, Set<Class<?>> providerContracts) {
            return false;
        }

        @Override
        public void done() {
            // nop
        }
    }

    /**
     * Subclass standard wrapper so that we make 100 % sure we are getting the right type.
     * It is also final, i.e. not proxiable, which we workaround by using custom http servlet request impl.
     */
    public static final class RequestWrapper extends HttpServletRequestWrapper {

        public RequestWrapper(HttpServletRequest request) {
            super(request);
        }
    }

    /**
     * Subclass standard wrapper so that we make 100 % sure we are getting the right type.
     * It is also final, i.e. not proxiable, which we workaround by using custom http servlet response impl.
     */
    public static final class ResponseWrapper extends HttpServletResponseWrapper {

        public ResponseWrapper(HttpServletResponse response) {
            super(response);
        }
    }

    @Override
    public boolean bindsServletRequestResponse() {
        return true;
    }

    @Override
    public RequestScopedInitializerProvider getRequestScopedInitializerProvider() {
        return new RequestScopedInitializerProvider() {

            @Override
            public RequestScopedInitializer get(final RequestContextProvider context) {
                return new RequestScopedInitializer() {
                    @Override
                    public void initialize(InjectionManager injectionManager) {
                        ServiceLocator locator = getServiceLocator(injectionManager);
                        locator.<Ref<HttpServletRequest>>getService(REQUEST_TYPE)
                                .set(finalWrap(context.getHttpServletRequest()));
                        locator.<Ref<HttpServletResponse>>getService(RESPONSE_TYPE)
                                .set(finalWrap(context.getHttpServletResponse()));
                    }
                };
            }
        };
    }

    private final class Binder extends AbstractBinder {

        @Override
        protected void configure() {

            bindFactory(HttpServletRequestReferencingFactory.class)
                    .to(HttpServletRequestWrapper.class).in(RequestScoped.class);

            bindFactory(ReferencingFactory.<HttpServletRequestWrapper>referenceFactory())
                    .to(new GenericType<Ref<HttpServletRequestWrapper>>() {
                    }).in(RequestScoped.class);

            bindFactory(HttpServletResponseFactory.class).to(HttpServletResponse.class);

            bindFactory(HttpServletResponseReferencingFactory.class)
                    .to(HttpServletResponseWrapper.class).in(RequestScoped.class);

            bindFactory(ReferencingFactory.<HttpServletResponseWrapper>referenceFactory())
                    .to(new GenericType<Ref<HttpServletResponseWrapper>>() {
                    }).in(RequestScoped.class);

        }
    }

    private static class HttpServletRequestDescriptor extends AbstractActiveDescriptor<HttpServletRequest> {

        static Set<Type> advertisedContracts = new HashSet<Type>() {
            {
                add(HttpServletRequest.class);
            }
        };

        final ServiceLocator locator;
        volatile javax.inject.Provider<Ref<HttpServletRequestWrapper>> request;

        public HttpServletRequestDescriptor(final ServiceLocator locator) {
            super(advertisedContracts,
                    PerLookup.class,
                    null, new HashSet<Annotation>(),
                    DescriptorType.CLASS, DescriptorVisibility.LOCAL,
                    0, null, null, null, null);
            this.locator = locator;
        }

        @Override
        public Class<?> getImplementationClass() {
            return HttpServletRequest.class;
        }

        @Override
        public Type getImplementationType() {
            return getImplementationClass();
        }

        @Override
        public synchronized String getImplementation() {
            return HttpServletRequest.class.getName();
        }

        @Override
        public HttpServletRequest create(ServiceHandle<?> serviceHandle) {
            if (request == null) {
                request = locator.getService(new TypeLiteral<Provider<Ref<HttpServletRequestWrapper>>>() {
                }.getType());
            }

            boolean direct = false;

            if (serviceHandle instanceof ServiceHandleImpl) {
                final ServiceHandleImpl serviceHandleImpl = (ServiceHandleImpl) serviceHandle;
                final Class<? extends Annotation> scopeAnnotation =
                        serviceHandleImpl.getOriginalRequest().getInjecteeDescriptor().getScopeAnnotation();

                if (scopeAnnotation == RequestScoped.class || scopeAnnotation == null) {
                    direct = true;
                }
            }


            return !direct ? new HttpServletRequestWrapper(new MyHttpServletRequestImpl() {
                @Override
                HttpServletRequest getHttpServletRequest() {
                    return request.get().get();
                }
            }) {
                @Override
                public ServletRequest getRequest() {
                    return request.get().get();
                }

            }
                    : new HttpServletRequestWrapper(request.get().get());
        }
    }

    private static class HttpServletResponseFactory implements Supplier<HttpServletResponse> {
        private final javax.inject.Provider<Ref<HttpServletResponseWrapper>> response;

        @Inject
        public HttpServletResponseFactory(javax.inject.Provider<Ref<HttpServletResponseWrapper>> response) {
            this.response = response;
        }

        @Override
        @PerLookup
        public HttpServletResponse get() {
            return new HttpServletResponseWrapper(new HttpServletResponse() {

                private HttpServletResponse getHttpServletResponse() {
                    return response.get().get();
                }

                @Override
                public void addCookie(Cookie cookie) {
                    getHttpServletResponse().addCookie(cookie);
                }

                @Override
                public boolean containsHeader(String s) {
                    return getHttpServletResponse().containsHeader(s);
                }

                @Override
                public String encodeURL(String s) {
                    return getHttpServletResponse().encodeURL(s);
                }

                @Override
                public String encodeRedirectURL(String s) {
                    return getHttpServletResponse().encodeRedirectURL(s);
                }

                @Override
                public String encodeUrl(String s) {
                    return getHttpServletResponse().encodeUrl(s);
                }

                @Override
                public String encodeRedirectUrl(String s) {
                    return getHttpServletResponse().encodeRedirectUrl(s);
                }

                @Override
                public void sendError(int i, String s) throws IOException {
                    getHttpServletResponse().sendError(i, s);
                }

                @Override
                public void sendError(int i) throws IOException {
                    getHttpServletResponse().sendError(i);
                }

                @Override
                public void sendRedirect(String s) throws IOException {
                    getHttpServletResponse().sendRedirect(s);
                }

                @Override
                public void setDateHeader(String s, long l) {
                    getHttpServletResponse().setDateHeader(s, l);
                }

                @Override
                public void addDateHeader(String s, long l) {
                    getHttpServletResponse().addDateHeader(s, l);
                }

                @Override
                public void setHeader(String h, String v) {
                    getHttpServletResponse().setHeader(h, v);
                }

                public Collection<String> getHeaderNames() {
                    return getHttpServletResponse().getHeaderNames();
                }

                public Collection<String> getHeaders(String s) {
                    return getHttpServletResponse().getHeaders(s);
                }

                public String getHeader(String s) {
                    return getHttpServletResponse().getHeader(s);
                }

                @Override
                public void addHeader(String h, String v) {
                    getHttpServletResponse().addHeader(h, v);
                }

                @Override
                public void setIntHeader(String s, int i) {
                    getHttpServletResponse().setIntHeader(s, i);
                }

                @Override
                public void addIntHeader(String s, int i) {
                    getHttpServletResponse().addIntHeader(s, i);
                }

                @Override
                public void setStatus(int i) {
                    getHttpServletResponse().setStatus(i);
                }

                @Override
                public int getStatus() {
                    return getHttpServletResponse().getStatus();
                }

                @Override
                public void setStatus(int i, String s) {
                    getHttpServletResponse().setStatus(i, s);
                }

                @Override
                public String getCharacterEncoding() {
                    return getHttpServletResponse().getCharacterEncoding();
                }

                @Override
                public String getContentType() {
                    return getHttpServletResponse().getContentType();
                }

                @Override
                public ServletOutputStream getOutputStream() throws IOException {
                    return getHttpServletResponse().getOutputStream();
                }

                @Override
                public PrintWriter getWriter() throws IOException {
                    return getHttpServletResponse().getWriter();
                }

                @Override
                public void setCharacterEncoding(String s) {
                    getHttpServletResponse().setCharacterEncoding(s);
                }

                @Override
                public void setContentLength(int i) {
                    getHttpServletResponse().setContentLength(i);
                }

                @Override
                public void setContentType(String s) {
                    getHttpServletResponse().setContentType(s);
                }

                @Override
                public void setBufferSize(int i) {
                    getHttpServletResponse().setBufferSize(i);
                }

                @Override
                public int getBufferSize() {
                    return getHttpServletResponse().getBufferSize();
                }

                @Override
                public void flushBuffer() throws IOException {
                    getHttpServletResponse().flushBuffer();
                }

                @Override
                public void resetBuffer() {
                    getHttpServletResponse().resetBuffer();
                }

                @Override
                public boolean isCommitted() {
                    return getHttpServletResponse().isCommitted();
                }

                @Override
                public void reset() {
                    getHttpServletResponse().reset();
                }

                @Override
                public void setLocale(Locale locale) {
                    getHttpServletResponse().setLocale(locale);
                }

                @Override
                public Locale getLocale() {
                    return getHttpServletResponse().getLocale();
                }
            }
            ) {
                @Override
                public ServletResponse getResponse() {
                    return response.get().get();
                }
            };
        }

    }

    @SuppressWarnings("JavaDoc")
    private static class HttpServletRequestReferencingFactory extends ReferencingFactory<HttpServletRequestWrapper> {

        @Inject
        public HttpServletRequestReferencingFactory(
                final javax.inject.Provider<Ref<HttpServletRequestWrapper>> referenceFactory) {

            super(referenceFactory);
        }
    }

    @SuppressWarnings("JavaDoc")
    private static class HttpServletResponseReferencingFactory extends ReferencingFactory<HttpServletResponseWrapper> {

        @Inject
        public HttpServletResponseReferencingFactory(
                final javax.inject.Provider<Ref<HttpServletResponseWrapper>> referenceFactory) {

            super(referenceFactory);
        }

    }

    @Override
    public void configure(final ResourceConfig resourceConfig) throws ServletException {
        resourceConfig.register(new Binder());
    }

    private HttpServletRequest finalWrap(final HttpServletRequest request) {
        return new RequestWrapper(request);
    }

    private HttpServletResponse finalWrap(final HttpServletResponse response) {
        return new ResponseWrapper(response);
    }

    private abstract static class MyHttpServletRequestImpl implements HttpServletRequest {

        @Override
        public String getAuthType() {
            return getHttpServletRequest().getAuthType();
        }

        @Override
        public boolean authenticate(HttpServletResponse response) throws IOException, ServletException {
            return getHttpServletRequest().authenticate(response);
        }

        @Override
        public boolean isAsyncSupported() {
            return getHttpServletRequest().isAsyncSupported();
        }

        @Override
        public boolean isAsyncStarted() {
            return getHttpServletRequest().isAsyncStarted();
        }

        @Override
        public AsyncContext startAsync() throws IllegalStateException {
            return getHttpServletRequest().startAsync();
        }

        @Override
        public AsyncContext startAsync(ServletRequest request, ServletResponse response) throws IllegalStateException {
            return getHttpServletRequest().startAsync(request, response);
        }

        abstract HttpServletRequest getHttpServletRequest();

        @Override
        public Cookie[] getCookies() {
            return getHttpServletRequest().getCookies();
        }

        @Override
        public long getDateHeader(String s) {
            return getHttpServletRequest().getDateHeader(s);
        }

        @Override
        public Part getPart(String s) throws ServletException, IOException {
            return getHttpServletRequest().getPart(s);
        }

        @Override
        public Collection<Part> getParts() throws ServletException, IOException {
            return getHttpServletRequest().getParts();
        }

        @Override
        public String getHeader(String s) {
            return getHttpServletRequest().getHeader(s);
        }

        @Override
        public Enumeration getHeaders(String s) {
            return getHttpServletRequest().getHeaders(s);
        }

        @Override
        public Enumeration getHeaderNames() {
            return getHttpServletRequest().getHeaderNames();
        }

        @Override
        public int getIntHeader(String s) {
            return getHttpServletRequest().getIntHeader(s);
        }

        @Override
        public String getMethod() {
            return getHttpServletRequest().getMethod();
        }

        @Override
        public String getPathInfo() {
            return getHttpServletRequest().getPathInfo();
        }

        @Override
        public String getPathTranslated() {
            return getHttpServletRequest().getPathTranslated();
        }

        @Override
        public String getContextPath() {
            return getHttpServletRequest().getContextPath();
        }

        @Override
        public String getQueryString() {
            return getHttpServletRequest().getQueryString();
        }

        @Override
        public String getRemoteUser() {
            return getHttpServletRequest().getRemoteUser();
        }

        @Override
        public boolean isUserInRole(String s) {
            return getHttpServletRequest().isUserInRole(s);
        }

        @Override
        public Principal getUserPrincipal() {
            return getHttpServletRequest().getUserPrincipal();
        }

        @Override
        public String getRequestedSessionId() {
            return getHttpServletRequest().getRequestedSessionId();
        }

        @Override
        public String getRequestURI() {
            return getHttpServletRequest().getRequestURI();
        }

        @Override
        public StringBuffer getRequestURL() {
            return getHttpServletRequest().getRequestURL();
        }

        @Override
        public String getServletPath() {
            return getHttpServletRequest().getServletPath();
        }

        @Override
        public HttpSession getSession(boolean b) {
            return getHttpServletRequest().getSession(b);
        }

        @Override
        public HttpSession getSession() {
            return getHttpServletRequest().getSession();
        }

        @Override
        public boolean isRequestedSessionIdValid() {
            return getHttpServletRequest().isRequestedSessionIdValid();
        }

        @Override
        public boolean isRequestedSessionIdFromCookie() {
            return getHttpServletRequest().isRequestedSessionIdFromCookie();
        }

        @Override
        public boolean isRequestedSessionIdFromURL() {
            return getHttpServletRequest().isRequestedSessionIdFromURL();
        }

        @Override
        public boolean isRequestedSessionIdFromUrl() {
            return getHttpServletRequest().isRequestedSessionIdFromUrl();
        }

        @Override
        public Object getAttribute(String s) {
            return getHttpServletRequest().getAttribute(s);
        }

        @Override
        public Enumeration getAttributeNames() {
            return getHttpServletRequest().getAttributeNames();
        }

        @Override
        public String getCharacterEncoding() {
            return getHttpServletRequest().getCharacterEncoding();
        }

        @Override
        public void setCharacterEncoding(String s) throws UnsupportedEncodingException {
            getHttpServletRequest().setCharacterEncoding(s);
        }

        @Override
        public int getContentLength() {
            return getHttpServletRequest().getContentLength();
        }

        @Override
        public String getContentType() {
            return getHttpServletRequest().getContentType();
        }

        @Override
        public ServletInputStream getInputStream() throws IOException {
            return getHttpServletRequest().getInputStream();
        }

        @Override
        public String getParameter(String s) {
            return getHttpServletRequest().getParameter(s);
        }

        @Override
        public Enumeration getParameterNames() {
            return getHttpServletRequest().getParameterNames();
        }

        @Override
        public String[] getParameterValues(String s) {
            return getHttpServletRequest().getParameterValues(s);
        }

        @Override
        public Map getParameterMap() {
            return getHttpServletRequest().getParameterMap();
        }

        @Override
        public String getProtocol() {
            return getHttpServletRequest().getProtocol();
        }

        @Override
        public String getScheme() {
            return getHttpServletRequest().getScheme();
        }

        @Override
        public String getServerName() {
            return getHttpServletRequest().getServerName();
        }

        @Override
        public int getServerPort() {
            return getHttpServletRequest().getServerPort();
        }

        @Override
        public BufferedReader getReader() throws IOException {
            return getHttpServletRequest().getReader();
        }

        @Override
        public String getRemoteAddr() {
            return getHttpServletRequest().getRemoteAddr();
        }

        @Override
        public String getRemoteHost() {
            return getHttpServletRequest().getRemoteHost();
        }

        @Override
        public void setAttribute(String s, Object o) {
            getHttpServletRequest().setAttribute(s, o);
        }

        @Override
        public void removeAttribute(String s) {
            getHttpServletRequest().removeAttribute(s);
        }

        @Override
        public Locale getLocale() {
            return getHttpServletRequest().getLocale();
        }

        @Override
        public Enumeration getLocales() {
            return getHttpServletRequest().getLocales();
        }

        @Override
        public boolean isSecure() {
            return getHttpServletRequest().isSecure();
        }

        @Override
        public RequestDispatcher getRequestDispatcher(String s) {
            return getHttpServletRequest().getRequestDispatcher(s);
        }

        @Override
        public String getRealPath(String s) {
            return getHttpServletRequest().getRealPath(s);
        }

        @Override
        public int getRemotePort() {
            return getHttpServletRequest().getRemotePort();
        }

        @Override
        public String getLocalName() {
            return getHttpServletRequest().getLocalName();
        }

        @Override
        public String getLocalAddr() {
            return getHttpServletRequest().getLocalAddr();
        }

        @Override
        public int getLocalPort() {
            return getHttpServletRequest().getLocalPort();
        }

        @Override
        public DispatcherType getDispatcherType() {
            return getHttpServletRequest().getDispatcherType();
        }

        @Override
        public AsyncContext getAsyncContext() {
            return getHttpServletRequest().getAsyncContext();
        }

        @Override
        public ServletContext getServletContext() {
            return getHttpServletRequest().getServletContext();
        }

        @Override
        public void logout() throws ServletException {
            getHttpServletRequest().logout();
        }

        @Override
        public void login(String u, String p) throws ServletException {
            getHttpServletRequest().login(u, p);
        }
    }

    private static ServiceLocator getServiceLocator(InjectionManager injectionManager) {
        if (injectionManager instanceof ImmediateHk2InjectionManager) {
            return  ((ImmediateHk2InjectionManager) injectionManager).getServiceLocator();
        } else if (injectionManager instanceof DelayedHk2InjectionManager) {
            return  ((DelayedHk2InjectionManager) injectionManager).getServiceLocator();
        } else {
            throw new RuntimeException("Invalid InjectionManager");
        }
    }
}
