/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.moxy.xml;

import java.io.InputStream;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.ext.ContextResolver;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;

import org.eclipse.persistence.jaxb.JAXBContextFactory;
import org.eclipse.persistence.jaxb.JAXBContextProperties;

/**
 * {@link ContextResolver} implementation which creates MOXy {@link JAXBContext}.
 *
 * TODO: deal with classes NOT annotated with @XmlRootElement/@XmlType
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
class MoxyContextResolver implements ContextResolver<JAXBContext> {

    private static final Logger LOGGER = Logger.getLogger(MoxyContextResolver.class.getName());
    private static final String MOXY_OXM_MAPPING_FILE_NAME = "eclipselink-oxm.xml";

    private final boolean oxmMappingLookup;
    private final Map<String, Object> properties;
    private final ClassLoader classLoader;
    private final Class[] classes;


    /**
     * Default constructor creates standard {@link JAXBContext} without any activated features
     * and properties. Current context {@link ClassLoader} will be used.
     */
    public MoxyContextResolver() {
        this(Collections.<String, Object>emptyMap(), Thread.currentThread().getContextClassLoader(), false);
    }

    /**
     * Constructor which allows MOXy {@link JAXBContext} customization.
     *
     * @param properties       properties to be passed to
     *                         {@link JAXBContextFactory#createContext(Class[], java.util.Map, ClassLoader)}. May be {@code null}.
     * @param classLoader      will be used to load classes. If {@code null}, current context {@link ClassLoader} will be used.
     * @param oxmMappingLookup if {@code true}, lookup for file with custom mappings will be performed.
     * @param classes          additional classes used for creating {@link org.eclipse.persistence.jaxb.JAXBContext}.
     */
    public MoxyContextResolver(
            Map<String, Object> properties,
            ClassLoader classLoader,
            boolean oxmMappingLookup,
            Class... classes) {
        this.properties = properties == null ? Collections.<String, Object>emptyMap() : properties;
        this.classLoader = (classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader);
        this.oxmMappingLookup = oxmMappingLookup;
        this.classes = classes;
    }

    @Override
    public JAXBContext getContext(Class<?> type) {
        Map<String, Object> propertiesCopy = new HashMap<String, Object>(properties);

        if (oxmMappingLookup) {
            final InputStream eclipseLinkOxm = type.getResourceAsStream(MOXY_OXM_MAPPING_FILE_NAME);
            if (eclipseLinkOxm != null && !propertiesCopy.containsKey(JAXBContextProperties.OXM_METADATA_SOURCE)) {
                propertiesCopy.put(JAXBContextProperties.OXM_METADATA_SOURCE, eclipseLinkOxm);
            }
        }

        final Class[] typeArray;
        if (classes != null && classes.length > 0) {
            typeArray = new Class[1 + classes.length];
            System.arraycopy(classes, 0, typeArray, 0, classes.length);
            typeArray[typeArray.length - 1] = type;
        } else {
            typeArray = new Class[]{type};
        }

        try {
            final JAXBContext context = JAXBContextFactory.createContext(typeArray, propertiesCopy, classLoader);
            LOGGER.log(Level.FINE, "Using JAXB context " + context);
            return context;
        } catch (JAXBException e) {
            LOGGER.fine("Unable to create JAXB context.");
            return null;
        }
    }
}
