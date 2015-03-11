/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.EventListener;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;

import javax.ws.rs.core.Application;

import javax.servlet.DispatcherType;
import javax.servlet.Filter;
import javax.servlet.ServletContextAttributeListener;
import javax.servlet.ServletContextListener;
import javax.servlet.ServletRequestAttributeListener;
import javax.servlet.ServletRequestListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpSessionActivationListener;
import javax.servlet.http.HttpSessionAttributeListener;
import javax.servlet.http.HttpSessionListener;

import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

/**
 * A Servlet-based deployment context.
 *
 * <p>
 * An instance of this class is created by creating using {@link Builder}, that allows to configure the deployment
 * context state, and finally building the context by invoking the {@link Builder#build()} method.
 * </p>
 * <p>
 * This deployment context is compatible with Servlet-based test containers. The following test container
 * factories support the descriptor:
 * <ul>
 * <li>{@code org.glassfish.jersey.test.grizzly.GrizzlyWebTestContainerFactory} for testing with the
 * Grizzly Servlet container.</li>
 * <li>{@code org.glassfish.jersey.test.external.ExternalTestContainerFactory} for testing Java EE Web
 * applications deployed independently in a separate JVM to that of the tests. For example, the application
 * may be deployed to the GlassFish or WebLogic application server.</li>
 * </ul>
 * </p>
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @since 2.8
 */
@SuppressWarnings("MethodOverridesStaticMethodOfSuperclass")
public class ServletDeploymentContext extends DeploymentContext {

    /**
     * Create a new servlet deployment context builder for a JAX-RS / Jersey application instance.
     *
     * @param application a JAX-RS / Jersey application to be tested.
     * @return new servlet deployment context builder instance associated with the JAX-RS / Jersey application to be tested.
     *
     * @throws java.lang.NullPointerException in case the {@code application} is {@code null}.
     */
    public static Builder builder(final Application application) {
        return new Builder(application);
    }

    /**
     * Create a new servlet deployment context builder for a JAX-RS / Jersey application instance.
     *
     * @param applicationClass a JAX-RS / Jersey application to be tested.
     * @return new servlet deployment context builder instance associated with the JAX-RS / Jersey application to be tested.
     *
     * @throws java.lang.NullPointerException in case the {@code applicationClass} is {@code null}.
     */
    public static Builder builder(final Class<? extends Application> applicationClass) {
        return new Builder(applicationClass);
    }

    /**
     * Create new servlet deployment context builder initialized from the supplied initialization parameters.
     *
     * @param initParams a map of initialization parameters. The parameters will be copied.
     * @return new servlet deployment context builder instance initialized from the supplied initialization parameters.
     *
     * @throws java.lang.NullPointerException is the specified map is {@code null}.
     */
    public static Builder builder(final Map<String, String> initParams) {
        return new Builder().initParams(initParams);
    }

    /**
     * Create new servlet deployment context builder bound to a Servlet instance.
     * <p>
     * Note that the servlet instance will be discarded if one of the {@link Builder#servlet(javax.servlet.http.HttpServlet)},
     * {@link Builder#servletClass(Class)}, {@link Builder#filterClass(Class)} or
     * {@link Builder#filterClass(Class, java.util.Set)} is invoked on the builder.
     * </p>
     *
     * @param servlet the servlet instance to serve the application.
     * @return new servlet deployment context builder instance bound to a Servlet instance.
     *
     * @throws java.lang.NullPointerException is the specified map is {@code null}.
     */
    public static Builder forServlet(HttpServlet servlet) {
        return new Builder().servlet(servlet);
    }

    /**
     * Create new servlet deployment context builder bound to a Servlet class.
     * <p>
     * Note that the servlet instance will be discarded if one of the {@link Builder#servlet(javax.servlet.http.HttpServlet)},
     * {@link Builder#servletClass(Class)}, {@link Builder#filterClass(Class)} or
     * {@link Builder#filterClass(Class, java.util.Set)} is invoked on the builder.
     * </p>
     *
     * @param servletClass the servlet class to serve the application.
     * @return new servlet deployment context builder instance bound to a Servlet instance.
     *
     * @throws java.lang.NullPointerException is the specified map is {@code null}.
     */
    public static Builder forServlet(Class<? extends HttpServlet> servletClass) {
        return new Builder().servletClass(servletClass);
    }

