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
package org.glassfish.jersey.media.sse;

import javax.ws.rs.core.Configurable;
import javax.ws.rs.core.Feature;
import javax.ws.rs.core.FeatureContext;
import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.internal.util.Property;
import org.glassfish.jersey.media.sse.internal.SseBinder;
import org.glassfish.jersey.media.sse.internal.SseEventSinkValueParamProvider;
import org.glassfish.jersey.server.spi.internal.ValueParamProvider;


/**
 * A JAX-RS {@link Feature feature} that enables Server-Sent Events support.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class SseFeature implements Feature {

    /**
     * {@link String} representation of Server sent events media type. ("{@value}").
     */
    public static final String SERVER_SENT_EVENTS = "text/event-stream";
    /**
     * Server sent events media type.
     */
    public static final MediaType SERVER_SENT_EVENTS_TYPE = MediaType.valueOf(SERVER_SENT_EVENTS);

    /**
     * If {@code true} then {@link org.glassfish.jersey.media.sse.SseFeature SSE Feature} automatic registration is
     * suppressed.
     * <p>
     * Since Jersey 2.8, by default SSE Feature is automatically enabled when the SSE module is on class path.
     * You can override this behavior by setting this property to {@code true}.
     * The value of this property may be specifically overridden on either client or server side by setting the
     * {@link #DISABLE_SSE_CLIENT client} or {@link #DISABLE_SSE_SERVER server} variant of this property.
     * </p>
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.8
     */
    @Property
    public static final String DISABLE_SSE = "jersey.config.media.sse.disable";
    /**
     * Client-side variant of {@link #DISABLE_SSE} property.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.8
     */
    @Property
    public static final String DISABLE_SSE_CLIENT = "jersey.config.client.media.sse.disable";

    /**
     * Server-side variant of {@link #DISABLE_SSE} property.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     *
     * @since 2.8
     */
    @Property
    public static final String DISABLE_SSE_SERVER = "jersey.config.server.media.sse.disable";

    /**
     * A "reconnection not set" value for the SSE reconnect delay set via {@code retry} field.
     *
     * @since 2.3
     */
    public static final long RECONNECT_NOT_SET = -1;

    /**
     * {@code "Last-Event-ID"} HTTP request header name as defined by
     * <a href="http://www.w3.org/TR/eventsource/#last-event-id">SSE specification</a>.
     *
     * @since 2.3
     */
    public static final String LAST_EVENT_ID_HEADER = "Last-Event-ID";


    @Override
    public boolean configure(final FeatureContext context) {
        if (context.getConfiguration().isEnabled(this.getClass())) {
            return false;
        }

        switch (context.getConfiguration().getRuntimeType()) {
            case CLIENT:
                context.register(EventInputReader.class);
                context.register(InboundEventReader.class);
                break;
            case SERVER:
                context.register(OutboundEventWriter.class);
                context.register(new SseBinder());
                context.register(SseEventSinkValueParamProvider.class, ValueParamProvider.class);
                break;
        }
        return true;
    }

    /**
     * Safely register a {@code SseFeature} in a given configurable context.
     *
     * @param ctx configurable context in which the SSE feature should be registered.
     * @return updated configurable context.
     */
    static <T extends Configurable<T>> T register(final T ctx) {
        if (!ctx.getConfiguration().isRegistered(SseFeature.class)) {
            ctx.register(SseFeature.class);
        }

        return ctx;
    }
}
