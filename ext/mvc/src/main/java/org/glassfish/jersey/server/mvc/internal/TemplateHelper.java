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

import java.lang.annotation.Annotation;
import java.nio.charset.Charset;
import java.util.Arrays;
import java.util.List;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Variant;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.internal.util.collection.Refs;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.message.internal.VariantSelector;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ExtendedUriInfo;
import org.glassfish.jersey.server.mvc.MvcFeature;
import org.glassfish.jersey.server.mvc.Template;
import org.glassfish.jersey.server.mvc.Viewable;

import jersey.repackaged.com.google.common.base.Function;
import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Helper class to provide some common functionality related to MVC.
 *
 * @author Michal Gajdos
 */
public final class TemplateHelper {

    private static final Charset DEFAULT_ENCODING = Charset.forName("UTF-8");

    /**
     * Return an absolute path to the given class where segments are separated using {@code delim} character and {@code path}
     * is appended to this path.
     *
     * @param resourceClass class for which an absolute path should be obtained.
     * @param path segment to be appended to the resulting path.
     * @param delim character used for separating path segments.
     * @return an absolute path to the resource class.
     */
    public static String getAbsolutePath(Class<?> resourceClass, String path, char delim) {
        return '/' + resourceClass.getName().replace('.', '/').replace('$', delim) + delim + path;
    }

    /**
     * Get media types for which the {@link org.glassfish.jersey.server.mvc.spi.ResolvedViewable resolved viewable} could be
     * produced.
     *
     * @param containerRequest request to obtain acceptable media types.
     * @param extendedUriInfo uri info to obtain resource method from and its producible media types.
     * @param varyHeaderValue Vary header reference.
     * @return list of producible media types.
     */
    public static List<MediaType> getProducibleMediaTypes(final ContainerRequest containerRequest,
                                                          final ExtendedUriInfo extendedUriInfo,
                                                          final Ref<String> varyHeaderValue) {
        final List<MediaType> producedTypes = getResourceMethodProducibleTypes(extendedUriInfo);
        final MediaType[] mediaTypes = producedTypes.toArray(new MediaType[producedTypes.size()]);

        final List<Variant> variants = VariantSelector.selectVariants(containerRequest, Variant.mediaTypes(mediaTypes)
                .build(), varyHeaderValue == null ? Refs.<String>emptyRef() : varyHeaderValue);

        return Lists.transform(variants, new Function<Variant, MediaType>() {
            @Override
            public MediaType apply(final Variant variant) {
                return MediaTypes.stripQualityParams(variant.getMediaType());
            }
        });
    }

    /**
     * Get template name from given {@link org.glassfish.jersey.server.mvc.Viewable viewable} or return {@code index} if the given
     * viewable doesn't contain a valid template name.
     *
     * @param viewable viewable to obtain template name from.
     * @return {@code non-null}, {@code non-empty} template name.
     */
    public static String getTemplateName(final Viewable viewable) {
        return viewable.getTemplateName() == null || viewable.getTemplateName().isEmpty() ? "index" : viewable.getTemplateName();
    }

    /**
     * Return a list of producible media types of the last matched resource method.
     *
     * @param extendedUriInfo uri info to obtain resource method from.
     * @return list of producible media types of the last matched resource method.
     */
    private static List<MediaType> getResourceMethodProducibleTypes(final ExtendedUriInfo extendedUriInfo) {
        if (extendedUriInfo.getMatchedResourceMethod() != null
                && !extendedUriInfo.getMatchedResourceMethod().getProducedTypes().isEmpty()) {
            return extendedUriInfo.getMatchedResourceMethod().getProducedTypes();
        }
        return Arrays.asList(MediaType.WILDCARD_TYPE);
    }

    /**
     * Extract {@link org.glassfish.jersey.server.mvc.Template template} annotation from given list.
     *
     * @param annotations list of annotations.
     * @return {@link org.glassfish.jersey.server.mvc.Template template} annotation or {@code null} if this annotation is not present.
     */
    public static Template getTemplateAnnotation(final Annotation[] annotations) {
        if (annotations != null && annotations.length > 0) {
            for (Annotation annotation : annotations) {
                if (annotation instanceof Template) {
                    return (Template) annotation;
                }
            }
        }

        return null;
    }

    /**
     * Get output encoding from configuration.
     * @param configuration Configuration.
     * @param suffix Template processor suffix of the
     *               to configuration property {@link org.glassfish.jersey.server.mvc.MvcFeature#ENCODING}.
     *
     * @return Encoding read from configuration properties or a default encoding if no encoding is configured.
     */
    public static Charset getTemplateOutputEncoding(Configuration configuration, String suffix) {
        final String enc = PropertiesHelper.getValue(configuration.getProperties(), MvcFeature.ENCODING + suffix,
                String.class, null);
        if (enc == null) {
            return DEFAULT_ENCODING;
        } else {
            return Charset.forName(enc);
        }
    }

    /**
     * Prevents instantiation.
     */
    private TemplateHelper() {
    }
}