    /**
     * Create new servlet deployment context builder initialized with the providers from the specified packages.
     * <p>
     * The {@code packages} value will be set as one of the {@link Builder#initParam(String, String) initialization parameters}
     * with <tt>{@value org.glassfish.jersey.server.ServerProperties#PROVIDER_PACKAGES}</tt> key.
     * </p>
     *
     * @param packages list of application packages containing JAX-RS / Jersey provider and resource classes.
     * @return new servlet deployment context builder instance initialized with the providers from the specified packages.
     *
     * @throws java.lang.NullPointerException is the specified map is {@code null}.
     * @see org.glassfish.jersey.server.ServerProperties#PROVIDER_PACKAGES
     */
    public static Builder forPackages(String packages) {
        return new Builder().initParam(ServerProperties.PROVIDER_PACKAGES, packages);
    }

    /**
     * Create a new servlet deployment context for a JAX-RS / Jersey application instance.
     * <p>
     * The created servlet deployment context will be configured to use default values.
     * </p>
     *
     * @param application a JAX-RS / Jersey application to be tested.
     * @return new servlet deployment context instance associated with the JAX-RS / Jersey application to be tested.
     *
     * @throws java.lang.NullPointerException in case the {@code application} is {@code null}.
     */

    public static ServletDeploymentContext newInstance(final Application application) {
        return new Builder(application).build();
    }

    /**
     * Create a new servlet deployment context for a JAX-RS / Jersey application instance.
     * <p>
     * The created servlet deployment context will be configured to use default values.
     * </p>
     *
     * @param applicationClass a JAX-RS / Jersey application to be tested.
     * @return new servlet deployment context instance associated with the JAX-RS / Jersey application to be tested.
     *
     * @throws java.lang.NullPointerException in case the {@code applicationClass} is {@code null}.
     */
    public static ServletDeploymentContext newInstance(final Class<? extends Application> applicationClass) {
        return new Builder(applicationClass).build();
    }

    /**
     * Create new servlet deployment context initialized with the providers from the specified packages.
     * <p>
     * The {@code packages} value will be set as one of the {@link Builder#initParam(String, String) initialization parameters}
     * with <tt>{@value org.glassfish.jersey.server.ServerProperties#PROVIDER_PACKAGES}</tt> key.
     * </p>
     *
     * @param packages list of application packages containing JAX-RS / Jersey provider and resource classes.
     * @return new servlet deployment context instance initialized with the providers from the specified packages.
     *
     * @throws java.lang.NullPointerException is the specified map is {@code null}.
     * @see org.glassfish.jersey.server.ServerProperties#PROVIDER_PACKAGES
     */
    public static Builder newInstance(String packages) {
        return new Builder().initParam(ServerProperties.PROVIDER_PACKAGES, packages);
    }

    /**
     * The builder for building a Servlet-based deployment context.
     * <p>
     * If properties of the builder are not modified, default values will be utilized:
     * <ul>
     * <li>The default value for initialization and context parameters is an empty map.</li>
     * <li>The default value for the context and servlet path is an empty string.</li>
     * <li>The default value for the servlet class is the class {@link ServletContainer}.</li>
     * <li>The default value for the servlet instance, filter class and the servlet context listener class is {@code null}.</li>
     * </ul>
     * </p>
     * <p>
     * After the {@link #build()} has been invoked the state of the builder will be reset to the default values.
     * </p>
     */
    public static class Builder extends DeploymentContext.Builder {
        private static final EnumSet<DispatcherType> DEFAULT_DISPATCHER_TYPES = EnumSet.of(DispatcherType.REQUEST);

        private Map<String, String> initParams;
        private Map<String, String> contextParams;
        private Class<? extends HttpServlet> servletClass = ServletContainer.class;
        private HttpServlet servletInstance;
        private List<FilterDescriptor> filters;
        private List<Class<? extends EventListener>> listeners;
        private String servletPath = "";

