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

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.annotation.Annotation;
import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.Modifier;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.Stack;
import java.util.TreeSet;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.InternalServerErrorException;
import javax.ws.rs.WebApplicationException;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.NoContentException;
import javax.ws.rs.ext.Providers;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlType;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.glassfish.jersey.message.internal.EntityInputStream;

/**
 * An abstract provider for {@code T[]}, {@code Collection&lt;T&gt;},
 * and its subtypes as long as they have the public default constructor or
 * are interfaces implemented by one the following classes:
 * <ul>
 * <li>{@link ArrayList}</li>
 * <li>{@link LinkedList}</li>
 * <li>{@link HashSet}</li>
 * <li>{@link TreeSet}</li>
 * <li>{@link Stack}</li>
 * </ul>
 * {@code T} must be a JAXB type annotated with {@link XmlRootElement}.
 * <p>
 * Implementing classes may extend this class to provide specific marshalling
 * and unmarshalling behaviour.
 * </p>
 * <p>
 * When unmarshalling a {@link UnmarshalException} will result in a
 * {@link WebApplicationException} being thrown with a status of 400
 * (Client error), and a {@link JAXBException} will result in a
 * {@link WebApplicationException} being thrown with a status of 500
 * (Internal Server error).
 * </p>
 * <p>
 * When marshalling a {@link JAXBException} will result in a
 * {@link WebApplicationException} being thrown with a status of 500
 * (Internal Server error).
 * </p>
 *
 * @author Paul Sandoz
 * @author Martin Matula
 */
public abstract class AbstractCollectionJaxbProvider extends AbstractJaxbProvider<Object> {

    private static final Logger LOGGER = Logger.getLogger(AbstractCollectionJaxbProvider.class.getName());

    private static final Class<?>[] DEFAULT_IMPLS = new Class[] {
            ArrayList.class,
            LinkedList.class,
            HashSet.class,
            TreeSet.class,
            Stack.class
    };

    /**
     * This is to allow customized JAXB collections checking.
     *
     * @see AbstractCollectionJaxbProvider#verifyArrayType
     * @see AbstractCollectionJaxbProvider#verifyGenericType
     */
    public static interface JaxbTypeChecker {

        boolean isJaxbType(Class<?> type);
    }

    private static final JaxbTypeChecker DefaultJaxbTypeCHECKER = new JaxbTypeChecker() {

        @Override
        public boolean isJaxbType(Class<?> type) {
            return type.isAnnotationPresent(XmlRootElement.class)
                    || type.isAnnotationPresent(XmlType.class);
        }
    };

    public AbstractCollectionJaxbProvider(Providers ps) {
        super(ps);
    }

    public AbstractCollectionJaxbProvider(Providers ps, MediaType mt) {
        super(ps, mt);
    }

