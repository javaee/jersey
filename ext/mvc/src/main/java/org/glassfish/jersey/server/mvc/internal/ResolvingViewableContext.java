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

package org.glassfish.jersey.server.mvc.internal;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.ResolvedViewable;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;
import org.glassfish.jersey.server.mvc.spi.ViewableContext;
import org.glassfish.jersey.server.mvc.spi.ViewableContextException;

/**
 * Default implementation of {@link org.glassfish.jersey.server.mvc.spi.ViewableContext viewable context}.
 * <p/>
 * If the template name of given {@link Viewable} is represented as a relative path then the resolving class,
 * and super classes in the inheritance hierarchy, are utilized to generate the absolute template name as follows.
 * <br/>
 * The base path starts with '/' character, followed by the fully qualified class name of the resolving class,
 * with any '.' and '$' characters replaced with a '/' character, followed by a '/' character,
 * followed by the relative template name.
 * <br/>
 * If the absolute template name cannot be resolved into a template reference (see {@link org.glassfish.jersey.server.mvc.spi
 * .TemplateProcessor} and {@link org.glassfish.jersey.server.mvc.spi.ViewableContext}) then the super class of the resolving
 * class is utilized, and is set as the resolving class. Traversal up the inheritance hierarchy proceeds until an absolute
 * template name can be resolved into a template reference, or the Object class is reached,
 * which means the absolute template name could not be resolved and an error will result.
 *
 * @author Michal Gajdos
 */
class ResolvingViewableContext implements ViewableContext {

    /**
     * Resolve given {@link Viewable viewable} using {@link MediaType media type}, {@code resolving class} and
     * {@link TemplateProcessor template processor}.
     *
     * @param viewable viewable to be resolved.
     * @param mediaType media type of te output.
     * @param resourceClass resolving class.
     * @param templateProcessor template processor to be used.
     * @return resolved viewable or {@code null} if the viewable cannot be resolved.
     */
    public ResolvedViewable resolveViewable(final Viewable viewable, final MediaType mediaType,
                                            final Class<?> resourceClass, final TemplateProcessor templateProcessor) {
        if (viewable.isTemplateNameAbsolute()) {
            return resolveAbsoluteViewable(viewable, resourceClass, mediaType, templateProcessor);
        } else {
            if (resourceClass == null) {
                throw new ViewableContextException(LocalizationMessages.TEMPLATE_RESOLVING_CLASS_CANNOT_BE_NULL());
            }

            return resolveRelativeViewable(viewable, resourceClass, mediaType, templateProcessor);
        }
    }

    /**
     * Resolve given {@link Viewable viewable} with absolute template name using {@link MediaType media type} and
     * {@link TemplateProcessor template processor}.
     *
     * @param viewable viewable to be resolved.
     * @param mediaType media type of te output.
     * @param resourceClass resource class.
     * @param templateProcessor template processor to be used.
     * @return resolved viewable or {@code null} if the viewable cannot be resolved.
     */
    @SuppressWarnings("unchecked")
    private ResolvedViewable resolveAbsoluteViewable(final Viewable viewable, Class<?> resourceClass,
                                                     final MediaType mediaType,
                                                     final TemplateProcessor templateProcessor) {
        final Object resolvedTemplateObject = templateProcessor.resolve(viewable.getTemplateName(), mediaType);

        if (resolvedTemplateObject != null) {
            return new ResolvedViewable(templateProcessor, resolvedTemplateObject, viewable, resourceClass, mediaType);
        }

        return null;
    }

    /**
     * Resolve given {@link Viewable viewable} with relative template name using {@link MediaType media type},
     * {@code resolving class} and {@link TemplateProcessor template processor}.
     *
     * @param viewable viewable to be resolved.
     * @param mediaType media type of te output.
     * @param resolvingClass resolving class.
     * @param templateProcessor template processor to be used.
     * @return resolved viewable or {@code null} if the viewable cannot be resolved.
     */
    @SuppressWarnings("unchecked")
    private ResolvedViewable resolveRelativeViewable(final Viewable viewable, final Class<?> resolvingClass,
                                                     final MediaType mediaType, final TemplateProcessor templateProcessor) {
        final String path = TemplateHelper.getTemplateName(viewable);

        // Find in directories.
        for (Class c = resolvingClass; c != Object.class; c = c.getSuperclass()) {
            final String absolutePath = TemplateHelper.getAbsolutePath(c, path, '/');
            final Object resolvedTemplateObject = templateProcessor.resolve(absolutePath, mediaType);

            if (resolvedTemplateObject != null) {
                return new ResolvedViewable(templateProcessor, resolvedTemplateObject, viewable, c, mediaType);
            }
        }

        // Find in flat files.
        for (Class c = resolvingClass; c != Object.class; c = c.getSuperclass()) {
            final String absolutePath = TemplateHelper.getAbsolutePath(c, path, '.');
            final Object resolvedTemplateObject = templateProcessor.resolve(absolutePath, mediaType);

            if (resolvedTemplateObject != null) {
                return new ResolvedViewable(templateProcessor, resolvedTemplateObject, viewable, c, mediaType);
            }
        }

        return null;
    }

}
