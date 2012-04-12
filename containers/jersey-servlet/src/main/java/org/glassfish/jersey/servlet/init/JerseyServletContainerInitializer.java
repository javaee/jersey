/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.servlet.init;

import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;
import javax.ws.rs.ext.Provider;

import javax.servlet.Registration;
import javax.servlet.ServletContainerInitializer;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletRegistration;
import javax.servlet.annotation.HandlesTypes;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.servlet.ServletContainer;

import com.google.common.collect.Lists;

/*
 It is RECOMMENDED that implementations support the Servlet 3 framework
 pluggability mechanism to enable portability between containers and to avail
 themselves of container-supplied class scanning facilities.
 When using the pluggability mechanism the following conditions MUST be met:

 - If no Application subclass is present the added servlet MUST be
 named "javax.ws.rs.core.Application" and all root resource classes and
 providers packaged in the web application MUST be included in the published
 JAX-RS application. The application MUST be packaged with a web.xml that
 specifies a servlet mapping for the added servlet.

 - If an Application subclass is present and there is already a servlet defined
 that has a servlet initialization parameter named "javax.ws.rs.Application"
 whose value is the fully qualified name of the Application subclass then no
 servlet should be added by the JAX-RS implementation's ContainerInitializer
 since the application is already being handled by an existing servlet.

 - If an application subclass is present that is not being handled by an
 existing servlet then the servlet added by the ContainerInitializer MUST be
 named with the fully qualified name of the Application subclass.  If the
 Application subclass is annotated with @PathPrefix and no servlet-mapping
 exists for the added servlet then a new servlet mapping is added with the
 value of the @PathPrefix  annotation with "/*" appended otherwise the existing
 mapping is used. If the Application subclass is not annotated with @PathPrefix
 then the application MUST be packaged with a web.xml that specifies a servlet
 mapping for the added servlet. It is an error for more than one Application
 to be deployed at the same effective servlet mapping.

 In either of the latter two cases, if both Application#getClasses and
 Application#getSingletons return an empty list then all root resource classes
 and providers packaged in the web application MUST be included in the
 published JAX-RS application. If either getClasses or getSingletons return a
 non-empty list then only those classes or singletons returned MUST be included
 in the published JAX-RS application.

 If not using the Servlet 3 framework pluggability mechanism
 (e.g. in a pre-Servet 3.0 container), the servlet-class or filter-class
 element of the web.xml descriptor SHOULD name the JAX-RS
 implementation-supplied Servlet or Filter class respectively. The
 application-supplied subclass of Application SHOULD be identified using an
 init-param with a param-name of javax.ws.rs.Application.
 */
/**
 * {@link ServletContainerInitializer} implementation used for Servlet 3.x deployment.
 *
 * @author Paul Sandoz
 * @author Martin Matula (martin.matula at oracle.com)
 */
@HandlesTypes({Path.class, Provider.class, Application.class, ApplicationPath.class})
public class JerseyServletContainerInitializer implements ServletContainerInitializer {

    private static final Logger LOGGER =
            Logger.getLogger(JerseyServletContainerInitializer.class.getName());

    @Override
    public void onStartup(Set<Class<?>> classes, ServletContext sc) throws ServletException {
        if (classes == null) {
            classes = Collections.emptySet();
        }
        final int nOfRegisterations = sc.getServletRegistrations().size();
        // first see if there are any application classes in the web app
        for (Class<? extends Application> a : getApplicationClasses(classes)) {
            final ServletRegistration appReg = sc.getServletRegistration(a.getName());
            if (appReg != null) {
                // Servlet is registered with app name
                addServletWithExistingRegistration(sc, appReg, a, classes);
            } else {
                // Servlet is not registered with app name
                final List<Registration> srs = getInitParamDeclaredRegistrations(sc, a);
                if (!srs.isEmpty()) {
                    // List of servlets registered with app name in init param
                    for (Registration sr : srs) {
                        if (sr instanceof ServletRegistration) {
                            addServletWithExistingRegistration(sc, (ServletRegistration) sr, a, classes);
                        }
                    }
                } else {
                    addServletWithApplication(sc, a, classes);
                }
            }
        }

        if (nOfRegisterations == sc.getServletRegistrations().size()) {
            // No app was registered
            addServletWithDefaultConfiguration(sc, classes);
        }
    }

    private List<Registration> getInitParamDeclaredRegistrations(ServletContext sc, Class<? extends Application> a) {
        final List<Registration> srs = Lists.newArrayList();
        collectJaxRsRegistrations(sc.getServletRegistrations(), srs, a);
        collectJaxRsRegistrations(sc.getFilterRegistrations(), srs, a);
        return srs;
    }

    private void collectJaxRsRegistrations(Map<String, ? extends Registration> registrations,
            List<Registration> collected, Class<? extends Application> a) {
        for (Registration sr : registrations.values()) {
            Map<String, String> ips = sr.getInitParameters();
            if (ips.containsKey(ServerProperties.JAXRS_APPLICATION_CLASS)) {
                if (ips.get(ServerProperties.JAXRS_APPLICATION_CLASS).equals(a.getName())) {
                    collected.add(sr);
                }
            }
        }
    }

