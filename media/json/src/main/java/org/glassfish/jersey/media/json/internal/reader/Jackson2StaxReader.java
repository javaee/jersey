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
package org.glassfish.jersey.media.json.internal.reader;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Queue;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.glassfish.jersey.media.json.JsonConfiguration;

import org.codehaus.jackson.JsonParser;

import com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext;
import org.glassfish.jersey.media.json.internal.LocalizationMessages;

/**
 * Jackson StAX reader.
 *
 * @author Jakub Podlesak
 */
public class Jackson2StaxReader implements XMLStreamReader {

    private static class ProcessingInfo {

        QName name;
        boolean isArray;
        boolean isFirstElement;

        ProcessingInfo(QName name, boolean isArray, boolean isFirstElement) {
            this.name = name;
            this.isArray = isArray;
            this.isFirstElement = isFirstElement;
        }
    }
    JsonParser parser;
    final Queue<JsonReaderXmlEvent> eventQueue = new LinkedList<JsonReaderXmlEvent>();
    final List<ProcessingInfo> processingStack = new ArrayList<ProcessingInfo>();
    final JsonNamespaceContext namespaceContext = new JsonNamespaceContext();
    private boolean properJAXBVersion = true;
    private final boolean attrsWithPrefix;
    final Collection<String> elemsExpected = new HashSet<String>();
    final Map<String, QName> qNamesOfExpElems = new HashMap<String, QName>();
    final Collection<String> attrsExpected = new HashSet<String>();
    final Map<String, QName> qNamesOfExpAttrs = new HashMap<String, QName>();

    static <T> T pop(List<T> stack) {
        return stack.remove(stack.size() - 1);
    }

    static <T> T peek(List<T> stack) {
        return (stack.size() > 0) ? stack.get(stack.size() - 1) : null;
    }

    static <T> T peek2nd(List<T> stack) {
        return (stack.size() > 1) ? stack.get(stack.size() - 2) : null;
    }

    public Jackson2StaxReader(JsonParser parser) throws XMLStreamException {
        this(parser, JsonConfiguration.DEFAULT);
    }

