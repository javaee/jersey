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

package org.glassfish.jersey.server.mvc.internal;

import java.util.List;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.ext.Provider;

import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;

import com.google.common.collect.Lists;

/**
 * Implementation of {@link TemplateProcessor} common to cases where a base path in template name should be used when resolving
 * a viewable.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
@Provider
public abstract class DefaultTemplateProcessor<T> implements TemplateProcessor<T> {

    private final Configuration configuration;

    private String basePath = "";

    /**
     * Creates an instance of {@link DefaultTemplateProcessor}.
     *
     * @param configuration {@code non-null} configuration to obtain properties from.
     */
    protected DefaultTemplateProcessor(final Configuration configuration) {
        this.configuration = configuration;
    }

    /**
     * Get configured base path of this processor's templates.
     *
     * @return configured base path or an empty string.
     */
    protected String getBasePath() {
        return basePath;
    }

    /**
     * Get template name prefixed with the base path.
     *
     * @param template template name.
     * @return base path + template name.
     */
    protected String getTemplateName(final String template) {
        return basePath + template;
    }

    /**
     * Get all possible template names which should be looked for during resolving
     * a {@link org.glassfish.jersey.server.mvc.Viewable viewable}.
     *
     * @param template template name.
     * @return possible template names - template name, combination of template name and extensions.
     */
    protected List<String> getPossibleTemplateNames(final String template) {
        final List<String> possibleTemplateNames = Lists.newArrayList();
        final String templateName = getTemplateName(template);

        possibleTemplateNames.add(templateName);

        for (final String extension : getExtensions()) {
            possibleTemplateNames.add(templateName + extension);
        }

        return possibleTemplateNames;
    }

    /**
     * Get a list of possible file extensions for this {@link TemplateProcessor template processor}.
     *
     * @return list of extensions.
     */
    protected abstract List<String> getExtensions();

    /**
     * Remove trailing slash character from the given path.
     *
     * @param path path to remove the trailing slash from.
     * @return path without the trailing slash.
     */
    private String removeTrailingSlash(final String path) {
        return path.endsWith("/") ? path.substring(0, path.length() - 1) : path;
    }

    /**
     * Set the base path field of this processor using value obtained from {@link Configuration configuration} with given
     * {@code basePathPropertyName}.
     *
     * @param basePathPropertyName name of the configuration property referencing base path in the configuration.
     */
    protected void setBasePathFromProperty(final String basePathPropertyName) {
        final String path = (String) configuration.getProperties().get(basePathPropertyName);

        if (path == null) {
            this.basePath = "";
        } else if (path.charAt(0) == '/') {
            this.basePath = removeTrailingSlash(path);
        } else {
            this.basePath = "/" + removeTrailingSlash(path);
        }
    }
}