        /**
         * Create new deployment context builder instance not explicitly bound to the JAX-RS / Jersey application class.
         * <p>
         * The constructor is provided to support different subclass initialization scenarios.
         * </p>
         */
        protected Builder() {
            super();
        }

        /**
         * Create a builder with one initialization parameter.
         *
         * @param name  the parameter name.
         * @param value the parameter value.
         */
        public Builder(String name, String value) {
            initParam(name, value);
        }

        /**
         * Create new deployment context builder instance and bind it to the JAX-RS / Jersey application instance.
         *
         * @param app JAX-RS / Jersey application instance.
         */
        protected Builder(final Application app) {
            super(app);
        }

        /**
         * Create new deployment context builder instance and bind it to the JAX-RS / Jersey application class.
         *
         * @param appClass JAX-RS / Jersey application class.
         */
        protected Builder(Class<? extends Application> appClass) {
            super(appClass);
        }

        /**
         * Add an initialization parameter.
         *
         * @param name  the parameter name.
         * @param value the parameter value.
         * @return this servlet deployment context builder.
         */
        public Builder initParam(String name, String value) {
            if (this.initParams == null) {
                this.initParams = new HashMap<>();
            }
            this.initParams.put(name, value);

            return this;
        }

        /**
         * Add initialization parameters.
         *
         * @param initParams a map of initialization parameters. The parameters will be copied.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException is the specified map is {@code null}.
         */
        public Builder initParams(Map<String, String> initParams) {
            if (this.initParams == null) {
                this.initParams = new HashMap<>();
            }
            this.initParams.putAll(initParams);

            return this;
        }

        /**
         * Add a context parameter.
         *
         * @param name  the parameter name.
         * @param value the parameter value.
         * @return this servlet deployment context builder.
         */
        public Builder contextParam(String name, String value) {
            if (this.contextParams == null) {
                this.contextParams = new HashMap<>();
            }
            this.contextParams.put(name, value);

            return this;
        }

        /**
         * Add context parameters.
         *
         * @param contextParams a map of context parameters. The parameters will be copied.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException is the specified map is {@code null}.
         */
        public Builder contextParams(Map<String, String> contextParams) {
            if (this.contextParams == null) {
                this.contextParams = new HashMap<>();
            }
            this.contextParams.putAll(contextParams);

            return this;
        }

        /**
         * Set the servlet class.
         *
         * <p>
         * Setting a servlet class resets the servlet instance as well as registered filters to {@code null}.
         * </p>
         *
         * @param servletClass the servlet class to serve the application.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException if {@code servletClass} is {@code null}.
         */
        public Builder servletClass(Class<? extends HttpServlet> servletClass) {
            if (servletClass == null) {
                throw new NullPointerException("The servlet class must not be null");
            }

            this.filters = null;
            this.servletInstance = null;
            this.servletClass = servletClass;
            return this;
        }

        /**
         * Set the servlet instance.
         *
         * <p>
         * Setting a servlet instance resets the servlet class as well as registered filters to {@code null}.
         * </p>
         *
         * @param servlet the servlet instance to serve the application.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException if {@code servletClass} is {@code null}.
         */
        public Builder servlet(HttpServlet servlet) {
            if (servletClass == null) {
                throw new NullPointerException("The servlet class must not be null");
            }

            this.filters = null;
            this.servletClass = null;
            this.servletInstance = servlet;
            return this;
        }

        /**
         * Set the filter class.
         *
         * The registered servlet filter will be active for {@link DispatcherType#REQUEST} dispatch types only.
         * <p>
         * Setting a filter class resets the servlet class and servlet instance to {@code null}.
         * </p>
         *
         * @param filterClass the filter class to serve the application.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException if {@code filterClass} is {@code null}.
         */
        public Builder filterClass(Class<? extends Filter> filterClass) {
            return this.filterClass(filterClass, DEFAULT_DISPATCHER_TYPES);
        }

