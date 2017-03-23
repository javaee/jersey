/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.mvc.provider;

import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.net.URL;
import java.util.Collection;

import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;
import org.glassfish.jersey.server.validation.ValidationError;

/**
 * @author Paul Sandoz
 * @author Michal Gajdos
 */
public class TestViewProcessor implements TemplateProcessor<String> {

    @Override
    public String resolve(String path, final MediaType mediaType) {
        final String extension = getExtension();

        if (!path.endsWith(extension)) {
            path = path + extension;
        }

        final URL u = this.getClass().getResource(path);
        if (u == null || !acceptMediaType(mediaType)) {
            return null;
        }
        return path;
    }

    protected boolean acceptMediaType(final MediaType mediaType) {
        return true;
    }

    @Override
    public void writeTo(String templateReference, Viewable viewable, MediaType mediaType,
                        MultivaluedMap<String, Object> httpHeaders, OutputStream out) throws IOException {

        final PrintStream ps = new PrintStream(out);
        ps.print("name=");
        ps.print(getViewProcessorName());
        ps.println();
        ps.print("path=");
        ps.print(templateReference);
        ps.println();
        ps.print("model=");
        ps.print(getModel(viewable.getModel()));
        ps.println();
    }

    private String getModel(final Object model) {
        if (model instanceof Collection) {
            StringBuilder builder = new StringBuilder();
            for (final Object object : (Collection) model) {
                builder.append(getModel(object)).append(',');
            }
            return builder.delete(builder.length() - 1, builder.length()).toString();
        } else if (model instanceof ValidationError) {
            final ValidationError error = (ValidationError) model;
            return error.getMessageTemplate() + "_" + error.getPath() + "_" + error.getInvalidValue();
        }
        return model.toString();
    }

    protected String getExtension() {
        return ".testp";
    }

    protected String getViewProcessorName() {
        return "TestViewProcessor";
    }

}
