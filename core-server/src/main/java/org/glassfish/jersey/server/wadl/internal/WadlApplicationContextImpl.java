/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.wadl.internal;

import java.net.URI;
import java.security.AccessController;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.namespace.QName;

import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.server.ExtendedResourceContext;
import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.server.wadl.WadlApplicationContext;
import org.glassfish.jersey.server.wadl.WadlGenerator;
import org.glassfish.jersey.server.wadl.config.WadlGeneratorConfig;
import org.glassfish.jersey.server.wadl.config.WadlGeneratorConfigLoader;

import org.glassfish.hk2.api.ServiceLocator;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Doc;
import com.sun.research.ws.wadl.Grammars;
import com.sun.research.ws.wadl.Include;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;

/**
 * WADL application context implementation.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 */
public class WadlApplicationContextImpl implements WadlApplicationContext {

    private static final Logger LOGGER = Logger.getLogger(WadlApplicationContextImpl.class.getName());

    static final String WADL_JERSEY_NAMESPACE = "http://jersey.java.net/";
    public static final JAXBElement extendedElement = new JAXBElement<String>(
            new QName(WADL_JERSEY_NAMESPACE, "extended", "jersey"), String.class, "true");

    @Context
    private ExtendedResourceContext resourceContext;

    private final ServiceLocator serviceLocator;


    private boolean wadlGenerationEnabled = true;

    private final WadlGeneratorConfig wadlGeneratorConfig;

    private final JAXBContext jaxbContext;

    public WadlApplicationContextImpl(@Context Configuration configuration, @Context ServiceLocator serviceLocator) {
        this.serviceLocator = serviceLocator;
        this.wadlGeneratorConfig = WadlGeneratorConfigLoader.loadWadlGeneratorsFromConfig(configuration.getProperties());

        // TODO perhaps this should be done another way for the moment
        // create a temporary generator just to do this one task
        final WadlGenerator wadlGenerator = wadlGeneratorConfig.createWadlGenerator(serviceLocator);

        JAXBContext jaxbContextCandidate;

        final ClassLoader contextClassLoader = AccessController.doPrivileged(ReflectionHelper.getContextClassLoaderPA());
        try {
            // Nasty ClassLoader magic. JAXB-API has some strange limitation about what classloader can
            // be used in OSGi environment - it must be same as context ClassLoader. Following code just
            // workarounds this limitation
            // see JERSEY-1818
            // see JSR222-46

            final ClassLoader jerseyModuleClassLoader =
                    AccessController.doPrivileged(ReflectionHelper.getClassLoaderPA(wadlGenerator.getClass()));

            AccessController.doPrivileged(ReflectionHelper.setContextClassLoaderPA(jerseyModuleClassLoader));

            jaxbContextCandidate = JAXBContext.newInstance(wadlGenerator.getRequiredJaxbContextPath(), jerseyModuleClassLoader);

        } catch (JAXBException ex) {
            try {
                // fallback for glassfish
                LOGGER.log(Level.FINE, LocalizationMessages.WADL_JAXB_CONTEXT_FALLBACK(), ex);
                jaxbContextCandidate = JAXBContext.newInstance(wadlGenerator.getRequiredJaxbContextPath());
            } catch (JAXBException innerEx) {
                throw new ProcessingException(LocalizationMessages.ERROR_WADL_JAXB_CONTEXT(), ex);
            }
        } finally {
            AccessController.doPrivileged(ReflectionHelper.setContextClassLoaderPA(contextClassLoader));
        }

        jaxbContext = jaxbContextCandidate;
    }

    @Override
    public ApplicationDescription getApplication(UriInfo uriInfo, boolean detailedWadl) {
        ApplicationDescription applicationDescription = getWadlBuilder(detailedWadl, uriInfo)
                .generate(resourceContext.getResourceModel().getRootResources());
        final Application application = applicationDescription.getApplication();
        for (Resources resources : application.getResources()) {
            if (resources.getBase() == null) {
                resources.setBase(uriInfo.getBaseUri().toString());
            }
        }
        attachExternalGrammar(application, applicationDescription, uriInfo.getRequestUri());
        return applicationDescription;
    }

    @Override
    public Application getApplication(UriInfo info,
                                      org.glassfish.jersey.server.model.Resource resource, boolean detailedWadl) {

        // Get the root application description
        //

        ApplicationDescription description = getApplication(info, detailedWadl);

        WadlGenerator wadlGenerator = wadlGeneratorConfig.createWadlGenerator(serviceLocator);
        Application application = new WadlBuilder(wadlGenerator, detailedWadl, info).generate(description, resource);
        if (application == null) {
            return null;
        }

        for (Resources resources : application.getResources()) {
            resources.setBase(info.getBaseUri().toString());
        }

        // Attach any grammar we may have

        attachExternalGrammar(application, description,
                info.getRequestUri());

        for (Resources resources : application.getResources()) {
            final Resource r = resources.getResource().get(0);
            r.setPath(info.getBaseUri().relativize(info.getAbsolutePath()).toString());

            // remove path params since path is fixed at this point
            r.getParam().clear();
        }

        return application;
    }

    // TODO probably no longer required
    @Override
    public JAXBContext getJAXBContext() {
        return jaxbContext;
    }

    private WadlBuilder getWadlBuilder(boolean detailedWadl, UriInfo uriInfo) {
        return (this.wadlGenerationEnabled ? new WadlBuilder(wadlGeneratorConfig.createWadlGenerator(serviceLocator),
                detailedWadl, uriInfo) : null);
    }

    @Override
    public void setWadlGenerationEnabled(boolean wadlGenerationEnabled) {
        this.wadlGenerationEnabled = wadlGenerationEnabled;
    }

    @Override
    public boolean isWadlGenerationEnabled() {
        return wadlGenerationEnabled;
    }

    /**
     * Update the application object to include the generated grammar objects
     */
    private void attachExternalGrammar(
            Application application,
            ApplicationDescription applicationDescription,
            URI requestURI) {

        // Massage the application.wadl URI slightly to get the right effect
        //

        try {
            final String requestURIPath = requestURI.getPath();

            if (requestURIPath.endsWith("application.wadl")) {
                requestURI = UriBuilder.fromUri(requestURI)
                        .replacePath(
                                requestURIPath
                                        .substring(0, requestURIPath.lastIndexOf('/') + 1))
                        .build();
            }


            String root = application.getResources().get(0).getBase();
            UriBuilder extendedPath = root != null ?
                    UriBuilder.fromPath(root).path("/application.wadl/")
                    : UriBuilder.fromPath("./application.wadl/");
            URI rootURI = root != null ? UriBuilder.fromPath(root).build() : null;


            // Add a reference to this grammar
            //

            Grammars grammars;
            if (application.getGrammars() != null) {
                LOGGER.info(LocalizationMessages.ERROR_WADL_GRAMMAR_ALREADY_CONTAINS());
                grammars = application.getGrammars();
            } else {
                grammars = new Grammars();
                application.setGrammars(grammars);
            }

            // Create a reference back to the root WADL
            //

            for (String path : applicationDescription.getExternalMetadataKeys()) {
                URI schemaURI =
                        extendedPath.clone().path(path).build();

                String schemaPath = rootURI != null ?
                        requestURI.relativize(schemaURI).toString()
                        : schemaURI.toString();

                Include include = new Include();
                include.setHref(schemaPath);
                Doc doc = new Doc();
                doc.setLang("en");
                doc.setTitle("Generated");
                include.getDoc().add(doc);

                // Finally add to list
                grammars.getInclude().add(include);
            }
        } catch (Exception e) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_EXTERNAL_GRAMMAR(), e);
        }
    }
}