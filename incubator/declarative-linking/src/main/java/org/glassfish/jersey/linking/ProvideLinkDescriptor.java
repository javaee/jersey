/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.linking;

import java.lang.annotation.Annotation;
import java.net.URI;
import java.util.HashMap;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.mapping.ResourceMappingContext;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.glassfish.jersey.server.model.ResourceMethod;

/**
 * Utility to work with {@link ProvideLink} annotations.
 *
 * @author Leonard Brünings
 */
public class ProvideLinkDescriptor implements InjectLinkDescriptor {
    private final ProvideLink provideLink;
    private final ResourceMethod resource;

    private final Annotation parentAnnotation;

    private final Map<String, String> bindings;

    /**
     * c'tor
     *
     * @param resource the annotated resource method
     * @param provideLink the annotaion
     * @param parentAnnotation the parent annotation if present or {@code null}
     */
    public ProvideLinkDescriptor(ResourceMethod resource, ProvideLink provideLink, Annotation parentAnnotation) {
        this.provideLink = provideLink;
        this.resource = resource;
        this.parentAnnotation = parentAnnotation;
        bindings = new HashMap<>();
        for (Binding binding : provideLink.bindings()) {
            bindings.put(binding.name(), binding.value());
        }
    }

    /**
     * @return the annotation
     */
    public ProvideLink getProvideLink() {
        return provideLink;
    }

    /**
     * @return the annotated resource method
     */
    public ResourceMethod getResource() {
        return resource;
    }

    /**
     * Get the style
     *
     * @return the style
     */
    public InjectLink.Style getLinkStyle() {
        return provideLink.style();
    }

    /**
     * Get the link template, either directly from the value() or from the
     * {@code @Path} of the class referenced in resource()
     *
     * @return the link template
     */
    @Override
    public String getLinkTemplate(ResourceMappingContext rmc) {
        String template = null;
        ResourceMappingContext.Mapping map = rmc.getMapping(resource.getInvocable().getHandler().getHandlerClass());
        if (map != null) {
            template = map.getTemplate().getTemplate();
        } else {
            // extract template from specified class' @Path annotation
            Path path = resource.getInvocable().getHandler().getHandlerClass().getAnnotation(Path.class);
            template = path == null ? "" : path.value();
        }
        StringBuilder builder = new StringBuilder(template);

        Path methodPath = resource.getInvocable().getDefinitionMethod().getAnnotation(Path.class);
        if (methodPath != null) {
            String methodTemplate = methodPath.value();

            if (!(template.endsWith("/") || methodTemplate.startsWith("/"))) {
                builder.append("/");
            }
            builder.append(methodTemplate);
        }

        CharSequence querySubString = InjectLinkFieldDescriptor.extractQueryParams(
                new AnnotatedMethod(resource.getInvocable().getDefinitionMethod()));

        if (querySubString.length() > 0) {
            builder.append("{?");
            builder.append(querySubString);
            builder.append("}");
        }

        template = builder.toString();

        return template;
    }

    /**
     * Get the binding as an EL expression for a particular URI template parameter
     *
     * @param name binding name.
     * @return the EL binding.
     */
    @Override
    public String getBinding(String name) {
        return bindings.get(name);
    }

    /**
     * Get the condition.
     *
     * @return the condition
     */
    @Override
    public String getCondition() {
        return provideLink.condition();
    }

    /**
     * Builds a link from a {@link URI}.
     *
     * @param uri base URI
     * @return the {@link Link} instance
     */
    public Link getLink(URI uri) {
        return ProvideLink.Util.buildLinkFromUri(uri, provideLink);
    }

    /**
     * @return the parent annotation or {@code null}
     */
    public Annotation getParentAnnotation() {
        return parentAnnotation;
    }
}
