/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jsonb;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.CommonProperties;
import org.glassfish.jersey.internal.InternalProperties;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.jsonb.internal.JsonbProvider;

/**
 * Feature used to register Jackson JSON providers.
 * <p>
 * The Feature is automatically enabled when {@link org.glassfish.jersey.jsonb.internal.JsonbAutoDiscoverable} is on classpath.
 * Default JSON-B configuration obtained by calling {@code JsonbBuilder.create()} is used.
 * <p>
 * Custom configuration, if required, can be achieved by implementing custom {@link javax.ws.rs.ext.ContextResolver} and
 * registering it as a provider into JAX-RS runtime:
 * <pre>
 * &#64;Provider
 * &#64;class JsonbContextResolver implements ContextResolver&lt;Jsonb&gt; {
 *      &#64;Override
 *      public Jsonb getContext(Class<?> type) {
 *          JsonbConfig config = new JsonbConfig();
 *          // add custom configuration
 *          return JsonbBuilder.create(config);
 *      }
 * }
 * </pre>
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class JsonbFeature implements Feature {

    private static final String JSON_FEATURE = JsonbFeature.class.getSimpleName();

    @Override
    public boolean configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();

        final String jsonFeature = CommonProperties.getValue(
                config.getProperties(),
                config.getRuntimeType(),
                InternalProperties.JSON_FEATURE, JSON_FEATURE, String.class);

        // Other JSON providers registered.
        if (!JSON_FEATURE.equalsIgnoreCase(jsonFeature)) {
            return false;
        }

        // Disable other JSON providers.
        context.property(PropertiesHelper.getPropertyNameForRuntime(
                InternalProperties.JSON_FEATURE, config.getRuntimeType()), JSON_FEATURE);

        context.register(JsonbProvider.class);


        return true;
    }
}