        /**
         * Set the filter class.
         *
         * <p>
         * Setting a filter class resets the servlet class and servlet instance to {@code null}.
         * </p>
         *
         * @param filterClass     the filter class to serve the application.
         * @param dispatcherTypes dispatcher types for which the filter should be registered.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException     if {@code filterClass} or {@code dispatcherTypes} is {@code null}.
         * @throws java.lang.IllegalArgumentException in case the {@code dispatcherTypes} is empty.
         */
        public Builder filterClass(Class<? extends Filter> filterClass, Set<DispatcherType> dispatcherTypes) {
            if (filterClass == null) {
                throw new NullPointerException("The filter class must not be null.");
            }

            this.servletClass = null;
            this.servletInstance = null;
            return addFilter(filterClass, "jerseyfilter", Collections.<String, String>emptyMap(), dispatcherTypes);
        }

        /**
         * Add a filter class.
         * <p>
         * Adding a filter class DOES NOT reset the servlet or filter classes. Filter will be instantiated without
         * initialization parameters.
         * </p>
         *
         * @param filterClass filter class. Must not be {@code null}.
         * @param filterName  filter name. Must not be {@code null} or empty string.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException     if {@code filterClass} or {@code filterName} is {@code null}.
         * @throws java.lang.IllegalArgumentException in case the {@code filterName} is empty.
         */
        public Builder addFilter(Class<? extends Filter> filterClass, String filterName) {
            return addFilter(filterClass, filterName, Collections.<String, String>emptyMap());
        }

        /**
         * Add a filter class.
         * <p>
         * Adding a filter class DOES NOT reset the servlet or filter classes. Filter will be instantiated without
         * initialization parameters.
         * </p>
         *
         * @param filterClass filter class. Must not be {@code null}.
         * @param filterName  filter name. Must not be {@code null} or empty string.
         * @param initParams  filter init parameters. Must not be {@code null}.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException     if {@code filterClass} or {@code filterName} or {@code initParams}
         *                                            is {@code null}.
         * @throws java.lang.IllegalArgumentException in case the {@code filterName} is empty.
         */
        public Builder addFilter(Class<? extends Filter> filterClass, String filterName, Map<String, String> initParams) {
            return this.addFilter(filterClass, filterName, initParams, DEFAULT_DISPATCHER_TYPES);
        }

        /**
         * Add a filter class.
         * <p>
         * Adding a filter class DOES NOT reset the servlet or filter classes. Filter will be instantiated without
         * initialization parameters.
         * </p>
         *
         * @param filterClass     filter class. Must not be {@code null}.
         * @param filterName      filter name. Must not be {@code null} or empty string.
         * @param dispatcherTypes filter will be registered for these dispatcher types. Must not be {@code null}.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException     if {@code filterClass} or {@code filterName} or {@code dispatcherTypes}
         *                                            is {@code null}.
         * @throws java.lang.IllegalArgumentException in case the {@code filterName} or {@code dispatcherTypes} is empty.
         */
        public Builder addFilter(Class<? extends Filter> filterClass, String filterName, Set<DispatcherType> dispatcherTypes) {
            return this.addFilter(filterClass, filterName, Collections.<String, String>emptyMap(), dispatcherTypes);
        }

