/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.filter;

import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.internal.inject.Providers;
import org.glassfish.jersey.spi.ContentEncoder;

/**
 * Feature that configures support for content encodings on the client side.
 * This feature registers {@link EncodingFilter} and the specified set of
 * {@link org.glassfish.jersey.spi.ContentEncoder encoding providers} to the
 * {@link javax.ws.rs.core.Configurable client configuration}. It also allows
 * setting the value of {@link ClientProperties#USE_ENCODING} property.
 *
 * @author Martin Matula
 */
public class EncodingFeature implements Feature {
    private final String useEncoding;
    private final Class<?>[] encodingProviders;

    /**
     * Create a new instance of the feature.
     *
     * @param encodingProviders Encoding providers to be registered in the client configuration.
     */
    public EncodingFeature(Class<?>... encodingProviders) {
        this(null, encodingProviders);
    }

    /**
     * Create a new instance of the feature specifying the default value for the
     * {@link ClientProperties#USE_ENCODING} property. Unless the value is set in the client configuration
     * properties at the time when this feature gets enabled, the provided value will be used.
     *
     * @param useEncoding Default value of {@link ClientProperties#USE_ENCODING} property.
     * @param encoders    Encoders to be registered in the client configuration.
     */
    public EncodingFeature(String useEncoding, Class<?>... encoders) {
        this.useEncoding = useEncoding;

        Providers.ensureContract(ContentEncoder.class, encoders);
        this.encodingProviders = encoders;
    }


    @Override
    public boolean configure(FeatureContext context) {
        if (useEncoding != null) {
            // properties take precedence over the constructor value
            if (!context.getConfiguration().getProperties().containsKey(ClientProperties.USE_ENCODING)) {
                context.property(ClientProperties.USE_ENCODING, useEncoding);
            }
        }
        for (Class<?> provider : encodingProviders) {
            context.register(provider);
        }
        boolean enable = useEncoding != null || encodingProviders.length > 0;
        if (enable) {
            context.register(EncodingFilter.class);
        }
        return enable;
    }
}
