/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.moxy.json;

import javax.ws.rs.Priorities;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.InternalProperties;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.message.filtering.EntityFilteringFeature;
import org.glassfish.jersey.moxy.internal.MoxyFilteringFeature;
import org.glassfish.jersey.moxy.json.internal.ConfigurableMoxyJsonProvider;
import org.glassfish.jersey.moxy.json.internal.FilteringMoxyJsonProvider;

/**
 * Feature used to register MOXy JSON providers.
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Michal Gajdos
 */
public class MoxyJsonFeature implements Feature {

    private static final String JSON_FEATURE = MoxyJsonFeature.class.getSimpleName();

    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        if (CommonProperties.getValue(config.getProperties(), config.getRuntimeType(),
                CommonProperties.MOXY_JSON_FEATURE_DISABLE, Boolean.FALSE, Boolean.class)) {
            return false;
        }

        final String jsonFeature = CommonProperties.getValue(config.getProperties(), config.getRuntimeType(),
                InternalProperties.JSON_FEATURE, JSON_FEATURE, String.class);
        // Other JSON providers registered.
        if (!JSON_FEATURE.equalsIgnoreCase(jsonFeature)) {
            return false;
        }

        // Disable other JSON providers.
        context.property(PropertiesHelper.getPropertyNameForRuntime(InternalProperties.JSON_FEATURE, config.getRuntimeType()),
                JSON_FEATURE);

        // Set a slightly lower priority of workers than JSON-P so MOXy is not pick-ed up for JsonStructures (if both are used).
        final int workerPriority = Priorities.USER + 2000;

        if (EntityFilteringFeature.enabled(config)) {
            context.register(MoxyFilteringFeature.class);
            context.register(FilteringMoxyJsonProvider.class, workerPriority);
        } else {
            context.register(ConfigurableMoxyJsonProvider.class, workerPriority);
        }

        return true;
    }
}
