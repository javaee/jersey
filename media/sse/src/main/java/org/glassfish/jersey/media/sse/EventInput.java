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

package org.glassfish.jersey.media.sse;

import java.io.InputStream;
import java.lang.annotation.Annotation;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.client.ChunkParser;
import org.glassfish.jersey.client.ChunkedInput;
import org.glassfish.jersey.internal.PropertiesDelegate;
import org.glassfish.jersey.message.MessageBodyWorkers;

/**
 * Inbound Server-Sent Events channel.
 *
 * The input channel lets you serially read & consume SSE events as they arrive.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class EventInput extends ChunkedInput<InboundEvent> {
    /**
     * SSE event chunk parser - SSE chunks are delimited with a fixed "\n\n" and "\r\n\r\n" delimiter in the response stream.
     */
    private static final ChunkParser SSE_EVENT_PARSER = ChunkedInput.createMultiParser("\n\n", "\r\n\r\n");

    /**
     * Package-private constructor used by the {@link org.glassfish.jersey.client.ChunkedInputReader}.
     *
     * @param inputStream        response input stream.
     * @param annotations        annotations associated with response entity.
     * @param mediaType          response entity media type.
     * @param headers            response headers.
     * @param messageBodyWorkers message body workers.
     * @param propertiesDelegate properties delegate for this request/response.
     */
    EventInput(InputStream inputStream,
               Annotation[] annotations,
               MediaType mediaType,
               MultivaluedMap<String, String> headers,
               MessageBodyWorkers messageBodyWorkers,
               PropertiesDelegate propertiesDelegate) {
        super(InboundEvent.class, inputStream, annotations, mediaType, headers, messageBodyWorkers, propertiesDelegate);

        super.setParser(SSE_EVENT_PARSER);
    }
}
