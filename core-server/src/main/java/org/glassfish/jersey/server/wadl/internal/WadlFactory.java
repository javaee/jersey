/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import java.util.List;
import java.util.logging.Logger;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.wadl.WadlApplicationContext;

/**
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 */
public final class WadlFactory {

    private static final Logger LOGGER = Logger.getLogger(WadlFactory.class.getName());

    private final boolean isWadlEnabled;

//    private final WadlGenerator wadlGenerator;
    
    private final ResourceConfig _resourceConfig;

    private WadlApplicationContext wadlApplicationContext;

    public WadlFactory(ResourceConfig resourceConfig) {
        isWadlEnabled = isWadlEnabled(resourceConfig);
        _resourceConfig = resourceConfig;

//        if (isWadlEnabled) {
//            wadlGenerator = WadlGeneratorConfigLoader.loadWadlGeneratorsFromConfig(resourceConfig);
//        }
//        else {
//            wadlGenerator = null;
//        }
    }

    public boolean isSupported() {
        return isWadlEnabled;
    }

    public WadlApplicationContext createWadlApplicationContext(List<org.glassfish.jersey.server.model.Resource> rootResources) {
        if (!isSupported()) return null;

        return new WadlApplicationContextImpl(rootResources, _resourceConfig);
    }


    // TODO - J2
    public void init(/*InjectableProviderFactory ipf, */List<org.glassfish.jersey.server.model.Resource> rootResources) {
        if (!isSupported()) return;

        wadlApplicationContext = new WadlApplicationContextImpl(rootResources, _resourceConfig);
    }

    // TODO - J2
//    /**
//     * Create the WADL resource method for OPTIONS.
//     * <p>
//     * This is created using reflection so that there is no runtime
//     * dependency on JAXB. If the JAXB jars are not in the class path
//     * then WADL generation will not be supported.
//     *
//     * @param resource the resource model
//     * @return the WADL resource OPTIONS method
//     */
//    public ResourceMethod createWadlOptionsMethod(
//            Map<String, List<ResourceMethod>> methods,
//            org.glassfish.jersey.server.model.Resource resource, PathPattern p) {
//        if (!isSupported()) return null;
//
//        if (p == null) {
//            return new WadlMethodFactory.WadlOptionsMethod(methods, resource, null, wadlApplicationContext);
//        } else {
//            // Remove the '/' from the beginning
//            String path = p.getTemplate().getTemplate().substring(1);
//            return new WadlMethodFactory.WadlOptionsMethod(methods, resource, path, wadlApplicationContext);
//        }
//    }

    /**
     * Check if WADL is not disabled in {@link ResourceConfig}.
     *
     * @param resourceConfig application configuration.
     * @return true when WADL is enabled, false otherwise.
     */
    private static boolean isWadlEnabled(ResourceConfig resourceConfig) {
        return !resourceConfig.isProperty(ServerProperties.FEATURE_DISABLE_WADL);
    }

    /**
     * Get {@link WadlApplicationContext}.
     *
     * @return {@link WadlApplicationContext}.
     */
    /* package */ WadlApplicationContext getWadlApplicationContext() {
        return wadlApplicationContext;
    }
}
