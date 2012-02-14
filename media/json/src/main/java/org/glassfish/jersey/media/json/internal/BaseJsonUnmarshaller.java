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
package org.glassfish.jersey.media.json.internal;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.charset.Charset;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.UnmarshalException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.glassfish.jersey.media.json.JsonConfigured;
import org.glassfish.jersey.media.json.JsonConfiguration;
import org.glassfish.jersey.media.json.JsonUnmarshaller;

/**
 * Base JSON marshaller implementation class.
 *
 * @author Jakub Podlesak
 */
public class BaseJsonUnmarshaller implements JsonUnmarshaller, JsonConfigured {

    private static final Charset UTF8 = Charset.forName("UTF-8");
    protected final Unmarshaller jaxbUnmarshaller;
    protected final JsonConfiguration jsonConfig;

    public BaseJsonUnmarshaller(JAXBContext jaxbContext, JsonConfiguration jsonConfig) throws JAXBException {
        this(jaxbContext.createUnmarshaller(), jsonConfig);
    }

    public BaseJsonUnmarshaller(Unmarshaller jaxbUnmarshaller, JsonConfiguration jsonConfig) {
        this.jaxbUnmarshaller = jaxbUnmarshaller;
        this.jsonConfig = jsonConfig;
    }

    // JsonConfigured
    public JsonConfiguration getJSONConfiguration() {
        return jsonConfig;
    }

    // JsonUnmarshaller
    public <T> T unmarshalFromJSON(InputStream inputStream, Class<T> expectedType) throws JAXBException {
        return unmarshalFromJSON(new InputStreamReader(inputStream, UTF8), expectedType);
    }

    public <T> T unmarshalFromJSON(Reader reader, Class<T> expectedType) throws JAXBException {
        if (jsonConfig.isRootUnwrapping() || !expectedType.isAnnotationPresent(XmlRootElement.class)) {
            return unmarshalJAXBElementFromJSON(reader, expectedType).getValue();
        } else {
            return (T) jaxbUnmarshaller.unmarshal(createXmlStreamReader(reader, expectedType));
        }
    }

    public <T> JAXBElement<T> unmarshalJAXBElementFromJSON(InputStream inputStream, Class<T> declaredType) throws JAXBException {
        return unmarshalJAXBElementFromJSON(new InputStreamReader(inputStream, UTF8), declaredType);
    }

    public <T> JAXBElement<T> unmarshalJAXBElementFromJSON(Reader reader, Class<T> declaredType) throws JAXBException {
        return jaxbUnmarshaller.unmarshal(createXmlStreamReader(reader, declaredType), declaredType);
    }

    private XMLStreamReader createXmlStreamReader(Reader reader, Class expectedType) throws JAXBException {
        try {
            return Stax2JsonFactory.createReader(reader, jsonConfig,
                    jsonConfig.isRootUnwrapping() ? JsonHelper.getRootElementName(expectedType) : null);
        } catch (XMLStreamException ex) {
            throw new UnmarshalException("Error creating JSON-based XMLStreamReader", ex);
        }
    }
}
