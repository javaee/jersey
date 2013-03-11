/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jettison.internal;

import java.io.File;
import java.io.InputStream;
import java.io.Reader;
import java.net.URL;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;
import javax.xml.bind.Unmarshaller;
import javax.xml.bind.UnmarshallerHandler;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentUnmarshaller;
import javax.xml.stream.XMLEventReader;
import javax.xml.stream.XMLStreamReader;
import javax.xml.transform.Source;
import javax.xml.validation.Schema;

import org.glassfish.jersey.jettison.JettisonConfig;

import org.w3c.dom.Node;
import org.xml.sax.InputSource;

/**
 * JSON JAXB unmarshaller.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class JettisonJaxbUnmarshaller extends BaseJsonUnmarshaller implements Unmarshaller {

    public JettisonJaxbUnmarshaller(JAXBContext jaxbContext, JettisonConfig jsonConfig) throws JAXBException {
        super(jaxbContext, jsonConfig);
    }

    // Unmarshaller
    @Override
    public Object unmarshal(File file) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(file);
    }

    @Override
    public Object unmarshal(InputStream inputStream) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(inputStream);
    }

    @Override
    public Object unmarshal(Reader reader) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(reader);
    }

    @Override
    public Object unmarshal(URL url) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(url);
    }

    @Override
    public Object unmarshal(InputSource inputSource) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(inputSource);
    }

    @Override
    public Object unmarshal(Node node) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(node);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(Node node, Class<T> type) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(node, type);
    }

    @Override
    public Object unmarshal(Source source) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(source);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(Source source, Class<T> type) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(source, type);
    }

    @Override
    public Object unmarshal(XMLStreamReader xmlStreamReader) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(xmlStreamReader);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(XMLStreamReader xmlStreamReader, Class<T> type) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(xmlStreamReader, type);
    }

    @Override
    public Object unmarshal(XMLEventReader xmlEventReader) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(xmlEventReader);
    }

    @Override
    public <T> JAXBElement<T> unmarshal(XMLEventReader xmlEventReader, Class<T> type) throws JAXBException {
        return this.jaxbUnmarshaller.unmarshal(xmlEventReader, type);
    }

    @Override
    public UnmarshallerHandler getUnmarshallerHandler() {
        return this.jaxbUnmarshaller.getUnmarshallerHandler();
    }

    @Override
    public void setValidating(boolean validating) throws JAXBException {
        this.jaxbUnmarshaller.setValidating(validating);
    }

    @Override
    public boolean isValidating() throws JAXBException {
        return this.jaxbUnmarshaller.isValidating();
    }

    @Override
    public void setEventHandler(ValidationEventHandler validationEventHandler) throws JAXBException {
        this.jaxbUnmarshaller.setEventHandler(validationEventHandler);
    }

    @Override
    public ValidationEventHandler getEventHandler() throws JAXBException {
        return this.jaxbUnmarshaller.getEventHandler();
    }

    @Override
    public void setProperty(String key, Object value) throws PropertyException {
        this.jaxbUnmarshaller.setProperty(key, value);
    }

    @Override
    public Object getProperty(String key) throws PropertyException {
        return this.jaxbUnmarshaller.getProperty(key);
    }

    @Override
    public void setSchema(Schema schema) {
        this.jaxbUnmarshaller.setSchema(schema);
    }

    @Override
    public Schema getSchema() {
        return this.jaxbUnmarshaller.getSchema();
    }

    @Override
    public void setAdapter(XmlAdapter xmlAdapter) {
        this.jaxbUnmarshaller.setAdapter(xmlAdapter);
    }

    @Override
    public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        this.jaxbUnmarshaller.setAdapter(type, adapter);
    }

    @Override
    public <A extends XmlAdapter> A getAdapter(Class<A> type) {
        return this.jaxbUnmarshaller.getAdapter(type);
    }

    @Override
    public void setAttachmentUnmarshaller(AttachmentUnmarshaller attachmentUnmarshaller) {
        this.jaxbUnmarshaller.setAttachmentUnmarshaller(attachmentUnmarshaller);
    }

    @Override
    public AttachmentUnmarshaller getAttachmentUnmarshaller() {
        return this.jaxbUnmarshaller.getAttachmentUnmarshaller();
    }

    @Override
    public void setListener(Listener listener) {
        this.jaxbUnmarshaller.setListener(listener);
    }

    @Override
    public Listener getListener() {
        return this.jaxbUnmarshaller.getListener();
    }
}