        /**
         * Add a filter class.
         * <p>
         * Adding a filter class DOES NOT reset the servlet or filter classes. Filter will be instantiated without
         * initialization parameters.
         * </p>
         *
         * @param filterClass     filter class. Must not be {@code null}.
         * @param filterName      filter name. Must not be {@code null} or empty string.
         * @param initParams      filter init parameters. Must not be {@code null}.
         * @param dispatcherTypes filter will be registered for these dispatcher types. Must not be {@code null}.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException     if {@code filterClass} or {@code filterName} or {@code initParams}
         *                                            or {@code dispatcherTypes} is {@code null}.
         * @throws java.lang.IllegalArgumentException in case the {@code filterName} or {@code dispatcherTypes} is empty.
         */
        public Builder addFilter(Class<? extends Filter> filterClass,
                                 String filterName,
                                 Map<String, String> initParams,
                                 Set<DispatcherType> dispatcherTypes) {
            if (this.filters == null) {
                this.filters = new ArrayList<>();
            }

            final LinkedList<String> nulls = new LinkedList<>();
            final LinkedList<String> empties = new LinkedList<>();
            if (filterClass == null) {
                nulls.add("filter class");
            }
            if ((filterName == null)) {
                nulls.add("filter name");
            } else if (filterName.isEmpty()) {
                empties.add("filter name");
            }
            if (initParams == null) {
                nulls.add("init parameters");
            }
            if (dispatcherTypes == null) {
                nulls.add("dispatcher types");
            } else if (dispatcherTypes.isEmpty()) {
                empties.add("dispatcher types");
            }

            if (!nulls.isEmpty()) {
                throw new NullPointerException(String.format("The %s must not be null.", nulls.toString()));
            }
            if (!empties.isEmpty()) {
                throw new IllegalArgumentException(String.format("The %s must not be empty.", empties.toString()));
            }

            this.filters.add(new FilterDescriptor(filterName, filterClass, initParams, dispatcherTypes));
            return this;
        }

        @Override
        public Builder contextPath(String contextPath) {
            super.contextPath(contextPath);
            return this;
        }

        /**
         * Set the servlet path.
         *
         * @param servletPath the servlet path to the application. (See Servlet specification for definition of servletPath.)
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException if {@code servletPath} is {@code null}.
         */
        public Builder servletPath(String servletPath) {
            if (servletPath == null) {
                throw new NullPointerException("The servlet path must not be null.");
            }

            this.servletPath = servletPath;
            return this;
        }

        /**
         * Add event listener class.
         * <p>
         * The event listener should be one of the following types:
         * <ul>
         * <li>{@code ServletContextListener}</li>
         * <li>{@code ServletContextAttributeListener}</li>
         * <li>{@code ServletRequestListener}</li>
         * <li>{@code ServletRequestAttributeListener}</li>
         * <li>{@code HttpSessionListener}</li>
         * <li>{@code HttpSessionActivationListener}</li>
         * <li>{@code HttpSessionAttributeListener}</li>
         * </ul>
         * </p>
         *
         * @param listenerClass the event listener class.
         * @return this servlet deployment context builder.
         *
         * @throws java.lang.NullPointerException     if {@code listenerClass} is {@code null}.
         * @throws java.lang.IllegalArgumentException if {@code listenerClass} is neither of the supported listener types listed
         *                                            above.
         */
        public Builder addListener(Class<? extends EventListener> listenerClass) {
            if (listenerClass == null) {
                throw new NullPointerException("The servlet context listener class must not be null");
            }

            if (!ServletContextListener.class.isAssignableFrom(listenerClass)
                    && !ServletContextAttributeListener.class.isAssignableFrom(listenerClass)
                    && !ServletRequestListener.class.isAssignableFrom(listenerClass)
                    && !ServletRequestAttributeListener.class.isAssignableFrom(listenerClass)
                    && !HttpSessionListener.class.isAssignableFrom(listenerClass)
                    && !HttpSessionActivationListener.class.isAssignableFrom(listenerClass)
                    && !HttpSessionAttributeListener.class.isAssignableFrom(listenerClass)) {
                throw new IllegalArgumentException("Unsupported event listener type.");
            }

            if (this.listeners == null) {
                this.listeners = new ArrayList<>();
            }

            this.listeners.add(listenerClass);
            return this;
        }

        /**
         * Build a new servlet deployment context configured by the current state of this
         * servlet deployment context builder.
         *
         * @return this servlet deployment context builder.
         */
        @Override
        public ServletDeploymentContext build() {
            ServletDeploymentContext wd = new ServletDeploymentContext(this);
            reset();
            return wd;
        }

        @Override
        protected void reset() {
            super.reset();
            this.initParams = null;
            this.contextParams = null;
            this.servletInstance = null;
            this.servletClass = ServletContainer.class;
            this.filters = null;
            this.listeners = null;
            this.servletPath = "";
        }
    }

    /**
     * Helper class to keep configuration information of a single filter.
     */
    public static class FilterDescriptor {

