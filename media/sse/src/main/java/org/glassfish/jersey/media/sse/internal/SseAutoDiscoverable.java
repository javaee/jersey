/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.media.sse.internal;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.FeatureContext;

import javax.annotation.Priority;

import org.glassfish.jersey.internal.spi.AutoDiscoverable;
import org.glassfish.jersey.internal.spi.ForcedAutoDiscoverable;
import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.media.sse.SseFeature;

/**
 * Jersey {@link org.glassfish.jersey.internal.spi.AutoDiscoverable} responsible for registering {@link SseFeature}.
 *
 * If this feature is not already registered and the property
 * {@link org.glassfish.jersey.media.sse.SseFeature#DISABLE_SSE} is not set to {@code true}, the {@code SseFeature}
 * will be automatically registered.
 *
 * @author Libor Kramolis (libor.kramolis at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Priority(AutoDiscoverable.DEFAULT_PRIORITY)
public final class SseAutoDiscoverable implements ForcedAutoDiscoverable {
    @Override
    public void configure(final FeatureContext context) {
        final Configuration config = context.getConfiguration();
        if (context.getConfiguration().isRegistered(SseFeature.class)) {
            return;
        }

        if (!PropertiesHelper.getValue(
                config.getProperties(), config.getRuntimeType(), SseFeature.DISABLE_SSE, Boolean.FALSE, Boolean.class, null)) {
            context.register(SseFeature.class);
        }
    }
}
