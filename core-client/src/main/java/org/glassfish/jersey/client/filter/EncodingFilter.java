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

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.SortedSet;
import java.util.logging.Logger;

import javax.ws.rs.client.ClientRequestContext;
import javax.ws.rs.client.ClientRequestFilter;
import javax.ws.rs.core.HttpHeaders;

import javax.inject.Inject;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.internal.LocalizationMessages;
import org.glassfish.jersey.spi.ContentEncoder;

import org.glassfish.hk2.api.ServiceLocator;

import jersey.repackaged.com.google.common.collect.Sets;

/**
 * Client filter adding support for {@link org.glassfish.jersey.spi.ContentEncoder content encoding}. The filter adds
 * list of supported encodings to the Accept-Header values.
 * Supported encodings are determined by looking
 * up all the {@link org.glassfish.jersey.spi.ContentEncoder} implementations registered in the corresponding
 * {@link ClientConfig client configuration}.
 * <p>
 * If {@link ClientProperties#USE_ENCODING} client property is set, the filter will add Content-Encoding header with
 * the value of the property, unless Content-Encoding header has already been set.
 * </p>
 *
 * @author Martin Matula
 */
public final class EncodingFilter implements ClientRequestFilter {
    @Inject
    private ServiceLocator serviceLocator;
    private volatile List<Object> supportedEncodings = null;

    @Override
    public void filter(ClientRequestContext request) throws IOException {
        if (getSupportedEncodings().isEmpty()) {
            return;
        }

        request.getHeaders().addAll(HttpHeaders.ACCEPT_ENCODING, getSupportedEncodings());

        String useEncoding = (String) request.getConfiguration().getProperty(ClientProperties.USE_ENCODING);
        if (useEncoding != null) {
            if (!getSupportedEncodings().contains(useEncoding)) {
                Logger.getLogger(getClass().getName()).warning(LocalizationMessages.USE_ENCODING_IGNORED(
                        ClientProperties.USE_ENCODING, useEncoding, getSupportedEncodings()));
            } else {
                if (request.hasEntity()) {   // don't add Content-Encoding header for requests with no entity
                    if (request.getHeaders().getFirst(HttpHeaders.CONTENT_ENCODING) == null) {
                        request.getHeaders().putSingle(HttpHeaders.CONTENT_ENCODING, useEncoding);
                    }
                }
            }
        }
    }

    List<Object> getSupportedEncodings() {
        // no need for synchronization - in case of a race condition, the property
        // may be set twice, but it does not break anything
        if (supportedEncodings == null) {
            SortedSet<String> se = Sets.newTreeSet();
            List<ContentEncoder> encoders = serviceLocator.getAllServices(ContentEncoder.class);
            for (ContentEncoder encoder : encoders) {
                se.addAll(encoder.getSupportedEncodings());
            }
            supportedEncodings = new ArrayList<Object>(se);
        }
        return supportedEncodings;
    }
}
