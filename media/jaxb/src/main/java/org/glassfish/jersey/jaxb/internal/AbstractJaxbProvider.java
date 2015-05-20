/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jaxb.internal;

import java.io.InputStream;
import java.lang.annotation.Annotation;
import java.lang.ref.WeakReference;
import java.util.Map;
import java.util.WeakHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Configuration;
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

import org.glassfish.jersey.internal.util.PropertiesHelper;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;
import org.glassfish.jersey.message.MessageProperties;
import org.glassfish.jersey.message.XmlHeader;
import org.glassfish.jersey.message.internal.AbstractMessageReaderWriterProvider;

import org.xml.sax.InputSource;

/**
 * A base class for implementing JAXB-based readers and writers.
 *
 * @param <T> Java type supported by the provider.
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class AbstractJaxbProvider<T> extends AbstractMessageReaderWriterProvider<T> {

    private static final Map<Class<?>, WeakReference<JAXBContext>> jaxbContexts =
            new WeakHashMap<Class<?>, WeakReference<JAXBContext>>();
    private final Providers jaxrsProviders;
    private final boolean fixedResolverMediaType;
    private final Value<ContextResolver<JAXBContext>> mtContext;
    private final Value<ContextResolver<Unmarshaller>> mtUnmarshaller;
    private final Value<ContextResolver<Marshaller>> mtMarshaller;
    private Value<Boolean> formattedOutput = Values.of(Boolean.FALSE);
    private Value<Boolean> xmlRootElementProcessing = Values.of(Boolean.FALSE);

    /**
     * Inheritance constructor.
     *
     * @param providers JAX-RS providers.
     */
    public AbstractJaxbProvider(final Providers providers) {
        this(providers, null);
    }

    /**
     * Inheritance constructor.
     *
     * @param providers         JAX-RS providers.
     * @param resolverMediaType JAXB component context resolver media type to be used.
     */
    public AbstractJaxbProvider(final Providers providers, final MediaType resolverMediaType) {
        this.jaxrsProviders = providers;

        fixedResolverMediaType = resolverMediaType != null;
        if (fixedResolverMediaType) {
            this.mtContext = Values.lazy(new Value<ContextResolver<JAXBContext>>() {

                @Override
                public ContextResolver<JAXBContext> get() {
                    return providers.getContextResolver(JAXBContext.class, resolverMediaType);
                }
            });
            this.mtUnmarshaller = Values.lazy(new Value<ContextResolver<Unmarshaller>>() {

                @Override
                public ContextResolver<Unmarshaller> get() {
                    return providers.getContextResolver(Unmarshaller.class, resolverMediaType);
                }
            });
            this.mtMarshaller = Values.lazy(new Value<ContextResolver<Marshaller>>() {

                @Override
                public ContextResolver<Marshaller> get() {
                    return providers.getContextResolver(Marshaller.class, resolverMediaType);
                }
            });
        } else {
            this.mtContext = null;
            this.mtUnmarshaller = null;
            this.mtMarshaller = null;
        }
    }

    // TODO This provider should be registered and configured via a feature.
    @Context
    public void setConfiguration(final Configuration config) {
        formattedOutput = Values.lazy(new Value<Boolean>() {

            @Override
            public Boolean get() {
                return PropertiesHelper.isProperty(config.getProperty(MessageProperties.XML_FORMAT_OUTPUT));
            }
        });

        xmlRootElementProcessing = Values.lazy(new Value<Boolean>() {

            @Override
            public Boolean get() {
                return PropertiesHelper.isProperty(config.getProperty(MessageProperties.JAXB_PROCESS_XML_ROOT_ELEMENT));
            }
        });
    }

    /**
     * Check if the given media type is supported by this JAXB entity provider.
     * <p>
     * Subclasses can override this method. Default implementation always returns {@code true}.
     * </p>
     *
     * @param mediaType media type to be checked for support.
     * @return {@code true} if the media type is supported by the entity provider, {@code false} otherwise.
     */
    protected boolean isSupported(MediaType mediaType) {
        return true;
    }

    /**
     * Get the JAXB unmarshaller for the given class and media type.
     * <p>
     * In case this provider instance has been {@link #AbstractJaxbProvider(Providers, MediaType)
     * created with a fixed resolver media type}, the supplied media type argument will be ignored.
     * </p>
     *
     * @param type      Java type to be unmarshalled.
     * @param mediaType entity media type.
     * @return JAXB unmarshaller for the requested Java type, media type combination.
     * @throws JAXBException in case retrieving the unmarshaller fails with a JAXB exception.
     */
    protected final Unmarshaller getUnmarshaller(Class type, MediaType mediaType) throws JAXBException {
        if (fixedResolverMediaType) {
            return getUnmarshaller(type);
        }

        final ContextResolver<Unmarshaller> unmarshallerResolver =
                jaxrsProviders.getContextResolver(Unmarshaller.class, mediaType);
        if (unmarshallerResolver != null) {
            Unmarshaller u = unmarshallerResolver.getContext(type);
            if (u != null) {
                return u;
            }
        }

        final JAXBContext ctx = getJAXBContext(type, mediaType);
        return (ctx == null) ? null : ctx.createUnmarshaller();
    }

    private Unmarshaller getUnmarshaller(Class type) throws JAXBException {
        final ContextResolver<Unmarshaller> resolver = mtUnmarshaller.get();
        if (resolver != null) {
            Unmarshaller u = resolver.getContext(type);
            if (u != null) {
                return u;
            }
        }

        final JAXBContext ctx = getJAXBContext(type);
        return (ctx == null) ? null : ctx.createUnmarshaller();
    }

    /**
     * Get the JAXB marshaller for the given class and media type.
     * <p>
     * In case this provider instance has been {@link #AbstractJaxbProvider(Providers, MediaType)
     * created with a fixed resolver media type}, the supplied media type argument will be ignored.
     * </p>
     *
     * @param type      Java type to be marshalled.
     * @param mediaType entity media type.
     * @return JAXB marshaller for the requested Java type, media type combination.
     * @throws JAXBException in case retrieving the marshaller fails with a JAXB exception.
     */
    protected final Marshaller getMarshaller(Class type, MediaType mediaType) throws JAXBException {
        if (fixedResolverMediaType) {
            return getMarshaller(type);
        }

        final ContextResolver<Marshaller> mcr = jaxrsProviders.getContextResolver(Marshaller.class, mediaType);
        if (mcr != null) {
            Marshaller m = mcr.getContext(type);
            if (m != null) {
                return m;
            }
        }

        final JAXBContext ctx = getJAXBContext(type, mediaType);
        if (ctx == null) {
            return null;
        }

        Marshaller m = ctx.createMarshaller();
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

        final JAXBContext ctx = getJAXBContext(type);
        if (ctx == null) {
            return null;
        }

        Marshaller m = ctx.createMarshaller();
        if (formattedOutput.get()) {
            m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, formattedOutput.get());
        }
        return m;
    }

    private JAXBContext getJAXBContext(Class type, MediaType mt) throws JAXBException {
        final ContextResolver<JAXBContext> cr = jaxrsProviders.getContextResolver(JAXBContext.class, mt);
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

    /**
     * Retrieve cached JAXB context capable of handling the given Java type.
     *
     * @param type Java type .
     * @return JAXB context associated with the Java type.
     * @throws JAXBException in case the JAXB context retrieval fails.
     */
    protected JAXBContext getStoredJaxbContext(Class type) throws JAXBException {
        synchronized (jaxbContexts) {
            final WeakReference<JAXBContext> ref = jaxbContexts.get(type);
            JAXBContext c = (ref != null) ? ref.get() : null;
            if (c == null) {
                c = JAXBContext.newInstance(type);
                jaxbContexts.put(type, new WeakReference<JAXBContext>(c));
            }
            return c;
        }
    }

    /**
     * Create new {@link javax.xml.transform.sax.SAXSource} for a given entity input stream.
     *
     * @param spf          SAX parser factory to be used to create the SAX source.
     * @param entityStream entity input stream.
     * @return new {@link javax.xml.transform.sax.SAXSource} representing the entity input stream.
     * @throws JAXBException in case SAX source creation fails.
     */
    protected static SAXSource getSAXSource(SAXParserFactory spf, InputStream entityStream) throws JAXBException {
        try {
            return new SAXSource(spf.newSAXParser().getXMLReader(), new InputSource(entityStream));
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

    /**
     * Set the custom XML header on a JAXB marshaller if specified via {@link org.glassfish.jersey.message.XmlHeader} annotation,
     * present in the supplied array of annotations.
     *
     * @param marshaller  JAXB marshaller.
     * @param annotations array of annotations that MAY contain a {@code XmlHeader} annotation instance.
     */
    protected void setHeader(Marshaller marshaller, Annotation[] annotations) {
        for (Annotation a : annotations) {
            if (a instanceof XmlHeader) {
                try {
                    // standalone jaxb ri
                    marshaller.setProperty("com.sun.xml.bind.xmlHeaders", ((XmlHeader) a).value());
                } catch (PropertyException e) {
                    try {
                        // jaxb ri from jdk
                        marshaller.setProperty("com.sun.xml.internal.bind.xmlHeaders", ((XmlHeader) a).value());
                    } catch (PropertyException ex) {
                        // other jaxb implementation
                        Logger.getLogger(AbstractJaxbProvider.class.getName()).log(
                                Level.WARNING, "@XmlHeader annotation is not supported with this JAXB implementation."
                                        + " Please use JAXB RI if you need this feature.");
                    }
                }
                break;
            }
        }
    }
}
