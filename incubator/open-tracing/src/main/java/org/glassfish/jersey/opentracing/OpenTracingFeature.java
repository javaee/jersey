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

package org.glassfish.jersey.opentracing;

import java.util.logging.Logger;

import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;

import org.glassfish.jersey.Beta;

import io.opentracing.util.GlobalTracer;

/**
 * A feature that enables OpenTracing support on server and client.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 * @since 2.26
 */
@Beta
public class OpenTracingFeature implements Feature {
    private static final Logger LOGGER = Logger.getLogger(OpenTracingFeature.class.getName());
    private final Verbosity verbosity;

    /**
     * Creates feature instance with default ({@link Verbosity#INFO} verbosity level.
     */
    public OpenTracingFeature() {
        verbosity = Verbosity.INFO;
    }

    /**
     * Creates feature instance with given ({@link Verbosity} level.
     * @param verbosity desired level of logging verbosity
     */
    public OpenTracingFeature(Verbosity verbosity) {
        this.verbosity = verbosity;
    }

    /**
     * Stored span's {@link ContainerRequestContext} property key.
     */
    public static final String SPAN_CONTEXT_PROPERTY = "span";

    /**
     * Default resource span name.
     */
    public static final String DEFAULT_RESOURCE_SPAN_NAME = "jersey-resource";

    /**
     * Default child span name.
     */
    public static final String DEFAULT_CHILD_SPAN_NAME = "jersey-resource-app";

    /**
     * Default request "root" span name.
     */
    public static final String DEFAULT_REQUEST_SPAN_NAME = "jersey-server";

    @Override
    public boolean configure(FeatureContext context) {
        if (!GlobalTracer.isRegistered()) {
            LOGGER.warning(LocalizationMessages.OPENTRACING_TRACER_NOT_REGISTERED());
        }

        switch (context.getConfiguration().getRuntimeType()) {
            case CLIENT:
                context.register(OpenTracingClientRequestFilter.class).register(OpenTracingClientResponseFilter.class);
                break;
            case SERVER:
                context.register(new OpenTracingApplicationEventListener(verbosity));
        }
        return true;
    }

    /**
     * OpenTracing Jersey event logging verbosity.
     */
    public enum Verbosity {
        /**
         * Only logs basic Jersey processing related events.
         */
        INFO,

        /**
         * Logs more fine grained events related to Jersey processing.
         */
        TRACE
    }

}