        private final String filterName;
        private final Class<? extends Filter> filterClass;
        private final Map<String, String> initParams;
        private final Set<DispatcherType> dispatcherTypes;

        /**
         * Create a new descriptor.
         *
         * @param filterName      Name for the filter registration.
         * @param filterClass     Servlet filter class.
         * @param initParams      Servlet filter init parameters.
         * @param dispatcherTypes dispatcher types for which the filter should be registered.
         */
        private FilterDescriptor(
                String filterName, Class<? extends Filter> filterClass,
                Map<String, String> initParams,
                Set<DispatcherType> dispatcherTypes) {

            this.filterName = filterName;
            this.filterClass = filterClass;
            this.initParams = initParams;
            this.dispatcherTypes = dispatcherTypes;
        }

        /**
         * Get filter class.
         *
         * @return filter class.
         */
        public Class<? extends Filter> getFilterClass() {
            return filterClass;
        }

        /**
         * Get filter name.
         *
         * @return filter name.
         */
        public String getFilterName() {
            return filterName;
        }

        /**
         * Get filter initialization parameters.
         *
         * @return filter initialization parameters.
         */
        public Map<String, String> getInitParams() {
            return initParams;
        }

        /**
         * Get filter dispatcher type set.
         *
         * @return filter dispatcher type set.
         */
        public Set<DispatcherType> getDispatcherTypes() {
            return dispatcherTypes;
        }
    }

    private final Map<String, String> initParams;
    private final Map<String, String> contextParams;
    private final Class<? extends HttpServlet> servletClass;
    private final HttpServlet servletInstance;
    private final List<FilterDescriptor> filters;
    private final List<Class<? extends EventListener>> listeners;
    private final String servletPath;

    /**
     * Create new servlet deployment context.
     *
     * @param b {@link Builder} instance.
     */
    protected ServletDeploymentContext(Builder b) {
        super(b);

        this.initParams = (b.initParams == null) ? Collections.<String, String>emptyMap() : b.initParams;
        this.contextParams = (b.contextParams == null) ? Collections.<String, String>emptyMap() : b.contextParams;
        this.servletClass = b.servletClass;
        this.servletInstance = b.servletInstance;
        this.filters = b.filters;
        this.servletPath = b.servletPath;
        this.listeners = (b.listeners == null)
                ? Collections.<Class<? extends EventListener>>emptyList() : Collections.unmodifiableList(b.listeners);
    }

    /**
     * Get the initialization parameters.
     *
     * @return the initialization parameters.
     */
    public Map<String, String> getInitParams() {
        return initParams;
    }

    /**
     * Get the context parameters.
     *
     * @return the context parameters.
     */
    public Map<String, String> getContextParams() {
        return contextParams;
    }

    /**
     * Get the servlet class.
     * <p>
     * Note that servlet class and {@link #getServletInstance() servlet instance} can either be both {@code null}
     * or one of them is specified exclusively (i.e. servlet class and servlet instance cannot be both not {@code null}
     * at the same time).
     * </p>
     *
     * @return the servlet class.
     *
     * @see #getServletInstance()
     */
    public Class<? extends HttpServlet> getServletClass() {
        return servletClass;
    }

    /**
     * Get the servlet instance.
     * <p>
     * Note that {@link #getServletClass() servlet class} and servlet instance can either be both {@code null}
     * or one of them is specified exclusively (i.e. servlet class and servlet instance cannot be both not {@code null}
     * at the same time).
     * </p>
     *
     * @return the servlet instance.
     *
     * @see #getServletClass()
     */
    public HttpServlet getServletInstance() {
        return servletInstance;
    }

    /**
     * Get the filter class.
     *
     * @return the filter classes.
     */
    public List<FilterDescriptor> getFilters() {
        return filters;
    }

    /**
     * Get the servlet path.
     *
     * @return the servlet path.
     */
    public String getServletPath() {
        return servletPath;
    }

    /**
     * Get all the registered Listener classes
     *
     * @return the registered listener classes, or {@code null} if none is registered.
     */
    public List<Class<? extends EventListener>> getListeners() {
        return listeners;
    }

}
