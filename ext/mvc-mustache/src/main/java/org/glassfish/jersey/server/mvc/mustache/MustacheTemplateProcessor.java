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

package org.glassfish.jersey.server.mvc.mustache;

import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.util.Map;
import java.util.concurrent.ConcurrentMap;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Provider;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.collection.DataStructures;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;

import org.jvnet.hk2.annotations.Optional;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * {@link TemplateProcessor Template processor} providing support for Mustache templates.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 * @see MustacheMvcFeature
 * @since 2.3
 */
@Provider
@Singleton
final class MustacheTemplateProcessor implements TemplateProcessor<Mustache> {

    private static final String SUFFIX = ".mustache";

    private final ConcurrentMap<String, Mustache> cache;

    private final ServletContext servletContext;

    private final String basePath;
    private final MustacheFactory factory;

    /**
     * Create an instance of this processor with injected {@link Configuration config} and
     * (optional) {@link ServletContext servlet context}.
     *
     * @param config configuration to configure this processor from.
     * @param servletContext (optional) servlet context to obtain template resources from.
     */
    @Inject
    public MustacheTemplateProcessor(final Configuration config, @Optional final ServletContext servletContext) {
        this.servletContext = servletContext;
        this.factory = new DefaultMustacheFactory();

        final Map<String,Object> properties = config.getProperties();

        this.basePath = !properties.containsKey(MustacheMvcFeature.TEMPLATE_BASE_PATH) ? "" :
                (String) config.getProperty(MustacheMvcFeature.TEMPLATE_BASE_PATH);

        this.cache = PropertiesHelper.isProperty(properties, MustacheMvcFeature.CACHING_TEMPLATES_ENABLED) ?
                DataStructures.<String, Mustache>createConcurrentMap() : null;
    }

    @Override
    public Mustache resolve(final String name, final MediaType mediaType) {
        // Look into the cache if enabled.
        if (cache != null) {
            if (!cache.containsKey(name)) {
                cache.putIfAbsent(name, resolve(name));
            }
            return cache.get(name);
        }

        return resolve(name);
    }

    /**
     * Resolve a template name to a template reference.
     *
     * @param name the template name.
     * @return the template reference, otherwise {@code null} if the template name cannot be resolved.
     */
    private Mustache resolve(final String name) {
        final String template = name.endsWith(SUFFIX) ? basePath + name : basePath + name + SUFFIX;

        Reader reader = null;

        // ServletContext.
        if (servletContext != null) {
            final InputStream stream = servletContext.getResourceAsStream(template);
            reader = stream != null ? new InputStreamReader(stream) : null;
        }

        // Classloader.
        if (reader == null) {
            final InputStream stream = getClass().getResourceAsStream(template);
            reader = stream != null ? new InputStreamReader(stream) : null;
        }

        // File-system path.
        if (reader == null) {
            try {
                reader = new FileReader(template);
            } catch (FileNotFoundException fnfe) {
                // NOOP.
            }
        }

        return reader != null ? factory.compile(reader, name) : null;
    }

    @Override
    public void writeTo(final Mustache mustache, final Viewable viewable, final MediaType mediaType,
                        final OutputStream out) throws IOException {
        mustache.execute(new OutputStreamWriter(out), viewable.getModel()).flush();
    }
}
