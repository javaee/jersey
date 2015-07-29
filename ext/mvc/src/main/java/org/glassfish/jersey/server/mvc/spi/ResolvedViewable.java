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

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.server.mvc.Viewable;

/**
 * A resolved {@link org.glassfish.jersey.server.mvc.Viewable viewable}.
 * <p/>
 * A resolved viewable is obtained from the resolving methods on {@link org.glassfish.jersey.server.mvc.spi.ViewableContext}
 * and has associated with it a {@link TemplateProcessor} that is capable of processing a template identified by a template
 * reference.
 *
 * @param <T> the type of the resolved template object.
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public final class ResolvedViewable<T> extends Viewable {

    private final TemplateProcessor<T> viewProcessor;

    private final T templateReference;

    private final MediaType mediaType;

    private final Class<?> resolvingClass;


    /**
     * Create a resolved viewable.
     *
     * @param viewProcessor the view processor that resolved a template name to a template reference.
     * @param templateReference the template reference.
     * @param viewable the viewable that is resolved.
     * @param mediaType media type the {@code templateReference} should be transformed into.
     */
    public ResolvedViewable(TemplateProcessor<T> viewProcessor, T templateReference, Viewable viewable, MediaType mediaType) {
        this(viewProcessor, templateReference, viewable, null, mediaType);
    }

    /**
     * Create a resolved viewable.
     *
     * @param viewProcessor the view processor that resolved a template name to a template reference.
     * @param templateReference the template reference.
     * @param viewable the viewable that is resolved.
     * @param resolvingClass the resolving class that was used to resolve a relative template name into an absolute template name.
     * @param mediaType media type the {@code templateReference} should be transformed into.
     */
    public ResolvedViewable(TemplateProcessor<T> viewProcessor, T templateReference, Viewable viewable,
                            Class<?> resolvingClass, MediaType mediaType) {
        super(viewable.getTemplateName(), viewable.getModel());

        this.viewProcessor = viewProcessor;
        this.templateReference = templateReference;
        this.mediaType = mediaType;
        this.resolvingClass = resolvingClass;
    }

    /**
     * Write the resolved viewable.
     * <p/>
     * This method defers to
     * {@link TemplateProcessor#writeTo(Object, org.glassfish.jersey.server.mvc.Viewable, javax.ws.rs.core.MediaType,
     * javax.ws.rs.core.MultivaluedMap, java.io.OutputStream)}
     * to write the viewable utilizing the template reference.
     *
     * @param out the output stream that the view processor writes to.
     * @throws java.io.IOException if there was an error processing the template.
     */
    public void writeTo(OutputStream out, final MultivaluedMap<String, Object> httpHeaders) throws IOException {
        viewProcessor.writeTo(templateReference, this, mediaType, httpHeaders, out);
    }

    /**
     * Get the media type for which the {@link TemplateProcessor view processor} resolved the template reference.
     *
     * @return final {@link javax.ws.rs.core.MediaType media type} of the resolved viewable.
     */
    public MediaType getMediaType() {
        return mediaType;
    }


    /**
     * Get resolving class.
     *
     * @return Resolving class.
     */
    public Class<?> getResolvingClass() {
        return resolvingClass;
    }
}
