/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message.internal;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.Providers;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.glassfish.hk2.api.Factory;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.FeaturesAndProperties;
import org.glassfish.jersey.message.XmlHeader;

import org.xml.sax.InputSource;

import com.google.common.base.Supplier;
import com.google.common.base.Suppliers;

/**
 * A base class for implementing JAXB-based readers and writers.
 *
 * @param <T> Java type supported by the provider.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class AbstractJaxbProvider<T> extends AbstractMessageReaderWriterProvider<T> {

    private static final Map<Class, JAXBContext> jaxbContexts =
            new WeakHashMap<Class, JAXBContext>();
    private final Providers ps;
    private final boolean fixedMediaType;
    private final Supplier<ContextResolver<JAXBContext>> mtContext;
    private final Supplier<ContextResolver<Unmarshaller>> mtUnmarshaller;
    private final Supplier<ContextResolver<Marshaller>> mtMarshaller;
    private Supplier<Boolean> formattedOutput = Suppliers.ofInstance(Boolean.FALSE);
    private Supplier<Boolean> xmlRootElementProcessing = Suppliers.ofInstance(Boolean.FALSE);

    public AbstractJaxbProvider(final Providers ps) {
        this(ps, null);
    }

    public AbstractJaxbProvider(final Providers ps, final MediaType mt) {
        this.ps = ps;

        fixedMediaType = mt != null;
        if (fixedMediaType) {
            this.mtContext = Suppliers.memoize(new Supplier<ContextResolver<JAXBContext>>() {

                @Override
                public ContextResolver<JAXBContext> get() {
                    return ps.getContextResolver(JAXBContext.class, mt);
                }
            });
            this.mtUnmarshaller = Suppliers.memoize(new Supplier<ContextResolver<Unmarshaller>>() {

                @Override
                public ContextResolver<Unmarshaller> get() {
                    return ps.getContextResolver(Unmarshaller.class, mt);
                }
            });
            this.mtMarshaller = Suppliers.memoize(new Supplier<ContextResolver<Marshaller>>() {

                @Override
                public ContextResolver<Marshaller> get() {
                    return ps.getContextResolver(Marshaller.class, mt);
                }
            });
        } else {
            this.mtContext = null;
            this.mtUnmarshaller = null;
            this.mtMarshaller = null;
        }
    }

    @Context
    public void setConfiguration(final Factory<FeaturesAndProperties> fp) {
        formattedOutput = Suppliers.memoize(new Supplier<Boolean>() {

            @Override
            public Boolean get() {
                return Boolean.valueOf(fp.provide().isProperty(MessageProperties.XML_FORMAT_OUTPUT));
            }
        });

        xmlRootElementProcessing = Suppliers.memoize(new Supplier<Boolean>() {

            @Override
            public Boolean get() {
                return Boolean.valueOf(fp.provide().isProperty(MessageProperties.JAXB_PROCESS_XML_ROOT_ELEMENT));
            }
        });
    }

    protected boolean isSupported(MediaType m) {
        return true;
    }

    protected final Unmarshaller getUnmarshaller(Class type, MediaType mt) throws JAXBException {
        if (fixedMediaType) {
            return getUnmarshaller(type);
        }

        final ContextResolver<Unmarshaller> uncr = ps.getContextResolver(Unmarshaller.class, mt);
        if (uncr != null) {
            Unmarshaller u = uncr.getContext(type);
            if (u != null) {
                return u;
            }
        }

        return getJAXBContext(type, mt).createUnmarshaller();
    }

    private Unmarshaller getUnmarshaller(Class type) throws JAXBException {
        final ContextResolver<Unmarshaller> resolver = mtUnmarshaller.get();
        if (resolver != null) {
            Unmarshaller u = resolver.getContext(type);
            if (u != null) {
                return u;
            }
        }

        return getJAXBContext(type).createUnmarshaller();
    }

    protected final Marshaller getMarshaller(Class type, MediaType mt) throws JAXBException {
        if (fixedMediaType) {
            return getMarshaller(type);
        }

        final ContextResolver<Marshaller> mcr = ps.getContextResolver(Marshaller.class, mt);
        if (mcr != null) {
            Marshaller m = mcr.getContext(type);
            if (m != null) {
                return m;
            }
        }

        Marshaller m = getJAXBContext(type, mt).createMarshaller();
        if (formattedOutput.get()) {
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formattedOutput.get());
        }
        return m;

    }

    private Marshaller getMarshaller(Class type) throws JAXBException {
        final ContextResolver<Marshaller> resolver = mtMarshaller.get();
        if (resolver != null) {
            Marshaller u = resolver.getContext(type);
            if (u != null) {
                return u;
            }
        }

        Marshaller m = getJAXBContext(type).createMarshaller();
        if (formattedOutput.get()) {
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formattedOutput.get());
        }
        return m;
    }

    private JAXBContext getJAXBContext(Class type, MediaType mt) throws JAXBException {
        final ContextResolver<JAXBContext> cr = ps.getContextResolver(JAXBContext.class, mt);
        if (cr != null) {
            JAXBContext c = cr.getContext(type);
            if (c != null) {
                return c;
            }
        }

        return getStoredJaxbContext(type);
    }

    private JAXBContext getJAXBContext(Class type) throws JAXBException {
        final ContextResolver<JAXBContext> resolver = mtContext.get();
        if (resolver != null) {
            JAXBContext c = resolver.getContext(type);
            if (c != null) {
                return c;
            }
        }

        return getStoredJaxbContext(type);
    }

    protected JAXBContext getStoredJaxbContext(Class type) throws JAXBException {
        synchronized (jaxbContexts) {
            JAXBContext c = jaxbContexts.get(type);
            if (c == null) {
                c = JAXBContext.newInstance(type);
                jaxbContexts.put(type, c);
            }
            return c;
        }
    }

    protected static SAXSource getSAXSource(SAXParserFactory spf,
            InputStream entityStream) throws JAXBException {
        try {
            return new SAXSource(
                    spf.newSAXParser().getXMLReader(),
                    new InputSource(entityStream));
        } catch (Exception ex) {
            throw new JAXBException("Error creating SAXSource", ex);
        }
    }

    protected boolean isFormattedOutput() {
        return formattedOutput.get();
    }

    protected boolean isXmlRootElementProcessing() {
        return xmlRootElementProcessing.get();
    }

    protected void setHeader(Marshaller m, Annotation[] annotations) throws PropertyException {
        for (Annotation a : annotations) {
            if (a instanceof XmlHeader) {
                try {
                    // standalone jaxb ri
                    m.setProperty("com.sun.xml.bind.xmlHeaders", ((XmlHeader) a).value());
                } catch (PropertyException e) {
                    try {
                        // jaxb ri from jdk
                        m.setProperty("com.sun.xml.internal.bind.xmlHeaders", ((XmlHeader) a).value());
                    } catch (PropertyException ex) {
                        // other jaxb implementation
                        Logger.getLogger(AbstractJaxbProvider.class.getName()).log(
                                Level.WARNING, "@XmlHeader annotation is not supported with this JAXB implementation. Please use JAXB RI if you need this feature.");
                    }
                }
                break;
            }
        }
    }
}