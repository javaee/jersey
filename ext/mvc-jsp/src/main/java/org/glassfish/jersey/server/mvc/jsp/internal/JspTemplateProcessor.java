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

package org.glassfish.jersey.server.mvc.jsp.internal;

import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.PrintWriter;
import java.net.MalformedURLException;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;

import javax.inject.Inject;
import javax.inject.Provider;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;

import org.glassfish.jersey.internal.util.ExtendedLogger;
import org.glassfish.jersey.internal.util.collection.Ref;
import org.glassfish.jersey.message.internal.TracingLogger;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.internal.DefaultTemplateProcessor;
import org.glassfish.jersey.server.mvc.jsp.JspProperties;
import org.glassfish.jersey.server.mvc.spi.TemplateProcessor;

import com.google.common.collect.Lists;

/**
 * A JSP template processor able to process resources obtained through {@link ServletContext servlet context}.
 *
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class JspTemplateProcessor extends DefaultTemplateProcessor<String> {

    private static final ExtendedLogger logger =
            new ExtendedLogger(Logger.getLogger(JspTemplateProcessor.class.getName()), Level.FINEST);

    @Context
    private ServletContext servletContext;

    @Inject
    private Provider<Ref<HttpServletRequest>> requestProviderRef;
    @Inject
    private Provider<Ref<HttpServletResponse>> responseProviderRef;
    @Inject
    private Provider<ContainerRequest> containerRequestProvider;

    /**
     * Creates new {@link TemplateProcessor template processor} for JSP.
     *
     * @param config configuration to obtain {@value JspProperties#TEMPLATES_BASE_PATH} init param from.
     */
    public JspTemplateProcessor(@Context final Configuration config) {
        super(config);

        setBasePathFromProperty(JspProperties.TEMPLATES_BASE_PATH);
    }

    @Override
    public String resolve(final String name, final MediaType mediaType) {
        if (servletContext == null) {
            return null;
        }

        try {
            for (final String templateName : getPossibleTemplateNames(name)) {
                if (servletContext.getResource(templateName) != null) {
                    return templateName;
                }
            }
        } catch (MalformedURLException ex) {
            logger.log(Level.FINE, LocalizationMessages.RESOURCE_PATH_NOT_IN_CORRECT_FORM(getTemplateName(name)));
        }

        return null;
    }

    @Override
    protected List<String> getExtensions() {
        return Lists.newArrayList(".jsp");
    }

    @Override
    public void writeTo(String templateReference, Viewable viewable, MediaType mediaType, final OutputStream out) throws IOException {
        TracingLogger tracingLogger = TracingLogger.getInstance(containerRequestProvider.get().getPropertiesDelegate());
        if (tracingLogger.isLogEnabled(MvcJspEvent.JSP_FORWARD)) {
            tracingLogger.log(MvcJspEvent.JSP_FORWARD, templateReference, viewable.getModel());
        }

        RequestDispatcher dispatcher = servletContext.getRequestDispatcher(templateReference);
        if (dispatcher == null) {
            throw new ContainerException(LocalizationMessages.NO_REQUEST_DISPATCHER_FOR_RESOLVED_PATH(templateReference));
        }

        dispatcher = new RequestDispatcherWrapper(dispatcher, getBasePath(), viewable);

        // OutputStream and Writer for HttpServletResponseWrapper.
        final ServletOutputStream responseStream = new ServletOutputStream() {
            @Override
            public void write(final int b) throws IOException {
                out.write(b);
            }
        };
        final PrintWriter responseWriter = new PrintWriter(new OutputStreamWriter(responseStream));

        try {
            dispatcher.forward(requestProviderRef.get().get(), new HttpServletResponseWrapper(responseProviderRef.get().get()) {

                @Override
                public ServletOutputStream getOutputStream() throws IOException {
                    return responseStream;
                }

                @Override
                public PrintWriter getWriter() throws IOException {
                    return responseWriter;
                }
            });
        } catch (Exception e) {
            throw new ContainerException(e);
        } finally {
            responseWriter.flush();
        }
    }

    /**
     * MVC-JSP side tracing events.
     */
    private static enum MvcJspEvent implements TracingLogger.Event {
        JSP_FORWARD(TracingLogger.Level.SUMMARY, "MVC", "Forwarding view to JSP page [%s], model %s");

        private final TracingLogger.Level level;
        private final String category;
        private final String messageFormat;

        private MvcJspEvent(TracingLogger.Level level, String category, String messageFormat) {
            this.level = level;
            this.category = category;
            this.messageFormat = messageFormat;
        }

        @Override
        public String category() {
            return category;
        }

        @Override
        public TracingLogger.Level level() {
            return level;
        }

        @Override
        public String messageFormat() {
            return messageFormat;
        }
    }

}
