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
import java.io.OutputStream;
import java.io.Writer;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.bind.ValidationEventHandler;
import javax.xml.bind.annotation.adapters.XmlAdapter;
import javax.xml.bind.attachment.AttachmentMarshaller;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamWriter;
import javax.xml.transform.Result;
import javax.xml.validation.Schema;

import org.glassfish.jersey.jettison.JettisonConfig;

import org.w3c.dom.Node;
import org.xml.sax.ContentHandler;

/**
 * JSON JAXB marshaller.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public final class JettisonJaxbMarshaller extends BaseJsonMarshaller implements Marshaller {

    public JettisonJaxbMarshaller(JAXBContext jaxbContext, JettisonConfig jsonConfig) throws JAXBException {
        super(jaxbContext, jsonConfig);
    }

    // Marshaller
    @Override
    public void marshal(Object jaxbObject, Result result) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, result);
    }

    @Override
    public void marshal(Object jaxbObject, OutputStream os) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, os);
    }

    @Override
    public void marshal(Object jaxbObject, File file) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, file);
    }

    @Override
    public void marshal(Object jaxbObject, Writer writer) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, writer);
    }

    @Override
    public void marshal(Object jaxbObject, ContentHandler handler) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, handler);
    }

    @Override
    public void marshal(Object jaxbObject, Node node) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, node);
    }

    @Override
    public void marshal(Object jaxbObject, XMLStreamWriter writer) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, writer);
    }

    @Override
    public void marshal(Object jaxbObject, XMLEventWriter writer) throws JAXBException {
        jaxbMarshaller.marshal(jaxbObject, writer);
    }

    @Override
    public Node getNode(Object jaxbObject) throws JAXBException {
        return jaxbMarshaller.getNode(jaxbObject);
    }

    @Override
    public void setProperty(String name, Object value) throws PropertyException {
        if (name == null) {
            throw new IllegalArgumentException("Name can't be null.");
        }

        if (name.equals(org.glassfish.jersey.jettison.JettisonMarshaller.FORMATTED)) {
            if (!(value instanceof Boolean)) {
                throw new PropertyException("property " + name + " must be an instance of type "
                        + "boolean, not " + value.getClass().getName());
            }

            jsonConfig = JettisonConfig.createJSONConfiguration(jsonConfig);
        } else {
            jaxbMarshaller.setProperty(name, value);
        }
    }

    @Override
    public Object getProperty(String key) throws PropertyException {
        return jaxbMarshaller.getProperty(key);
    }

    @Override
    public void setEventHandler(ValidationEventHandler handler) throws JAXBException {
        jaxbMarshaller.setEventHandler(handler);
    }

    @Override
    public ValidationEventHandler getEventHandler() throws JAXBException {
        return jaxbMarshaller.getEventHandler();
    }

    @Override
    public void setAdapter(XmlAdapter adapter) {
        jaxbMarshaller.setAdapter(adapter);
    }

    @Override
    public <A extends XmlAdapter> void setAdapter(Class<A> type, A adapter) {
        jaxbMarshaller.setAdapter(type, adapter);
    }

    @Override
    public <A extends XmlAdapter> A getAdapter(Class<A> type) {
        return jaxbMarshaller.getAdapter(type);
    }

    @Override
    public void setAttachmentMarshaller(AttachmentMarshaller marshaller) {
        jaxbMarshaller.setAttachmentMarshaller(marshaller);
    }

    @Override
    public AttachmentMarshaller getAttachmentMarshaller() {
        return jaxbMarshaller.getAttachmentMarshaller();
    }

    @Override
    public void setSchema(Schema schema) {
        jaxbMarshaller.setSchema(schema);
    }

    @Override
    public Schema getSchema() {
        return jaxbMarshaller.getSchema();
    }

    @Override
    public void setListener(Listener listener) {
        jaxbMarshaller.setListener(listener);
    }

    @Override
    public Listener getListener() {
        return jaxbMarshaller.getListener();
    }
}