    @Override
    public boolean isReadable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (verifyCollectionSubclass(type)) {
            return verifyGenericType(genericType) && isSupported(mediaType);
        } else {
            return type.isArray() && verifyArrayType(type) && isSupported(mediaType);
        }
    }

    @Override
    public boolean isWriteable(Class<?> type, Type genericType, Annotation[] annotations, MediaType mediaType) {
        if (Collection.class.isAssignableFrom(type)) {
            return verifyGenericType(genericType) && isSupported(mediaType);
        } else {
            return type.isArray() && verifyArrayType(type) && isSupported(mediaType);
        }
    }

    public static boolean verifyCollectionSubclass(Class<?> type) {
        try {
            if (Collection.class.isAssignableFrom(type)) {
                for (Class c : DEFAULT_IMPLS) {
                    if (type.isAssignableFrom(c)) {
                        return true;
                    }
                }
                return !Modifier.isAbstract(type.getModifiers()) && Modifier.isPublic(type.getConstructor().getModifiers());
            }
        } catch (NoSuchMethodException ex) {
            LOGGER.log(Level.WARNING, LocalizationMessages.NO_PARAM_CONSTRUCTOR_MISSING(type.getName()), ex);
        } catch (SecurityException ex) {
            LOGGER.log(Level.WARNING, LocalizationMessages.UNABLE_TO_ACCESS_METHODS_OF_CLASS(type.getName()), ex);
        }
        return false;
    }

    private static boolean verifyArrayType(Class type) {
        return verifyArrayType(type, DefaultJaxbTypeCHECKER);
    }

    /**
     * The method could be used to check if given type is an array of JAXB beans.
     * It allows customizing the "is this a JAXB bean?" part.
     *
     * @param type    the array to be checked
     * @param checker allows JAXB bean check customization
     * @return true if given type is an array of JAXB beans
     */
    public static boolean verifyArrayType(Class type, JaxbTypeChecker checker) {
        type = type.getComponentType();

        return checker.isJaxbType(type)
                || JAXBElement.class.isAssignableFrom(type);
    }

    private static boolean verifyGenericType(Type genericType) {
        return verifyGenericType(genericType, DefaultJaxbTypeCHECKER);
    }

    /**
     * The method could be used to check if given type is a collection of JAXB beans.
     * It allows customizing the "is this a JAXB bean?" part.
     *
     * @param genericType the type to be checked
     * @param checker     allows JAXB bean check customization
     * @return true if given type is a collection of JAXB beans
     */
    public static boolean verifyGenericType(Type genericType, JaxbTypeChecker checker) {
        if (!(genericType instanceof ParameterizedType)) {
            return false;
        }

        final ParameterizedType pt = (ParameterizedType) genericType;

        if (pt.getActualTypeArguments().length > 1) {
            return false;
        }

        final Type ta = pt.getActualTypeArguments()[0];

        if (ta instanceof ParameterizedType) {
            ParameterizedType lpt = (ParameterizedType) ta;
            return (lpt.getRawType() instanceof Class)
                    && JAXBElement.class.isAssignableFrom((Class) lpt.getRawType());
        }

        if (!(pt.getActualTypeArguments()[0] instanceof Class)) {
            return false;
        }

        final Class listClass = (Class) pt.getActualTypeArguments()[0];

        return checker.isJaxbType(listClass);
    }

    @Override
    public final void writeTo(
            Object t,
            Class<?> type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, Object> httpHeaders,
            OutputStream entityStream) throws IOException {
        try {
            final Collection c = (type.isArray())
                    ? Arrays.asList((Object[]) t)
                    : (Collection) t;
            final Class elementType = getElementClass(type, genericType);
            final Charset charset = getCharset(mediaType);
            final String charsetName = charset.name();

            final Marshaller m = getMarshaller(elementType, mediaType);
            m.setProperty(Marshaller.JAXB_FRAGMENT, true);
            if (charset != UTF8) {
                m.setProperty(Marshaller.JAXB_ENCODING, charsetName);
            }
            setHeader(m, annotations);
            writeCollection(elementType, c, mediaType, charset, m, entityStream);
        } catch (JAXBException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

    /**
     * Write a collection of JAXB objects as child elements of the root element.
     *
     * @param elementType  the element type in the collection.
     * @param t            the collection to marshall
     * @param mediaType    the media type
     * @param c            the charset
     * @param m            the marshaller
     * @param entityStream the output stream to marshall the collection
     * @throws javax.xml.bind.JAXBException in case the marshalling of element collection fails.
     * @throws IOException                  in case of any other I/O error while marshalling the collection of JAXB objects.
     */
    public abstract void writeCollection(Class<?> elementType, Collection<?> t,
                                         MediaType mediaType, Charset c,
                                         Marshaller m, OutputStream entityStream)
            throws JAXBException, IOException;

    @Override
    @SuppressWarnings("unchecked")
    public final Object readFrom(
            Class<Object> type,
            Type genericType,
            Annotation[] annotations,
            MediaType mediaType,
            MultivaluedMap<String, String> httpHeaders,
            InputStream inputStream) throws IOException {

        final EntityInputStream entityStream = EntityInputStream.create(inputStream);
        if (entityStream.isEmpty()) {
            throw new NoContentException(LocalizationMessages.ERROR_READING_ENTITY_MISSING());
        }

        try {
            final Class<?> elementType = getElementClass(type, genericType);
            final Unmarshaller u = getUnmarshaller(elementType, mediaType);
            final XMLStreamReader r = getXMLStreamReader(elementType, mediaType, u, entityStream);
            boolean jaxbElement = false;

            Collection<Object> l = null;
            if (type.isArray()) {
                l = new ArrayList<Object>();
            } else {
                try {
                    l = (Collection<Object>) type.newInstance();
                } catch (Exception e) {
                    for (Class<?> c : DEFAULT_IMPLS) {
                        if (type.isAssignableFrom(c)) {
                            try {
                                l = (Collection<Object>) c.newInstance();
                                break;
                            } catch (InstantiationException ex) {
                                LOGGER.log(Level.WARNING, LocalizationMessages.UNABLE_TO_INSTANTIATE_CLASS(c.getName()), ex);
                            } catch (IllegalAccessException ex) {
                                LOGGER.log(Level.WARNING, LocalizationMessages.UNABLE_TO_INSTANTIATE_CLASS(c.getName()), ex);
                            } catch (SecurityException ex) {
                                LOGGER.log(Level.WARNING, LocalizationMessages.UNABLE_TO_INSTANTIATE_CLASS(c.getName()), ex);
                            }
                        }
                    }
                }
            }
            if (l == null) {
                l = new ArrayList<Object>();
            }

            // Move to root element
            int event = r.next();
            while (event != XMLStreamReader.START_ELEMENT) {
                event = r.next();
            }

            // Move to first child (if any)
            event = r.next();
            while (event != XMLStreamReader.START_ELEMENT
                    && event != XMLStreamReader.END_DOCUMENT) {
                event = r.next();
            }

            while (event != XMLStreamReader.END_DOCUMENT) {
                if (elementType.isAnnotationPresent(XmlRootElement.class)) {
                    l.add(u.unmarshal(r));
                } else if (elementType.isAnnotationPresent(XmlType.class)) {
                    l.add(u.unmarshal(r, elementType).getValue());
                } else {
                    l.add(u.unmarshal(r, elementType));
                    jaxbElement = true;
                }

                // Move to next peer (if any)
                event = r.getEventType();
                while (event != XMLStreamReader.START_ELEMENT
                        && event != XMLStreamReader.END_DOCUMENT) {
                    event = r.next();
                }
            }

            return (type.isArray())
                    ? createArray(l, jaxbElement ? JAXBElement.class : elementType)
                    : l;
        } catch (UnmarshalException ex) {
            throw new BadRequestException(ex);
        } catch (XMLStreamException ex) {
            throw new BadRequestException(ex);
        } catch (JAXBException ex) {
            throw new InternalServerErrorException(ex);
        }
    }

    private static Object createArray(Collection<?> collection, Class componentType) {
        Object array = Array.newInstance(componentType, collection.size());
        int i = 0;
        for (Object value : collection) {
            Array.set(array, i++, value);
        }
        return array;
    }

    /**
     * Get the {@link XMLStreamReader} for unmarshalling.
     *
     * @param elementType  the individual element type.
     * @param mediaType    the media type.
     * @param unmarshaller the unmarshaller as a carrier of possible config options.
     * @param entityStream the input stream.
     * @return the XMLStreamReader.
     * @throws javax.xml.stream.XMLStreamException in case {@code XMLStreamReader} retrieval fails.
     */
    protected abstract XMLStreamReader getXMLStreamReader(Class<?> elementType, MediaType mediaType, Unmarshaller unmarshaller,
                                                          InputStream entityStream)
            throws XMLStreamException;

    protected static Class getElementClass(Class<?> type, Type genericType) {
        Type ta;
        if (genericType instanceof ParameterizedType) {
            // Collection case
            ta = ((ParameterizedType) genericType).getActualTypeArguments()[0];
        } else if (genericType instanceof GenericArrayType) {
            // GenericArray case
            ta = ((GenericArrayType) genericType).getGenericComponentType();
        } else {
            // Array case
            ta = type.getComponentType();
        }
        if (ta instanceof ParameterizedType) {
            // JAXBElement case
            ta = ((ParameterizedType) ta).getActualTypeArguments()[0];
        }
        return (Class) ta;
    }

    private final NounInflector inflector = NounInflector.getInstance();

    private static String convertToXmlName(final String name) {
        return name.replace("$", "_");
    }

    /**
     * Construct the name of the root element from it's Java type name.
     *
     * @param elementType element Java type.
     * @return constructed root element name for a given element Java type.
     */
    protected final String getRootElementName(Class<?> elementType) {
        if (isXmlRootElementProcessing()) {
            return convertToXmlName(inflector.pluralize(inflector.demodulize(getElementName(elementType))));
        } else {
            return convertToXmlName(inflector.decapitalize(inflector.pluralize(inflector.demodulize(elementType.getName()))));
        }
    }

    /**
     * Get the element name for a given Java type.
     * <p>
     * In case the element is annotated with a {@link javax.xml.bind.annotation.XmlRootElement} annotation
     * and the {@link javax.xml.bind.annotation.XmlRootElement#name() specified element name} is not default,
     * the method returns the specified element name in the annotation. Otherwise, the method returns the name of
     * the element class instead.
     * </p>
     *
     * @param elementType element Java type.
     * @return element name for a given element Java type.
     */
    protected static String getElementName(Class<?> elementType) {
        String name = elementType.getName();
        XmlRootElement xre = elementType.getAnnotation(XmlRootElement.class);
        if (xre != null && !"##default".equals(xre.name())) {
            name = xre.name();
        }
        return name;
    }
}