    private void addServletWithDefaultConfiguration(ServletContext sc, Set<Class<?>> classes) throws ServletException {
        ServletRegistration appReg = sc.getServletRegistration(Application.class.getName());
        if (appReg != null && appReg.getClassName() == null) {
            final Set<Class<?>> x = getRootResourceAndProviderClasses(classes);
            final ServletContainer s = new ServletContainer(new ResourceConfig(x));
            appReg = sc.addServlet(appReg.getName(), s);

            if (appReg.getMappings().isEmpty()) {
                // Error
                LOGGER.log(Level.SEVERE,
                        "The Jersey servlet application, named {0}, has no servlet mapping", appReg.getName());
            } else {
                LOGGER.log(Level.INFO,
                        "Registering the Jersey servlet application, named {0}, "
                        + "with the following root resource and provider classes: {1}",
                        new Object[]{appReg.getName(), x});
            }
        }
    }

    private void addServletWithApplication(final ServletContext sc,
            final Class<? extends Application> a, final Set<Class<?>> classes) throws ServletException {
        final ApplicationPath ap = a.getAnnotation(ApplicationPath.class);
        if (ap != null) {
            // App is annotated with ApplicationPath
            final ResourceConfig rc = new ResourceConfig(a).addClasses(classes);
            final ServletContainer s = new ServletContainer(rc);

            final String mapping = createMappingPath(ap);
            if (!mappingExists(sc, mapping)) {
                sc.addServlet(a.getName(), s).
                        addMapping(mapping);

                LOGGER.log(Level.INFO,
                        "Registering the Jersey servlet application, named {0}, "
                        + "at the servlet mapping, {1}, with the Application class of the same name",
                        new Object[]{a.getName(), mapping});
            } else {
                LOGGER.log(Level.SEVERE,
                        "Mapping conflict. A Servlet declaration exists with same mapping "
                        + "as the Jersey servlet application, named {0}, at the servlet mapping, {1}. "
                        + "The Jersey servlet is not deployed.",
                        new Object[]{a.getName(), mapping});
            }
        }
    }

    private void addServletWithExistingRegistration(final ServletContext sc, ServletRegistration sr,
            final Class<? extends Application> a, final Set<Class<?>> classes) throws ServletException {
        if (sr.getClassName() == null) {

            final Map<String, Object> initParams = new HashMap<String, Object>();
            for (Map.Entry<String, String> entry : sr.getInitParameters().entrySet()) {
                initParams.put(entry.getKey(), entry.getValue());
            }
            final ResourceConfig rc = new ResourceConfig(a)
                    .addClasses(classes).addProperties(initParams);
            final ServletContainer s = new ServletContainer(rc);

            sr = sc.addServlet(a.getName(), s);
            if (sr.getMappings().isEmpty()) {
                final ApplicationPath ap = a.getAnnotation(ApplicationPath.class);
                if (ap != null) {
                    final String mapping = createMappingPath(ap);
                    if (!mappingExists(sc, mapping)) {
                        sr.addMapping(mapping);

                        LOGGER.log(Level.INFO,
                                "Registering the Jersey servlet application, named {0}, at the servlet mapping, {1}, "
                                + "with the Application class of the same name",
                                new Object[]{a.getName(), mapping});
                    } else {
                        LOGGER.log(Level.SEVERE,
                                "Mapping conflict. A Servlet registration exists with same mapping "
                                + "as the Jersey servlet application, named {0}, at the servlet mapping, {1}. "
                                + "The Jersey servlet is not deployed.",
                                new Object[]{a.getName(), mapping});
                    }
                } else {
                    // Error
                    LOGGER.log(Level.SEVERE,
                            "The Jersey servlet application, named {0}, is not annotated with {1} and has no servlet mapping",
                            new Object[]{a.getName(), ApplicationPath.class.getSimpleName()});
                }
            } else {
                LOGGER.log(Level.INFO,
                        "Registering the Jersey servlet application, named {0}, with the Application class of the same name",
                        a.getName());
            }
        }
    }

    private boolean mappingExists(ServletContext sc, String mapping) {
        for (ServletRegistration sr : sc.getServletRegistrations().values()) {
            for (String declaredMapping : sr.getMappings()) {
                if (mapping.equals(declaredMapping)) {
                    return true;
                }
            }
        }

        return false;
    }

    private String createMappingPath(ApplicationPath ap) {
        String path = ap.value();
        if (!path.startsWith("/")) {
            path = "/" + path;
        }

        if (!path.endsWith("/*")) {
            if (path.endsWith("/")) {
                path += "*";
            } else {
                path += "/*";
            }
        }

        return path;
    }

    private Set<Class<? extends Application>> getApplicationClasses(Set<Class<?>> classes) {
        Set<Class<? extends Application>> s = new LinkedHashSet<Class<? extends Application>>();
        for (Class<?> c : classes) {
            if (Application.class != c && Application.class.isAssignableFrom(c)) {
                s.add(c.asSubclass(Application.class));
            }
        }

        return s;
    }

    private Set<Class<?>> getRootResourceAndProviderClasses(Set<Class<?>> classes) {
        // TODO filter out any classes from the Jersey jars
        Set<Class<?>> s = new LinkedHashSet<Class<?>>();
        for (Class<?> c : classes) {
            if (c.isAnnotationPresent(Path.class) || c.isAnnotationPresent(Provider.class)) {
                s.add(c);
            }
        }

        return s;
    }
}
