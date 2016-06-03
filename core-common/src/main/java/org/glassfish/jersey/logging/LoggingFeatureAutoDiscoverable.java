/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.logging;

import java.util.Map;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.FeatureContext;

import javax.annotation.Priority;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;

import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_CLIENT;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_LOGGER_LEVEL_SERVER;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_LOGGER_NAME;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_LOGGER_NAME_CLIENT;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_LOGGER_NAME_SERVER;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_MAX_ENTITY_SIZE;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_MAX_ENTITY_SIZE_CLIENT;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_MAX_ENTITY_SIZE_SERVER;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_VERBOSITY;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_VERBOSITY_CLIENT;
import static org.glassfish.jersey.logging.LoggingFeature.LOGGING_FEATURE_VERBOSITY_SERVER;

/**
 * Auto-discoverable class that registers {@link LoggingFeature} based on configuration properties.
 * <p>
 * Feature is registered if any of the common properties (see {@link LoggingFeature}) is set or any of the client properties is
 * set and context's {@link RuntimeType} is {@link RuntimeType#CLIENT} or any of the server properties is set and context's
 * {@link RuntimeType} is {@link RuntimeType#SERVER}.
 * <p>
 * The registration does not occur if the feature is already registered or auto-discoverable mechanism is disabled.
 *
 * @author Ondrej Kosatka (ondrej.kosatka at oracle.com)
 * @since 2.23
 */
@Priority(AutoDiscoverable.DEFAULT_PRIORITY)
public final class LoggingFeatureAutoDiscoverable implements AutoDiscoverable {

    @Override
    public void configure(FeatureContext context) {
        if (!context.getConfiguration().isRegistered(LoggingFeature.class)) {

            Map properties = context.getConfiguration().getProperties();

            if (commonPropertyConfigured(properties)
                    || (context.getConfiguration().getRuntimeType() == RuntimeType.CLIENT && clientConfigured(properties))
                    || (context.getConfiguration().getRuntimeType() == RuntimeType.SERVER && serverConfigured(properties))) {
                context.register(LoggingFeature.class);
            }
        }
    }

    private boolean commonPropertyConfigured(Map properties) {
        return properties.containsKey(LOGGING_FEATURE_LOGGER_NAME)
                || properties.containsKey(LOGGING_FEATURE_LOGGER_LEVEL)
                || properties.containsKey(LOGGING_FEATURE_VERBOSITY)
                || properties.containsKey(LOGGING_FEATURE_MAX_ENTITY_SIZE);
    }

    private boolean clientConfigured(Map properties) {
        return properties.containsKey(LOGGING_FEATURE_LOGGER_NAME_CLIENT)
                || properties.containsKey(LOGGING_FEATURE_LOGGER_LEVEL_CLIENT)
                || properties.containsKey(LOGGING_FEATURE_VERBOSITY_CLIENT)
                || properties.containsKey(LOGGING_FEATURE_MAX_ENTITY_SIZE_CLIENT);
    }

    private boolean serverConfigured(Map properties) {
        return properties.containsKey(LOGGING_FEATURE_LOGGER_NAME_SERVER)
                || properties.containsKey(LOGGING_FEATURE_LOGGER_LEVEL_SERVER)
                || properties.containsKey(LOGGING_FEATURE_VERBOSITY_SERVER)
                || properties.containsKey(LOGGING_FEATURE_MAX_ENTITY_SIZE_SERVER);
    }
}
