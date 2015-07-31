/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.mvc.spi;

import java.io.IOException;
import java.io.OutputStream;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.spi.Contract;

/**
 * A view processor.
 * <p/>
 * Implementations of this interface shall be capable of resolving a
 * template name (+ media type) to a template reference that identifies a template supported
 * by the implementation. And, processing the template, identified by template
 * reference and media type, the results of which are written to an output stream.
 * <p/>
 * Implementations can register a view processor as a provider, for
 * example, annotating the implementation class with {@link javax.ws.rs.ext.Provider}
 * or registering an implementing class or instance as a singleton with
 * {@link org.glassfish.jersey.server.ResourceConfig} or {@link javax.ws.rs.core.Application}.
 * <p/>
 * Such view processors could be JSP view processors (supported by the
 * Jersey servlet and filter implementations) or say Freemarker or Velocity
 * view processors (not implemented).
 *
 * @param <T> the type of the template object.
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface TemplateProcessor<T> {

    /**
     * Resolve a template name to a template reference.
     *
     * @param name the template name.
     * @param mediaType requested media type of the template.
     * @return the template reference, otherwise {@code null} if the template name cannot be resolved.
     */
    public T resolve(String name, MediaType mediaType);

    /**
     * Process a template and write the result to an output stream.
     *
     * @param templateReference the template reference. This is obtained by calling the {@link #resolve(String,
     * javax.ws.rs.core.MediaType)} method with a template name and media type.
     * @param viewable the viewable that contains the model to be passed to the template.
     * @param mediaType media type the {@code templateReference} should be transformed into.
     * @param httpHeaders http headers that will be send in the response. Headers can be modified to
     *                    influence response headers before the the first byte is written
     *                    to the {@code out}. After the response buffer is committed the headers modification
     *                    has no effect. Template processor can for example set the content type of
     *                    the response.
     * @param out the output stream to write the result of processing the template.
     * @throws java.io.IOException if there was an error processing the template.
     *
     * @since 2.7
     */
    public void writeTo(T templateReference, Viewable viewable, MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders, OutputStream out) throws IOException;

}
