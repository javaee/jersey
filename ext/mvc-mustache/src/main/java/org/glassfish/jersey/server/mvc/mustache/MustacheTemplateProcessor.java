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

package org.glassfish.jersey.server.mvc.mustache;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;

import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.AbstractTemplateProcessor;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;

import org.glassfish.hk2.api.ServiceLocator;

import org.jvnet.hk2.annotations.Optional;

import com.github.mustachejava.DefaultMustacheFactory;
import com.github.mustachejava.Mustache;
import com.github.mustachejava.MustacheFactory;

/**
 * {@link TemplateProcessor Template processor} providing support for Mustache templates.
 *
 * @author Michal Gajdos
 * @see MustacheMvcFeature
 * @since 2.3
 */
@Singleton
final class MustacheTemplateProcessor extends AbstractTemplateProcessor<Mustache> {

    private final MustacheFactory factory;

    /**
     * Create an instance of this processor with injected {@link Configuration config} and
     * (optional) {@link ServletContext servlet context}.
     *
     * @param config configuration to configure this processor from.
     * @param serviceLocator service locator to initialize template object factory if needed.
     * @param servletContext (optional) servlet context to obtain template resources from.
     */
    @Inject
    public MustacheTemplateProcessor(final Configuration config, final ServiceLocator serviceLocator,
                                     @Optional final ServletContext servletContext) {
        super(config, servletContext, "mustache", "mustache");

        this.factory = getTemplateObjectFactory(serviceLocator, MustacheFactory.class, new Value<MustacheFactory>() {
            @Override
            public MustacheFactory get() {
                return new DefaultMustacheFactory();
            }
        });
    }

    @Override
    protected Mustache resolve(final String templatePath, final Reader reader) {
        return factory.compile(reader, templatePath);
    }

    @Override
    public void writeTo(final Mustache mustache, final Viewable viewable, final MediaType mediaType,
                        final MultivaluedMap<String, Object> httpHeaders, final OutputStream out) throws IOException {
        Charset encoding = setContentType(mediaType, httpHeaders);
        mustache.execute(new OutputStreamWriter(out, encoding), viewable.getModel()).flush();
    }
}
