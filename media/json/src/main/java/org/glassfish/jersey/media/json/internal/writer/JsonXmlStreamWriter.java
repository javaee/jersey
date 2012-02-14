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
package org.glassfish.jersey.media.json.internal.writer;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.xml.namespace.NamespaceContext;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamWriter;

import org.glassfish.jersey.media.json.JsonConfiguration;

/**
 * JSOn StAX writer.
 *
 * @author Jakub Podlesak
 */
public class JsonXmlStreamWriter implements XMLStreamWriter {

    private static class WriterAdapter {

        Writer writer;
        boolean isEmpty = true;

        WriterAdapter() {
        }

        WriterAdapter(Writer w) {
            writer = w;
        }

        void write(String s) throws IOException {
            assert null != writer;
            writer.write(s);
            isEmpty = false;
        }

        String getContent() {
            return null;
        }
    }

    private static final class StringWriterAdapter extends WriterAdapter {

        StringWriterAdapter() {
            writer = new StringWriter();
        }

        @Override
        String getContent() {
            return writer.toString();
        }
    }

    private static final class DummyWriterAdapter extends WriterAdapter {

        DummyWriterAdapter() {
        }

        @Override
        void write(String s) throws IOException {
        }

        @Override
        String getContent() {
            return null;
        }
    }

    private static final class ProcessingState {

        String lastName;
        String currentName;
        WriterAdapter lastElementWriter;
        Boolean lastWasPrimitive;
        boolean lastIsArray;
        boolean isNotEmpty = false;
        WriterAdapter writer;

        ProcessingState() {
            writer = new StringWriterAdapter();
        }

        ProcessingState(WriterAdapter w) {
            writer = w;
        }

        @Override
        public String toString() {
            return String.format("{currentName:%s, writer: \"%s\", lastName:%s, lastWriter: %s}", currentName, ((writer != null) ? writer.getContent() : null), lastName, ((lastElementWriter != null) ? lastElementWriter.getContent() : null));
        }
    }
    Writer mainWriter;
    boolean stripRoot;
    char nsSeparator;
    final List<ProcessingState> processingStack = new ArrayList<ProcessingState>();
    int depth;
    final Collection<String> arrayElementNames = new LinkedList<String>();
    final Collection<String> nonStringElementNames = new LinkedList<String>();
    final Map<String, String> xml2JsonNs = new HashMap<String, String>();

    private JsonXmlStreamWriter(Writer writer) {
        this(writer, JsonConfiguration.DEFAULT);
    }

    private JsonXmlStreamWriter(Writer writer, JsonConfiguration config) {
        this.mainWriter = writer;
        this.stripRoot = config.isRootUnwrapping();
        this.nsSeparator = config.getNsSeparator();
        if (null != config.getArrays()) {
            this.arrayElementNames.addAll(config.getArrays());
        }
        if (null != config.getNonStrings()) {
            this.nonStringElementNames.addAll(config.getNonStrings());
        }
        if (null != config.getXml2JsonNs()) {
            this.xml2JsonNs.putAll(config.getXml2JsonNs());
        }
        processingStack.add(createProcessingState());
        depth = 0;
    }

    public static XMLStreamWriter createWriter(Writer writer, JsonConfiguration config) {
        final Collection<String> attrsAsElems = config.getAttributeAsElements();
        if ((attrsAsElems != null) && !attrsAsElems.isEmpty()) {
            return new A2EXmlStreamWriterProxy(
                    new JsonXmlStreamWriter(writer, config), attrsAsElems);
        } else {
            return new JsonXmlStreamWriter(writer, config);
        }
    }

