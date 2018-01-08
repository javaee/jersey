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

package org.glassfish.jersey.examples.httppatch;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.annotation.Annotation;
import java.lang.reflect.Method;

import javax.ws.rs.GET;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedHashMap;
import javax.ws.rs.core.UriInfo;
import javax.ws.rs.ext.MessageBodyWriter;
import javax.ws.rs.ext.ReaderInterceptor;
import javax.ws.rs.ext.ReaderInterceptorContext;

import org.glassfish.jersey.message.MessageBodyWorkers;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.fge.jsonpatch.JsonPatch;
import com.github.fge.jsonpatch.JsonPatchException;

/**
 * JAX-RS reader interceptor that implements server-side PATCH support.
 *
 * @author Gerard Davison (gerard.davison at oracle.com)
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class PatchingInterceptor implements ReaderInterceptor {

    private final UriInfo uriInfo;
    private final MessageBodyWorkers workers;

    /**
     * {@code PatchingInterceptor} injection constructor.
     *
     * @param uriInfo {@code javax.ws.rs.core.UriInfo} proxy instance.
     * @param workers {@link org.glassfish.jersey.message.MessageBodyWorkers} message body workers.
     */
    public PatchingInterceptor(@Context UriInfo uriInfo, @Context MessageBodyWorkers workers) {
        this.uriInfo = uriInfo;
        this.workers = workers;
    }

    @SuppressWarnings("unchecked")
    @Override
    public Object aroundReadFrom(ReaderInterceptorContext readerInterceptorContext) throws IOException, WebApplicationException {
        // Get the resource we are being called on, and find the GET method
        Object resource = uriInfo.getMatchedResources().get(0);

        Method found = null;
        for (Method next : resource.getClass().getMethods()) {
            if (next.getAnnotation(GET.class) != null) {
                found = next;
                break;
            }
        }

        if (found == null) {
            throw new InternalServerErrorException("No matching GET method on resource");
        }

        // Invoke the get method to get the state we are trying to patch
        Object bean;
        try {
            bean = found.invoke(resource);
        } catch (Exception e) {
            throw new WebApplicationException(e);
        }

        // Convert this object to a an array of bytes
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        MessageBodyWriter bodyWriter =
                workers.getMessageBodyWriter(bean.getClass(), bean.getClass(),
                        new Annotation[0], MediaType.APPLICATION_JSON_TYPE);

        bodyWriter.writeTo(bean, bean.getClass(), bean.getClass(),
                new Annotation[0], MediaType.APPLICATION_JSON_TYPE,
                new MultivaluedHashMap<String, Object>(), baos);


        // Use the Jackson 2.x classes to convert both the incoming patch
        // and the current state of the object into a JsonNode / JsonPatch
        ObjectMapper mapper = new ObjectMapper();
        JsonNode serverState = mapper.readValue(baos.toByteArray(), JsonNode.class);
        JsonNode patchAsNode = mapper.readValue(readerInterceptorContext.getInputStream(), JsonNode.class);
        JsonPatch patch = JsonPatch.fromJson(patchAsNode);

        try {
            // Apply the patch
            JsonNode result = patch.apply(serverState);

            // Stream the result & modify the stream on the readerInterceptor
            ByteArrayOutputStream resultAsByteArray = new ByteArrayOutputStream();
            mapper.writeValue(resultAsByteArray, result);
            readerInterceptorContext.setInputStream(new ByteArrayInputStream(resultAsByteArray.toByteArray()));

            // Pass control back to the Jersey code
            return readerInterceptorContext.proceed();
        } catch (JsonPatchException ex) {
            throw new InternalServerErrorException("Error applying patch.", ex);
        }
    }
}
