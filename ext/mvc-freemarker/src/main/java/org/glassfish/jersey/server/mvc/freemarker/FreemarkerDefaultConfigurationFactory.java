/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.mvc.freemarker;


import javax.inject.Inject;
import javax.servlet.ServletContext;
import java.io.File;
import java.io.IOException;
import java.util.List;

import freemarker.cache.ClassTemplateLoader;
import freemarker.cache.FileTemplateLoader;
import freemarker.cache.MultiTemplateLoader;
import freemarker.cache.TemplateLoader;
import freemarker.cache.WebappTemplateLoader;
import freemarker.template.Configuration;
import org.jvnet.hk2.annotations.Optional;
import jersey.repackaged.com.google.common.collect.Lists;


/**
 * Handy {@link FreemarkerConfigurationFactory} that supplies a minimally
 * configured {@link freemarker.template.Configuration Configuration} able to
 * create {@link freemarker.template.Template Freemarker templates}.
 * The recommended method to provide custom Freemarker configuration is to
 * sub-class this class, further customize the
 * {@link freemarker.template.Configuration configuration} as desired in that
 * class, and then register the sub-class with the {@link FreemarkerMvcFeature}
 * TEMPLATE_OBJECT_FACTORY property.
 *
 * @author Jeff Wilde (jeff.wilde at complicatedrobot.com)
 */
public class FreemarkerDefaultConfigurationFactory implements FreemarkerConfigurationFactory {

    protected final Configuration configuration;

    @Inject
    public FreemarkerDefaultConfigurationFactory(@Optional final ServletContext servletContext) {
        super();

        // Create different loaders.
        final List<TemplateLoader> loaders = Lists.newArrayList();
        if (servletContext != null) {
            loaders.add(new WebappTemplateLoader(servletContext));
        }
        loaders.add(new ClassTemplateLoader(FreemarkerDefaultConfigurationFactory.class, "/"));
        try {
            loaders.add(new FileTemplateLoader(new File("/")));
        } catch (IOException e) {
            // NOOP
        }

        // Create Base configuration.
        configuration = new Configuration();
        configuration.setTemplateLoader(new MultiTemplateLoader(loaders.toArray(new TemplateLoader[loaders.size()])));

    }

    @Override
    public Configuration getConfiguration() {
        return configuration;
    }

}