    @Override
    public void close() throws XMLStreamException {
        try {
            this.mainWriter.close();
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public void flush() throws XMLStreamException {
        try {
            mainWriter.flush();
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public void writeEndDocument() throws XMLStreamException {
        try {
            if (null != processingStack.get(depth).lastElementWriter) {
                processingStack.get(depth).writer.write(processingStack.get(depth).lastElementWriter.getContent());
            }
            if ((null == processingStack.get(depth).lastWasPrimitive) || !processingStack.get(depth).lastWasPrimitive) {
                processingStack.get(depth).writer.write("}");
            }
            pollStack();
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public void writeEndElement() throws XMLStreamException {
        try {
            if (null != processingStack.get(depth).lastElementWriter) {
                if (processingStack.get(depth).lastIsArray) {
                    processingStack.get(depth).writer.write(",");
                    processingStack.get(depth).writer.write(processingStack.get(depth).lastElementWriter.getContent());
                    processingStack.get(depth).writer.write("]");
                } else {
                    if (isArrayElement(processingStack.get(depth).lastName)) { // one elem array
                        processingStack.get(depth).writer.write(processingStack.get(depth).lastIsArray ? "," : "[");
                        processingStack.get(depth).lastIsArray = true;
                        processingStack.get(depth).writer.write(processingStack.get(depth).lastElementWriter.getContent());
                        processingStack.get(depth).writer.write("]");
                    } else {
                        processingStack.get(depth).writer.write(processingStack.get(depth).lastElementWriter.getContent());
                    }
                }
            }
            if (processingStack.get(depth).writer.isEmpty) {
                processingStack.get(depth).writer.write("null");
            } else if ((null == processingStack.get(depth).lastWasPrimitive) || !processingStack.get(depth).lastWasPrimitive) {
                processingStack.get(depth).writer.write("}");
            }
            processingStack.get(depth - 1).lastName = processingStack.get(depth - 1).currentName;
            processingStack.get(depth - 1).lastWasPrimitive = false;
            processingStack.get(depth - 1).lastElementWriter = processingStack.get(depth).writer;
            pollStack();
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public void writeStartDocument() throws XMLStreamException {
    }

    @Override
    public void writeCharacters(char[] text, int start, int length) throws XMLStreamException {
        writeCharacters(new String(text, start, length));
    }

    @Override
    public void setDefaultNamespace(String uri) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeCData(String data) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeCharacters(String text) throws XMLStreamException {
        if (processingStack.get(depth).isNotEmpty) {
            writeStartElement(null, "$", null);
            _writeCharacters(text);
            writeEndElement();
        } else {
            _writeCharacters(text);
        }
    }

    private void _writeCharacters(String text) throws XMLStreamException {
        try {
            if (isNonString(processingStack.get(depth - 1).currentName)) {
                processingStack.get(depth).writer.write(JsonEncoder.encode(text));
            } else {
                processingStack.get(depth).writer.write("\"" + JsonEncoder.encode(text) + "\"");
            }
            processingStack.get(depth).lastWasPrimitive = true;
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public void writeComment(String data) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeDTD(String dtd) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeDefaultNamespace(String uri) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeEmptyElement(String localName) throws XMLStreamException {
        writeEmptyElement(null, localName, null);
    }

    @Override
    public void writeEntityRef(String name) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeProcessingInstruction(String target) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeStartDocument(String version) throws XMLStreamException {
    }

    @Override
    public void writeStartElement(String localName) throws XMLStreamException {
        writeStartElement(null, localName, null);
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return null;
    }

    @Override
    public void setNamespaceContext(NamespaceContext context) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrefix(String uri) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void setPrefix(String prefix, String uri) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeAttribute(String localName, String value) throws XMLStreamException {
        writeAttribute(null, null, localName, value);
    }

    @Override
    public void writeEmptyElement(String namespaceURI, String localName) throws XMLStreamException {
        writeEmptyElement(null, localName, null);
    }

    @Override
    public void writeNamespace(String prefix, String namespaceURI) throws XMLStreamException {
        // we do not want to deal with namespaces
        // the main goal of this writer is keep the produced json as simple as possible
    }

    @Override
    public void writeProcessingInstruction(String target, String data) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeStartDocument(String encoding, String version) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void writeStartElement(String namespaceURI, String localName) throws XMLStreamException {
        writeStartElement(null, localName, namespaceURI);
    }

    @Override
    public void writeAttribute(String namespaceURI, String localName, String value) throws XMLStreamException {
        writeAttribute(null, namespaceURI, localName, value);
    }

    @Override
    public void writeEmptyElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        writeStartElement(localName);
        writeEndElement();
    }

    private void pollStack() throws IOException {
        processingStack.remove(depth--);
    }

//    private void printStack(String localName) {
//        try {
//            for (int d = 0; d <= depth; d++) {
//                mainWriter.write("\n**" + d + ":" + processingStack.get(d));
//            }
//            mainWriter.write("\n*** [" + localName + "]");
//        } catch (IOException ex) {
//            Logger.getLogger(JsonXmlStreamWriter.class.getName()).log(Level.SEVERE, null, ex);
//        }
//    }
    private boolean isArrayElement(String name) {
        if (null == name) {
            return false;
        }
        return arrayElementNames.contains(name);
    }

    private boolean isNonString(String name) {
        if (null == name) {
            return false;
        }
        return nonStringElementNames.contains(name);
    }

    private String getEffectiveName(String namespaceURI, String localName) {
        if ((namespaceURI != null) && xml2JsonNs.containsKey(namespaceURI)) {
            return String.format("%s%c%s", xml2JsonNs.get(namespaceURI), nsSeparator, localName);
        } else {
            return localName;
        }
    }

    @Override
    public void writeStartElement(String prefix, String localName, String namespaceURI) throws XMLStreamException {
        String effectiveName = getEffectiveName(namespaceURI, localName);
        processingStack.get(depth).isNotEmpty = true;
        processingStack.get(depth).currentName = effectiveName;
        try {
            boolean isNextArrayElement = processingStack.get(depth).currentName.equals(processingStack.get(depth).lastName);
            if (!isNextArrayElement) {
                if (isArrayElement(processingStack.get(depth).lastName)) {
                    processingStack.get(depth).writer.write(processingStack.get(depth).lastIsArray ? "," : "[");
                    processingStack.get(depth).lastIsArray = true;
                    processingStack.get(depth).writer.write(processingStack.get(depth).lastElementWriter.getContent());
                } else {
                    // write previous elements from buffer
                    if (null != processingStack.get(depth).lastElementWriter) {
                        if (processingStack.get(depth).lastIsArray) {
                            processingStack.get(depth).writer.write(",");
                            processingStack.get(depth).writer.write(processingStack.get(depth).lastElementWriter.getContent());
                            processingStack.get(depth).writer.write("]");
                        } else {
                            processingStack.get(depth).writer.write(processingStack.get(depth).lastElementWriter.getContent());
                        }
                    }

                    processingStack.get(depth).lastIsArray = false;
                }
                if (null != processingStack.get(depth).lastName) {
                    if (processingStack.get(depth).lastIsArray) {
                        processingStack.get(depth).writer.write("]");
                        processingStack.get(depth).lastIsArray = false;
                    }
                    processingStack.get(depth).writer.write(",");  // next element at the same level
                }
                if (null == processingStack.get(depth).lastWasPrimitive) {
                    processingStack.get(depth).writer.write("{"); // first sub-element
                }
                processingStack.get(depth).writer.write("\"" + effectiveName + "\":");
            } else { // next array element
                processingStack.get(depth).writer.write(processingStack.get(depth).lastIsArray ? "," : "[");  // next element at the same level
                processingStack.get(depth).lastIsArray = true;
                processingStack.get(depth).writer.write(processingStack.get(depth).lastElementWriter.getContent());
            }
            depth++;
            processingStack.add(depth, createProcessingState());
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public void writeAttribute(String prefix, String namespaceURI, String localName, String value) throws XMLStreamException {
        writeStartElement(prefix, "@" + getEffectiveName(namespaceURI, localName), null);
        writeCharacters(value);
        writeEndElement();
    }

    private ProcessingState createProcessingState() {
        switch (depth) {
            case 0:
                return new ProcessingState(
                        stripRoot ? new DummyWriterAdapter() : new WriterAdapter(mainWriter));
            case 1:
                return stripRoot ? new ProcessingState(new WriterAdapter(mainWriter)) : new ProcessingState();
            default:
                return new ProcessingState();
        }
    }
}
