/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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
import java.nio.charset.Charset;
import java.util.Collection;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.stream.XMLInputFactory;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

/**
 * Base XML-based message body provider for collections of JAXB beans.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class XmlCollectionJaxbProvider extends AbstractCollectionJaxbProvider {

    private final Provider<XMLInputFactory> xif;

    XmlCollectionJaxbProvider(Provider<XMLInputFactory> xif, Providers ps) {
        super(ps);

        this.xif = xif;
    }

    XmlCollectionJaxbProvider(Provider<XMLInputFactory> xif, Providers ps, MediaType mt) {
        super(ps, mt);

        this.xif = xif;
    }

    /**
     * JAXB  provider for marshalling/un-marshalling collections
     * from/to entities of {@code application/xml} media type.
     */
    @Produces("application/xml")
    @Consumes("application/xml")
    @Singleton
    public static final class App extends XmlCollectionJaxbProvider {

        public App(@Context Provider<XMLInputFactory> xif, @Context Providers ps) {
            super(xif, ps, MediaType.APPLICATION_XML_TYPE);
        }
    }

    /**
     * JAXB  provider for marshalling/un-marshalling collections
     * from/to entities of {@code text/xml} media type.
     */
    @Produces("text/xml")
    @Consumes("text/xml")
    @Singleton
    public static final class Text extends XmlCollectionJaxbProvider {

        public Text(@Context Provider<XMLInputFactory> xif, @Context Providers ps) {
            super(xif, ps, MediaType.TEXT_XML_TYPE);
        }
    }

    /**
     * JAXB provider for marshalling/un-marshalling collections
     * from/to entities of {@code <type>/<sub-type>+xml} media types.
     */
    @Produces("*/*")
    @Consumes("*/*")
    @Singleton
    public static final class General extends XmlCollectionJaxbProvider {

        public General(@Context Provider<XMLInputFactory> xif, @Context Providers ps) {
            super(xif, ps);
        }

        @Override
        protected boolean isSupported(MediaType m) {
            return m.getSubtype().endsWith("+xml");
        }
    }

    @Override
    protected final XMLStreamReader getXMLStreamReader(Class<?> elementType,
                                                       MediaType mediaType,
                                                       Unmarshaller u,
                                                       InputStream entityStream)
            throws XMLStreamException {
        return xif.get().createXMLStreamReader(entityStream);
    }

    @Override
    public final void writeCollection(Class<?> elementType, Collection<?> t,
                                      MediaType mediaType, Charset c,
                                      Marshaller m, OutputStream entityStream)
            throws JAXBException, IOException {
        final String rootElement = getRootElementName(elementType);
        final String cName = c.name();

        entityStream.write(
                String.format("<?xml version=\"1.0\" encoding=\"%s\" standalone=\"yes\"?>", cName).getBytes(cName));
        String property = "com.sun.xml.bind.xmlHeaders";
        String header;
        try {
            // standalone jaxb ri?
            header = (String) m.getProperty(property);
        } catch (PropertyException e) {
            // jaxb ri from jdk?
            property = "com.sun.xml.internal.bind.xmlHeaders";
            try {
                header = (String) m.getProperty(property);
            } catch (PropertyException ex) {
                // other jaxb implementation
                header = null;
                Logger.getLogger(XmlCollectionJaxbProvider.class.getName())
                        .log(Level.WARNING,
                                "@XmlHeader annotation is not supported with this JAXB implementation. Please use JAXB RI if "
                                        + "you need this feature.");
            }
        }
        if (header != null) {
            m.setProperty(property, "");
            entityStream.write(header.getBytes(cName));
        }
        entityStream.write(String.format("<%s>", rootElement).getBytes(cName));
        for (Object o : t) {
            m.marshal(o, entityStream);
        }

        entityStream.write(String.format("</%s>", rootElement).getBytes(cName));
    }
}
