/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jetty.connector;

import java.util.Map;

import org.glassfish.jersey.internal.util.PropertiesClass;
import org.glassfish.jersey.internal.util.PropertiesHelper;

/**
 * Configuration options specific to the Client API that utilizes {@link JettyConnectorProvider}.
 *
 * @author Arul Dhesiaseelan (aruld at acm.org)
 */
@PropertiesClass
public final class JettyClientProperties {

    /**
     * Prevents instantiation.
     */
    private JettyClientProperties() {
        throw new AssertionError("No instances allowed.");
    }

    /**
     * A value of {@code false} indicates the client should handle cookies
     * automatically using HttpClient's default cookie policy. A value
     * of {@code false} will cause the client to ignore all cookies.
     * <p/>
     * The value MUST be an instance of {@link java.lang.Boolean}.
     * If the property is absent the default value is {@code false}
     */
    public static final String DISABLE_COOKIES =
            "jersey.config.jetty.client.disableCookies";

    /**
     * The credential provider that should be used to retrieve
     * credentials from a user.
     *
     * If an {@link org.eclipse.jetty.client.api.Authentication} mechanism is found,
     * it is then used for the given request, returning an {@link org.eclipse.jetty.client.api.Authentication.Result},
     * which is then stored in the {@link org.eclipse.jetty.client.api.AuthenticationStore}
     * so that subsequent requests can be preemptively authenticated.

     * <p/>
     * The value MUST be an instance of {@link
     * org.eclipse.jetty.client.util.BasicAuthentication}.  If
     * the property is absent a default provider will be used.
     */
    public static final String PREEMPTIVE_BASIC_AUTHENTICATION =
            "jersey.config.jetty.client.preemptiveBasicAuthentication";

    /**
     * A value of {@code false} indicates the client disable a hostname verification
     * during SSL Handshake. A client will ignore CN value defined in a certificate
     * that is stored in a truststore.
     * <p/>
     * The value MUST be an instance of {@link java.lang.Boolean}.
     * If the property is absent the default value is {@code true}
     */
    public static final String ENABLE_SSL_HOSTNAME_VERIFICATION =
            "jersey.config.jetty.client.enableSslHostnameVerification";

    /**
     * Get the value of the specified property.
     *
     * If the property is not set or the real value type is not compatible with the specified value type, returns {@code null}.
     *
     * @param properties  Map of properties to get the property value from.
     * @param key         Name of the property.
     * @param type        Type to retrieve the value as.
     * @param <T>         Type of the property value.
     * @return Value of the property or {@code null}.
     *
     * @since 2.8
     */
    public static <T> T getValue(final Map<String, ?> properties, final String key, final Class<T> type) {
        return PropertiesHelper.getValue(properties, key, type, null);
    }

}
