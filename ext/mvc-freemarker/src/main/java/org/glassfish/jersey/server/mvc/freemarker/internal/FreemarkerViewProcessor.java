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

package org.glassfish.jersey.server.mvc.freemarker.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.util.List;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.freemarker.FreemarkerProperties;
import org.glassfish.jersey.server.mvc.internal.DefaultTemplateProcessor;

import com.google.common.collect.Lists;

import freemarker.template.Configuration;
import freemarker.template.DefaultObjectWrapper;
import freemarker.template.Template;
import freemarker.template.TemplateException;

/**
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public final class FreemarkerViewProcessor extends DefaultTemplateProcessor<String> {

    private final Configuration configuration;

    @Context
    private UriInfo uriInfo;

    public FreemarkerViewProcessor(@Context final javax.ws.rs.core.Configuration config) {
        super(config);

        configuration = new Configuration();
        configuration.setObjectWrapper(new DefaultObjectWrapper());

        setBasePathFromProperty(FreemarkerProperties.TEMPLATES_BASE_PATH);
    }

    @Override
    public String resolve(final String name, final MediaType mediaType) {
        final Class<?> lastMatchedResourceClass = getLastMatchedResourceClass();

        for (final String templateName : getPossibleTemplateNames(name)) {
            if (lastMatchedResourceClass.getResource(templateName) != null) {
                return templateName;
            }
        }

        return null;
    }

    @Override
    protected List<String> getExtensions() {
        return Lists.newArrayList(".ftl");
    }

    @Override
    public void writeTo(final String templateReference, final Viewable viewable, final MediaType mediaType,
                        final OutputStream out) throws IOException {
        // Commit the status and headers to the HttpServletResponse
        out.flush();

        configuration.setClassForTemplateLoading(getLastMatchedResourceClass(), "/");
        final Template template = configuration.getTemplate(templateReference);

        try {
            template.process(viewable.getModel(), new OutputStreamWriter(out));
        } catch (TemplateException te) {
            throw new ContainerException(te);
        }
    }

    private Class<?> getLastMatchedResourceClass() {
        return uriInfo.getMatchedResources().get(0).getClass();
    }
}
