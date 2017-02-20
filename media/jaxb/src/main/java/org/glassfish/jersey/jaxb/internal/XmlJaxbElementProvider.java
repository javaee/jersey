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

import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.Charset;

import javax.ws.rs.Consumes;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.ext.Providers;

import javax.inject.Provider;
import javax.inject.Singleton;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.SAXParserFactory;

/**
 * Base XML-based message body provider for {@link JAXBElement JAXB element} instances.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public abstract class XmlJaxbElementProvider extends AbstractJaxbElementProvider {

    private final Provider<SAXParserFactory> spf;

    public XmlJaxbElementProvider(Provider<SAXParserFactory> spf, Providers ps) {
        super(ps);

        this.spf = spf;
    }

    public XmlJaxbElementProvider(Provider<SAXParserFactory> spf, Providers ps, MediaType mt) {
        super(ps, mt);

        this.spf = spf;
    }

    /**
     * Provider for marshalling/un-marshalling {@link JAXBElement JAXB elements}
     * from/to entities of {@code application/xml} media type.
     */
    @Produces("application/xml")
    @Consumes("application/xml")
    @Singleton
    public static final class App extends XmlJaxbElementProvider {

        public App(@Context Provider<SAXParserFactory> spf, @Context Providers ps) {
            super(spf, ps, MediaType.APPLICATION_XML_TYPE);
        }
    }

    /**
     * Provider for marshalling/un-marshalling {@link JAXBElement JAXB elements}
     * from/to entities of {@code text/xml} media type.
     */
    @Produces("text/xml")
    @Consumes("text/xml")
    @Singleton
    public static final class Text extends XmlJaxbElementProvider {

        public Text(@Context Provider<SAXParserFactory> spf, @Context Providers ps) {
            super(spf, ps, MediaType.TEXT_XML_TYPE);
        }
    }

    /**
     * Provider for marshalling/un-marshalling {@link JAXBElement JAXB elements}
     * from/to entities of {@code <type>/<sub-type>+xml} media types.
     */
    @Produces("*/*,*/*+xml")
    @Consumes("*/*,*/*+xml")
    @Singleton
    public static final class General extends XmlJaxbElementProvider {

        public General(@Context Provider<SAXParserFactory> spf, @Context Providers ps) {
            super(spf, ps);
        }

        @Override
        protected boolean isSupported(MediaType m) {
            return m.getSubtype().endsWith("+xml");
        }
    }

    @Override
    protected final JAXBElement<?> readFrom(Class<?> type, MediaType mediaType,
                                            Unmarshaller unmarshaller, InputStream entityStream) throws JAXBException {
        return unmarshaller.unmarshal(getSAXSource(spf.get(), entityStream), type);
    }

    @Override
    protected final void writeTo(JAXBElement<?> t, MediaType mediaType, Charset c,
                                 Marshaller m, OutputStream entityStream) throws JAXBException {
        m.marshal(t, entityStream);
    }
}
