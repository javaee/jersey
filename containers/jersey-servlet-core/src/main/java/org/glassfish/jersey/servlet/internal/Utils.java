/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.servlet.internal;

import java.util.Enumeration;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.ServletContext;

import org.glassfish.jersey.server.ResourceConfig;

/**
 * Utility class.
 *
 * @author Michal Gajdos
 */
public final class Utils {

    /**
     * Internal {@link javax.servlet.ServletContext servlet context} attribute name under which an instance of
     * {@link org.glassfish.jersey.server.ResourceConfig resource config} can be stored. The instance is later used to initialize
     * servlet in {@link org.glassfish.jersey.servlet.WebConfig} instead of creating a new one.
     */
    private static final String RESOURCE_CONFIG = "jersey.config.servlet.internal.resourceConfig";

    /**
     * Store {@link org.glassfish.jersey.server.ResourceConfig resource config} as an attribute of given
     * {@link javax.servlet.ServletContext servlet context}. If {@code config} is {@code null} then the previously stored value
     * (if any) is removed. The {@code configName} is used as an attribute name suffix.
     *
     * @param config resource config to be stored.
     * @param context servlet context to store the config in.
     * @param configName name or id of the resource config.
     */
    public static void store(final ResourceConfig config, final ServletContext context, final String configName) {
        final String attributeName = RESOURCE_CONFIG + "_" + configName;
        context.setAttribute(attributeName, config);
    }

    /**
     * Load {@link org.glassfish.jersey.server.ResourceConfig resource config} from given
     * {@link javax.servlet.ServletContext servlet context}. If found then the resource config is also removed from servlet
     * context. The {@code configName} is used as an attribute name suffix.
     *
     * @param context servlet context to load resource config from.
     * @param configName name or id of the resource config.
     * @return previously stored resource config or {@code null} if no resource config has been stored.
     */
    public static ResourceConfig retrieve(final ServletContext context, final String configName) {
        final String attributeName = RESOURCE_CONFIG + "_" + configName;
        final ResourceConfig config = (ResourceConfig) context.getAttribute(attributeName);
        context.removeAttribute(attributeName);
        return config;
    }

    /**
     * Extract context params from {@link ServletContext}.
     *
     * @param servletContext actual servlet context.
     * @return map representing current context parameters.
     */
    public static Map<String, Object> getContextParams(final ServletContext servletContext) {
        final Map<String, Object> props = new HashMap<>();
        final Enumeration names = servletContext.getAttributeNames();
        while (names.hasMoreElements()) {
            final String name = (String) names.nextElement();
            props.put(name, servletContext.getAttribute(name));
        }
        return props;
    }

    /**
     * Prevents instantiation.
     */
    private Utils() {
    }
}
