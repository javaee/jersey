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

import java.util.Collections;
import java.util.Map;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

/**
 * Feature used to register MOXy XML providers.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class MoxyXmlFeature implements Feature {

    private final Map<String, Object> properties;
    private final ClassLoader classLoader;
    private final boolean oxmMappingLookup;
    private final Class[] classes;

    /**
     * Default constructor creates standard {@link org.eclipse.persistence.jaxb.JAXBContext} without any activated features
     * and properties. Current context {@link ClassLoader} will be used.
     */
    public MoxyXmlFeature() {
        this(Collections.<String, Object>emptyMap(), Thread.currentThread().getContextClassLoader(), false);
    }

    /**
     * Constructor which allows MOXy {@link org.eclipse.persistence.jaxb.JAXBContext} customization.
     *
     * @param classes additional classes used for creating {@link org.eclipse.persistence.jaxb.JAXBContext}.
     */
    public MoxyXmlFeature(Class<?>... classes) {
        this(Collections.<String, Object>emptyMap(), Thread.currentThread().getContextClassLoader(), false, classes);
    }

    /**
     * Constructor which allows MOXy {@link org.eclipse.persistence.jaxb.JAXBContext} customization.
     *
     * @param properties       properties to be passed to
     *                         {@link org.eclipse.persistence.jaxb.JAXBContextFactory#createContext(Class[], java.util.Map,
     *                         ClassLoader)}. May be {@code null}.
     * @param classLoader      will be used to load classes. If {@code null}, current context {@link ClassLoader} will be used.
     * @param oxmMappingLookup if {@code true}, lookup for file with custom mappings will be performed.
     * @param classes          additional classes used for creating {@link org.eclipse.persistence.jaxb.JAXBContext}.
     */
    public MoxyXmlFeature(Map<String, Object> properties, ClassLoader classLoader, boolean oxmMappingLookup, Class... classes) {
        this.properties = (properties == null ? Collections.<String, Object>emptyMap() : properties);
        this.classLoader = (classLoader == null ? Thread.currentThread().getContextClassLoader() : classLoader);
        this.oxmMappingLookup = oxmMappingLookup;
        this.classes = classes;
    }

    @Override
    public boolean configure(FeatureContext context) {
        context.register(new MoxyContextResolver(properties, classLoader, oxmMappingLookup, classes));
        return true;
    }
}
