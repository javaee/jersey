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

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;
import java.util.Map;
import java.util.WeakHashMap;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Context;
import javax.ws.rs.ext.ContextResolver;
import javax.ws.rs.ext.ParamConverter;
import javax.ws.rs.ext.ParamConverterProvider;
import javax.ws.rs.ext.Providers;

import javax.inject.Provider;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.glassfish.jersey.internal.inject.ExtractorException;
import org.glassfish.jersey.internal.util.collection.Value;
import org.glassfish.jersey.internal.util.collection.Values;

import org.xml.sax.InputSource;

/**
 * String reader provider producing {@link ParamConverterProvider param converter provider} that
 * support conversion of a string value into a JAXB instance.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JaxbStringReaderProvider {

    private static final Map<Class, JAXBContext> jaxbContexts = new WeakHashMap<Class, JAXBContext>();
    private final Value<ContextResolver<JAXBContext>> mtContext;
    private final Value<ContextResolver<Unmarshaller>> mtUnmarshaller;

    /**
     * Create JAXB string reader provider.
     *
     * @param ps used to obtain {@link JAXBContext} and {@link Unmarshaller} {@link ContextResolver ContextResolvers}
     */
    public JaxbStringReaderProvider(final Providers ps) {
        this.mtContext = Values.lazy(new Value<ContextResolver<JAXBContext>>() {

            @Override
            public ContextResolver<JAXBContext> get() {
                return ps.getContextResolver(JAXBContext.class, null);
            }
        });

        this.mtUnmarshaller = Values.lazy(new Value<ContextResolver<Unmarshaller>>() {
            @Override
            public ContextResolver<Unmarshaller> get() {
                return ps.getContextResolver(Unmarshaller.class, null);
            }
        });
    }

    /**
     * Get JAXB unmarshaller for the type.
     *
     * @param type Java type to be unmarshalled.
     * @return JAXB unmarshaller for the given type.
     * @throws JAXBException in case there's an error retrieving the unmarshaller.
     */
    protected final Unmarshaller getUnmarshaller(Class type) throws JAXBException {
        final ContextResolver<Unmarshaller> unmarshallerContextResolver = mtUnmarshaller.get();
        if (unmarshallerContextResolver != null) {
            Unmarshaller u = unmarshallerContextResolver.getContext(type);
            if (u != null) {
                return u;
            }
        }
        return getJAXBContext(type).createUnmarshaller();
    }

    private JAXBContext getJAXBContext(Class type) throws JAXBException {
        final ContextResolver<JAXBContext> jaxbContextContextResolver = mtContext.get();
        if (jaxbContextContextResolver != null) {
            JAXBContext c = jaxbContextContextResolver.getContext(type);
            if (c != null) {
                return c;
            }
        }
        return getStoredJAXBContext(type);
    }

    /**
     * Get the stored JAXB context supporting the Java type.
     *
     * @param type Java type supported by the stored JAXB context.
     * @return stored JAXB context supporting the Java type.
     * @throws JAXBException in case JAXB context retrieval fails.
     */
    protected JAXBContext getStoredJAXBContext(Class type) throws JAXBException {
        synchronized (jaxbContexts) {
            JAXBContext c = jaxbContexts.get(type);
            if (c == null) {
                c = JAXBContext.newInstance(type);
                jaxbContexts.put(type, c);
            }
            return c;
        }
    }

    /**
     * Root element JAXB {@link ParamConverter param converter}.
     */
    public static class RootElementProvider extends JaxbStringReaderProvider implements ParamConverterProvider {

        private final Provider<SAXParserFactory> spfProvider;

        /**
         * Creates new instance.
         *
         * @param spfProvider {@link SAXParserFactory SAX parser factory} injection provider.
         * @param ps used to obtain {@link JAXBContext} and {@link Unmarshaller} {@link ContextResolver ContextResolvers}
         */
        public RootElementProvider(@Context Provider<SAXParserFactory> spfProvider, @Context Providers ps) {
            super(ps);
            this.spfProvider = spfProvider;
        }


        @Override
        public <T> ParamConverter<T> getConverter(final Class<T> rawType, Type genericType, Annotation[] annotations) {
            final boolean supported = (rawType.getAnnotation(XmlRootElement.class) != null
                    || rawType.getAnnotation(XmlType.class) != null);
            if (!supported) {
                return null;
            }

            return new ParamConverter<T>() {

                @Override
                public T fromString(String value) {
                    try {
                        final SAXSource source = new SAXSource(
                                spfProvider.get().newSAXParser().getXMLReader(),
                                new InputSource(new java.io.StringReader(value)));

                        final Unmarshaller u = getUnmarshaller(rawType);
                        if (rawType.isAnnotationPresent(XmlRootElement.class)) {
                            return rawType.cast(u.unmarshal(source));
                        } else {
                            return u.unmarshal(source, rawType).getValue();
                        }
                    } catch (UnmarshalException ex) {
                        throw new ExtractorException(LocalizationMessages.ERROR_UNMARSHALLING_JAXB(rawType), ex);
                    } catch (JAXBException ex) {
                        throw new ProcessingException(LocalizationMessages.ERROR_UNMARSHALLING_JAXB(rawType), ex);
                    } catch (Exception ex) {
                        throw new ProcessingException(LocalizationMessages.ERROR_UNMARSHALLING_JAXB(rawType), ex);
                    }
                }

                @Override
                public String toString(T value) throws IllegalArgumentException {
                    // TODO: JERSEY-1385
                    return "test";
                }
            };
        }
    }
}