    public Jackson2StaxReader(JsonParser parser, JsonConfiguration config) throws XMLStreamException {
        this.attrsWithPrefix = config.isUsingPrefixesAtNaturalAttributes();
        this.parser = parser;
        try {
            readNext();
        } catch (IOException ex) {
            Logger.getLogger(Jackson2StaxReader.class.getName()).log(Level.SEVERE, null, ex);
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public Object getProperty(String name) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private void readNext() throws IOException {
        readNext(false);
    }

    static final Collection<org.codehaus.jackson.JsonToken> valueTokens = new HashSet<org.codehaus.jackson.JsonToken>() {
        private static final long serialVersionUID = 3109256773218160485L;

        {
            add(org.codehaus.jackson.JsonToken.VALUE_FALSE);
            add(org.codehaus.jackson.JsonToken.VALUE_TRUE);
            add(org.codehaus.jackson.JsonToken.VALUE_NULL);
            add(org.codehaus.jackson.JsonToken.VALUE_STRING);
            add(org.codehaus.jackson.JsonToken.VALUE_NUMBER_FLOAT);
            add(org.codehaus.jackson.JsonToken.VALUE_NUMBER_INT);
        }
    };

    private QName getQNameForTagLocName(final String localName) {
        return getQNameForLocName(localName, qNamesOfExpElems);
    }

    private QName getQNameForLocName(final String localName, final Map<String, QName> qNamesMap) {
        final QName result = qNamesMap.get(localName);
        if (result != null) {
            return result;
        } else {
            return new QName(localName);
        }
    }

    private void readNext(boolean lookingForAttributes) throws IOException {
        if (!lookingForAttributes) {
            eventQueue.poll();
        }
        while (eventQueue.isEmpty() || lookingForAttributes) {
            org.codehaus.jackson.JsonToken jtok;
            // TODO: ask Jackson to take care of DOS
            while (true) {
                parser.nextToken();
                jtok = parser.getCurrentToken();
                final ProcessingInfo pi = peek(processingStack);
                switch (jtok) {
                    case FIELD_NAME:
                        // start tag
                        String currentName = parser.getCurrentName();
                        if (attrsWithPrefix && currentName.startsWith("@")) {
                            currentName = currentName.substring(1);
                        }
                        boolean currentIsAttribute = !("$".equals(currentName)) && properJAXBVersion ? attrsExpected.contains(currentName) : !elemsExpected.contains(currentName);
                        if (lookingForAttributes && currentIsAttribute) {
                            parser.nextToken();
                            if (valueTokens.contains(parser.getCurrentToken())) {
                                eventQueue.peek().addAttribute(getQNameForLocName(currentName, qNamesOfExpAttrs), parser.getText());
                            } else {
                                throw new IOException("Not an attribute, expected primitive value! CurrentNme=" + currentName);
                            }
                        } else { // non attribute
                            lookingForAttributes = false; // stop seeking attributes
                            if (!("$".equals(currentName))) {
                                final QName currentQName = getQNameForTagLocName(currentName);
                                eventQueue.add(new StartElementEvent(currentQName, new StaxLocation(parser.getCurrentLocation())));
                                processingStack.add(new ProcessingInfo(currentQName, false, true));
                                return;
                            } else {
                                parser.nextToken();
                                if (valueTokens.contains(parser.getCurrentToken())) {
                                    eventQueue.add(new CharactersEvent(parser.getText(), new StaxLocation(parser.getCurrentLocation())));
                                    return;
                                } else {
                                    throw new IOException("Not a xml value, expected primitive value!");
                                }
                            }
                        }
                        break;
                    case START_OBJECT:
                        if (pi == null) {
                            eventQueue.add(new StartDocumentEvent(new StaxLocation(0, 0, 0)));
                            return;
                        }
                        if (pi.isArray && !pi.isFirstElement) {
                            eventQueue.add(new StartElementEvent(pi.name, new StaxLocation(parser.getCurrentLocation())));
                            return;
                        } else {
                            pi.isFirstElement = false;
                        }
                        break;
                    case END_OBJECT:
                        lookingForAttributes = false;
                        // end tag
                        eventQueue.add(new EndElementEvent(pi.name, new StaxLocation(parser.getCurrentLocation())));
                        if (!pi.isArray) {
                            pop(processingStack);
                        }
                        if (processingStack.isEmpty()) {
                            eventQueue.add(new EndDocumentEvent(new StaxLocation(parser.getCurrentLocation())));
                        }
                        return;
                    case VALUE_FALSE:
                    case VALUE_NULL:
                    case VALUE_NUMBER_FLOAT:
                    case VALUE_NUMBER_INT:
                    case VALUE_TRUE:
                    case VALUE_STRING:
                        if (!pi.isFirstElement) {
                            eventQueue.add(new StartElementEvent(pi.name, new StaxLocation(parser.getCurrentLocation())));
                        } else {
                            pi.isFirstElement = false;
                        }
                        if (jtok != org.codehaus.jackson.JsonToken.VALUE_NULL) {
                            eventQueue.add(new CharactersEvent(parser.getText(), new StaxLocation(parser.getCurrentLocation())));
                        }
                        eventQueue.add(new EndElementEvent(pi.name, new StaxLocation(parser.getCurrentLocation())));
                        if (!pi.isArray) {
                            pop(processingStack);
                        }
                        if (processingStack.isEmpty()) {
                            eventQueue.add(new EndDocumentEvent(new StaxLocation(parser.getCurrentLocation())));
                        }
                        lookingForAttributes = false;
                        return;
                    case START_ARRAY:
                        peek(processingStack).isArray = true;
                        break;
                    case END_ARRAY:
                        pop(processingStack);
                        lookingForAttributes = false;
                }
            }
        }
    }

    @Override
    public void require(int arg0, String arg1, String arg2) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getElementText() throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public int next() throws XMLStreamException {
        try {
            readNext();
            return eventQueue.peek().getEventType();
        } catch (IOException ex) {
            Logger.getLogger(JsonXmlStreamReader.class.getName()).log(Level.SEVERE, null, ex);
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public int nextTag() throws XMLStreamException {
        int eventType = next();
        while ((eventType == XMLStreamConstants.CHARACTERS && isWhiteSpace()) // skip whitespace
                || (eventType == XMLStreamConstants.CDATA && isWhiteSpace()) // skip whitespace
                || eventType == XMLStreamConstants.SPACE || eventType == XMLStreamConstants.PROCESSING_INSTRUCTION || eventType == XMLStreamConstants.COMMENT) {
            eventType = next();
        }
        if (eventType != XMLStreamConstants.START_ELEMENT && eventType != XMLStreamConstants.END_ELEMENT) {
            throw new XMLStreamException("expected start or end tag", getLocation());
        }
        return eventType;
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void close() throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getNamespaceURI(String arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isStartElement() {
        return eventQueue.peek().isStartElement();
    }

    @Override
    public boolean isEndElement() {
        return eventQueue.peek().isEndElement();
    }

    @Override
    public boolean isCharacters() {
        return eventQueue.peek().isCharacters();
    }

    @Override
    public boolean isWhiteSpace() {
        return false;
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        return eventQueue.peek().getAttributeValue(namespaceURI, localName);
    }

    @Override
    public int getAttributeCount() {
        try {

            if (!eventQueue.peek().attributesChecked) {

                elemsExpected.clear();
                qNamesOfExpElems.clear();
                attrsExpected.clear();
                qNamesOfExpAttrs.clear();

                final UnmarshallingContext uctx = UnmarshallingContext.getInstance();

                if (uctx != null) {
                    try {
                        Collection<QName> currExpElems = uctx.getCurrentExpectedElements();
                        for (QName n : currExpElems) {
                            String nu = n.getNamespaceURI();
                            if (nu != null && (nu.equals("\u0000"))) {
                                elemsExpected.add("$");
                                qNamesOfExpElems.put("$", null);
                            } else {
                                elemsExpected.add(n.getLocalPart());
                                qNamesOfExpElems.put(n.getLocalPart(), n);
                            }
                        }
                    } catch (NullPointerException npe) {
                        // TODO: need to check what could be done in JAXB in order to prevent the npe
                        // thrown from com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext#1206
                    }

                    if (properJAXBVersion) {
                        try {
                            Collection<QName> currExpAttrs = uctx.getCurrentExpectedAttributes();
                            for (QName n : currExpAttrs) {
                                attrsExpected.add(n.getLocalPart());
                                qNamesOfExpAttrs.put(n.getLocalPart(), n);
                            }
                        } catch (NullPointerException npe) {
                            // thrown from com.sun.xml.bind.v2.runtime.unmarshaller.UnmarshallingContext
                        } catch (NoSuchMethodError nsme) {
                            // thrown when JAXB version is less than 2.1.12
                            properJAXBVersion = false;
                            Logger.getLogger(Jackson2StaxReader.class.getName()).log(Level.SEVERE, LocalizationMessages.ERROR_JAXB_RI_2_1_12_MISSING(), nsme);
                        }
                    }
                }

                readNext(true);
                eventQueue.peek().attributesChecked = true;
            }
        } catch (IOException ex) {
            Logger.getLogger(Jackson2StaxReader.class.getName()).log(Level.SEVERE, null, ex);
        }
        return eventQueue.peek().getAttributeCount();
    }

    @Override
    public QName getAttributeName(
            int index) {
        return eventQueue.peek().getAttributeName(index);
    }

    @Override
    public String getAttributeNamespace(
            int index) {
        return eventQueue.peek().getAttributeNamespace(index);
    }

    @Override
    public String getAttributeLocalName(
            int index) {
        return eventQueue.peek().getAttributeLocalName(index);
    }

    @Override
    public String getAttributePrefix(
            int index) {
        return eventQueue.peek().getAttributePrefix(index);
    }

    @Override
    public String getAttributeType(
            int index) {
        return eventQueue.peek().getAttributeType(index);
    }

    @Override
    public String getAttributeValue(
            int index) {
        return eventQueue.peek().getAttributeValue(index);
    }

    @Override
    public boolean isAttributeSpecified(int index) {
        return eventQueue.peek().isAttributeSpecified(index);
    }

    @Override
    public int getNamespaceCount() {
        return this.namespaceContext.getNamespaceCount();
    }

    @Override
    public String getNamespacePrefix(int idx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getNamespaceURI(int idx) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        return this.namespaceContext;
    }

    @Override
    public int getEventType() {
        return eventQueue.peek().getEventType();
    }

    @Override
    public String getText() {
        return eventQueue.peek().getText();
    }

    @Override
    public char[] getTextCharacters() {
        return eventQueue.peek().getTextCharacters();
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        return eventQueue.peek().getTextCharacters(sourceStart, target, targetStart, length);
    }

    @Override
    public int getTextStart() {
        return eventQueue.peek().getTextStart();
    }

    @Override
    public int getTextLength() {
        return eventQueue.peek().getTextLength();
    }

    @Override
    public String getEncoding() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public Location getLocation() {
        return eventQueue.peek().getLocation();
    }

    @Override
    public QName getName() {
        return eventQueue.peek().getName();
    }

    @Override
    public String getLocalName() {
        return eventQueue.peek().getLocalName();
    }

    @Override
    public boolean hasName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getNamespaceURI() {
        return eventQueue.peek().getName().getNamespaceURI();
    }

    @Override
    public String getPrefix() {
        return eventQueue.peek().getPrefix();
    }

    @Override
    public String getVersion() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isStandalone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean standaloneSet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getCharacterEncodingScheme() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPITarget() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPIData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }
}
