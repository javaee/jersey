/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey;

/**
 * Common (server/client) Jersey configuration properties.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public final class CommonProperties {

    /**
     * If {@code true} then disable feature auto discovery globally on client/server.
     * <p>
     * By default auto discovery is automatically enabled. The value of this property may be overridden by the client/server
     * variant of this property.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String FEATURE_AUTO_DISCOVERY_DISABLE = "jersey.config.disableAutoDiscovery";

    /**
     * If {@code true} then disable configuration of Json Processing (JSR-353) feature.
     * <p>
     * By default Json Processing is automatically enabled. The value of this property may be overridden by the client/server
     * variant of this property.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String JSON_PROCESSING_FEATURE_DISABLE = "jersey.config.disableJsonProcessing";

    /**
     * If {@code true} then disable configuration of MOXy Json feature.
     * <p>
     * By default MOXy Json is automatically enabled. The value of this property may be overridden by the client/server
     * variant of this property.
     * <p>
     * The default value is {@code false}.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String MOXY_JSON_FEATURE_DISABLE = "jersey.config.disableMoxyJson";

    /**
     * An integer value that defines the buffer size used to buffer the outbound message entity in order to
     * determine its size and set the value of HTTP <tt>{@value javax.ws.rs.core.HttpHeaders#CONTENT_LENGTH}</tt> header.
     * <p>
     * If the entity size exceeds the configured buffer size, the buffering would be cancelled and the entity size
     * would not be determined. Value less or equal to zero disable the buffering of the entity at all.
     * </p>
     * The value of this property may be overridden by the client/server variant of this property by defining the suffix
     * to this property "<tt>.server</tt>" or "<tt>.client</tt>"
     * (<tt>{@value}.server</tt> or  <tt>{@value}.client</tt>).
     * <p>
     * The default value is <tt>{@value org.glassfish.jersey.message.internal.CommittingOutputStream#DEFAULT_BUFFER_SIZE}</tt>.
     * </p>
     * <p>
     * The name of the configuration property is <tt>{@value}</tt>.
     * </p>
     */
    public static final String OUTBOUND_CONTENT_LENGTH_BUFFER = "jersey.config.contentLength.buffer";

    /**
     * Prevent instantiation.
     */
    private CommonProperties() {
    }
}
