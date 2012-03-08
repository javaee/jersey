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
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Queue;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.xml.namespace.NamespaceContext;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamConstants;
import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;

import org.glassfish.jersey.media.json.JsonConfiguration;

/**
 * JSON StAX reader.
 *
 * @author Jakub Podlesak
 */
public class JsonXmlStreamReader implements XMLStreamReader {

    private enum LaState {
        START,
        END,
        AFTER_OBJ_START_BRACE,
        BEFORE_OBJ_NEXT_KV_PAIR,
        BEFORE_COLON_IN_KV_PAIR,
        BEFORE_VALUE_IN_KV_PAIR,
        AFTER_OBJ_KV_PAIR,
        AFTER_ARRAY_START_BRACE,
        BEFORE_NEXT_ARRAY_ELEM,
        AFTER_ARRAY_ELEM
    };

    private static final Logger LOGGER = Logger.getLogger(JsonXmlStreamReader.class.getName());

    boolean jsonRootUnwrapping;
    String rootElementName;
    char nsSeparator;
    CharSequence nsSeparatorAsSequence;
    final Map<String, String> revertedXml2JsonNs = new HashMap<String, String>();
    final Collection<String> attrAsElemNames = new LinkedList<String>();

    JsonLexer lexer;
    JsonToken lastToken;

    private static final class ProcessingState {
        String lastName;
        LaState state;
        JsonReaderXmlEvent eventToReadAttributesFor;

        ProcessingState() {
            this(LaState.START);
        }

        ProcessingState(LaState state) {
            this(state, null);
        }

        ProcessingState(LaState state, String name) {
            this.state = state;
            this.lastName = name;
        }

        @Override
        public String toString() {
            return String.format("{lastName:%s,laState:%s}", lastName, state);
        }
    }

    final Queue<JsonReaderXmlEvent> eventQueue = new LinkedList<JsonReaderXmlEvent>();

    boolean endDocumentReached = false;

    List<ProcessingState> processingStack;
    int depth;

    public JsonXmlStreamReader(Reader reader, JsonConfiguration config) throws IOException {
        this(reader, config.isRootUnwrapping() ? "rootElement" : null, config);
    }

    public JsonXmlStreamReader(Reader reader, String rootElementName) throws IOException {
        this(reader, rootElementName, JsonConfiguration.DEFAULT);
    }

    public JsonXmlStreamReader(Reader reader, String rootElementName, JsonConfiguration config) throws IOException {
        this.jsonRootUnwrapping = (rootElementName != null);
        this.rootElementName = rootElementName;
        if (config.getAttributeAsElements() != null) {
            this.attrAsElemNames.addAll(config.getAttributeAsElements());
        }
        if (config.getXml2JsonNs() != null) {
            for (String uri : config.getXml2JsonNs().keySet()) {
                revertedXml2JsonNs.put(config.getXml2JsonNs().get(uri), uri);
            }
        }
        nsSeparator = config.getNsSeparator();
        nsSeparatorAsSequence = new StringBuffer(1).append(nsSeparator);
        lexer = new JsonLexer(reader);
        depth = 0;
        processingStack = new ArrayList<ProcessingState>();
        processingStack.add(new ProcessingState());
        readNext();
    }

    void colon() throws IOException {
        JsonToken token = nextToken();
        if (token.tokenType != JsonToken.COLON) {
            throw new JsonFormatException(token.tokenText, token.line, token.column, "Colon expected instead of \"" + token.tokenText + "\"");
        }
    }

    JsonToken nextToken() throws IOException {
        JsonToken result = lexer.yylex();
        return result;
    }

    private void valueRead() {
        if (LaState.BEFORE_VALUE_IN_KV_PAIR == processingStack.get(depth).state) {
            processingStack.get(depth).state = LaState.AFTER_OBJ_KV_PAIR;
        } else if (LaState.BEFORE_NEXT_ARRAY_ELEM == processingStack.get(depth).state) {
            processingStack.get(depth).state = LaState.AFTER_ARRAY_ELEM;
        } else if (LaState.AFTER_ARRAY_START_BRACE == processingStack.get(depth).state) {
            processingStack.get(depth).state = LaState.AFTER_ARRAY_ELEM;
        }
    }
    private static final Set<Integer> valueTokenTypes = new HashSet<Integer>() {

        {
            add(JsonToken.FALSE);
            add(JsonToken.TRUE);
            add(JsonToken.NULL);
            add(JsonToken.NUMBER);
            add(JsonToken.STRING);
        }
    };

    private void readNext() throws IOException {
        readNext(false);
    }

    private void readNext(boolean checkAttributesOnly) throws IOException {
        if (!checkAttributesOnly) {
            eventQueue.poll();
        }
        //boolean attributesStarted = false;
        while (eventQueue.isEmpty() || checkAttributesOnly) {
            lastToken = nextToken();
            if ((null == lastToken) || (LaState.END == processingStack.get(depth).state)) {
                if (jsonRootUnwrapping) {
                    generateEEEvent(processingStack.get(depth).lastName);
                }
                eventQueue.add(new EndDocumentEvent(new StaxLocation(lexer)));
                break;
            }
            switch (processingStack.get(depth).state) {
                case START:
                    if (0 == depth) {
                        eventQueue.add(new StartDocumentEvent(new StaxLocation(lexer)));
                        processingStack.get(depth).state = LaState.AFTER_OBJ_START_BRACE;
                        if (jsonRootUnwrapping) {
                            processingStack.get(depth).lastName = this.rootElementName;
                            StartElementEvent event = generateSEEvent(processingStack.get(depth).lastName);
                            processingStack.get(depth).eventToReadAttributesFor = event;
                        }
                        switch (lastToken.tokenType) {
                            case JsonToken.START_OBJECT:
                                processingStack.add(new ProcessingState(LaState.AFTER_OBJ_START_BRACE));
                                depth++;
                                break;
                            case JsonToken.START_ARRAY:
                                processingStack.add(new ProcessingState(LaState.AFTER_ARRAY_START_BRACE));
                                depth++;
                                break;
                            case JsonToken.STRING:
                            case JsonToken.NUMBER:
                            case JsonToken.TRUE:
                            case JsonToken.FALSE:
                            case JsonToken.NULL:
                                eventQueue.add(new CharactersEvent(lastToken.tokenText, new StaxLocation(lexer)));
                                processingStack.get(depth).state = LaState.END;
                                break;
                            default:
                                throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "Unexpected JSON token");
                        }
                    }
                    processingStack.get(depth).state = LaState.AFTER_OBJ_START_BRACE;
                    break;
                case AFTER_OBJ_START_BRACE:
                    switch (lastToken.tokenType) {
                        case JsonToken.STRING:
                            if (lastToken.tokenText.startsWith("@") || attrAsElemNames.contains(lastToken.tokenText)) { // eat attribute
                                String attrName = lastToken.tokenText.startsWith("@") ? lastToken.tokenText : ("@" + lastToken.tokenText);
                                colon();
                                lastToken = nextToken();
                                // TODO process attr value
                                if (!valueTokenTypes.contains(lastToken.tokenType)) {
                                    throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "Attribute value expected instead of \"" + lastToken.tokenText + "\"");
                                }
                                if (null != processingStack.get(depth - 1).eventToReadAttributesFor) {
                                    processingStack.get(depth - 1).eventToReadAttributesFor.addAttribute(
                                            createQName(attrName.substring(1)), lastToken.tokenText);
                                }
                                lastToken = nextToken();
                                switch (lastToken.tokenType) {
                                    case JsonToken.END_OBJECT:
                                        processingStack.remove(depth);
                                        depth--;
                                        valueRead();
                                        checkAttributesOnly = false;
                                        break;
                                    case JsonToken.COMMA:
                                        break;
                                    default:
                                        throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "\'\"\', or \'}\' expected instead of \"" + lastToken.tokenText + "\"");
                                }
                            } else { // non attribute
                                StartElementEvent event =
                                        generateSEEvent(lastToken.tokenText);
                                processingStack.get(depth).eventToReadAttributesFor = event;
                                checkAttributesOnly = false;
                                processingStack.get(depth).lastName = lastToken.tokenText;
                                colon();
                                processingStack.get(depth).state = LaState.BEFORE_VALUE_IN_KV_PAIR;
                            }
                            break;
                        case JsonToken.END_OBJECT: // empty object/element
                            generateEEEvent(processingStack.get(depth).lastName);
                            checkAttributesOnly = false;
                            processingStack.remove(depth);
                            depth--;
                            valueRead();
                            break;
                        default:
                            throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "Unexpected JSON token");
                    }
                    break;
                case BEFORE_OBJ_NEXT_KV_PAIR:
                    switch (lastToken.tokenType) {
                        case JsonToken.STRING:
                            StartElementEvent event =
                                    generateSEEvent(lastToken.tokenText);
                            processingStack.get(depth).eventToReadAttributesFor = event;
                            processingStack.get(depth).lastName = lastToken.tokenText;
                            colon();
                            processingStack.get(depth).state = LaState.BEFORE_VALUE_IN_KV_PAIR;
                            break;
                        default:
                            throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "Unexpected JSON token");
                    }
                    break;
                case BEFORE_VALUE_IN_KV_PAIR:
                    switch (lastToken.tokenType) {
                        case JsonToken.START_OBJECT:
                            processingStack.add(new ProcessingState(LaState.AFTER_OBJ_START_BRACE));
                            depth++;
                            break;
                        case JsonToken.START_ARRAY:
                            processingStack.add(new ProcessingState(LaState.AFTER_ARRAY_START_BRACE));
                            depth++;
                            break;
                        case JsonToken.STRING:
                        case JsonToken.NUMBER:
                        case JsonToken.TRUE:
                        case JsonToken.FALSE:
                            eventQueue.add(new CharactersEvent(lastToken.tokenText, new StaxLocation(lexer)));
                            processingStack.get(depth).state = LaState.AFTER_OBJ_KV_PAIR;
                            break;
                        case JsonToken.NULL:
                            //TODO: optionally generate a fake xsi:nil attribute
//                            if (!eventQueue.isEmpty()) {
//                                 eventQueue.peek().addAttribute(new QName("http://www.w3.org/2001/XMLSchema-instance", "nil"), "true");
//                            }
                            processingStack.get(depth).state = LaState.AFTER_OBJ_KV_PAIR;
                            break;
                        default:
                            throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "Unexpected JSON token");
                    }
                    break; // AFTER_ARRAY_ELEM
                case AFTER_OBJ_KV_PAIR:
                    switch (lastToken.tokenType) {
                        case JsonToken.COMMA:
                            processingStack.get(depth).state = LaState.BEFORE_OBJ_NEXT_KV_PAIR;
                            generateEEEvent(processingStack.get(depth).lastName);
                            break; // STRING
                        case JsonToken.END_OBJECT: // empty object/element
                            generateEEEvent(processingStack.get(depth).lastName);
                            processingStack.remove(depth);
                            depth--;
                            valueRead();
                            break; // END_OBJECT
                        default:
                            throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "Unexpected JSON token");
                    }
                    break; // AFTER_OBJ_KV_PAIR
                case AFTER_ARRAY_START_BRACE:
                    switch (lastToken.tokenType) {
                        case JsonToken.START_OBJECT:
                            processingStack.add(new ProcessingState(LaState.AFTER_OBJ_START_BRACE));
                            processingStack.get(depth).eventToReadAttributesFor = processingStack.get(depth - 1).eventToReadAttributesFor;
                            depth++;
                            break;
                        case JsonToken.START_ARRAY:
                            processingStack.add(new ProcessingState(LaState.AFTER_ARRAY_START_BRACE));
                            depth++;
                            break;
                        case JsonToken.END_ARRAY:
                            processingStack.remove(depth);
                            depth--;
                            valueRead();
                            break;
                        case JsonToken.STRING:
                            eventQueue.add(new CharactersEvent(lastToken.tokenText, new StaxLocation(lexer)));
                            processingStack.get(depth).state = LaState.AFTER_ARRAY_ELEM;
                            break;
                        default:
                            throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "Unexpected JSON token");
                    }
                    break; // AFTER_ARRAY_ELEM
                case BEFORE_NEXT_ARRAY_ELEM:
                    StartElementEvent event =
                            generateSEEvent(processingStack.get(depth - 1).lastName);
                    switch (lastToken.tokenType) {
                        case JsonToken.START_OBJECT:
                            processingStack.add(new ProcessingState(LaState.AFTER_OBJ_START_BRACE));
                            processingStack.get(depth).eventToReadAttributesFor = event;
                            depth++;
                            break;
                        case JsonToken.START_ARRAY:
                            processingStack.add(new ProcessingState(LaState.AFTER_ARRAY_START_BRACE));
                            depth++;
                            break;
                        case JsonToken.STRING:
                            eventQueue.add(new CharactersEvent(lastToken.tokenText, new StaxLocation(lexer)));
                            processingStack.get(depth).state = LaState.AFTER_ARRAY_ELEM;
                            break;
                        default:
                            throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "Unexpected JSON token");                   }
                    break; // BEFORE_NEXT_ARRAY_ELEM
                case AFTER_ARRAY_ELEM:
                    switch (lastToken.tokenType) {
                        case JsonToken.END_ARRAY:
                            processingStack.remove(depth);
                            depth--;
                            valueRead();
                            break;
                        case JsonToken.COMMA:
                            processingStack.get(depth).state = LaState.BEFORE_NEXT_ARRAY_ELEM;
                            generateEEEvent(processingStack.get(depth - 1).lastName);
                            break;
                        default:
                            throw new JsonFormatException(lastToken.tokenText, lastToken.line, lastToken.column, "Unexpected JSON token");
                    }
                    break; // AFTER_ARRAY_ELEM
            }
        } // end while lastEvent null
    }

    @Override
    public int getAttributeCount() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getAttributeCount");
        assert !eventQueue.isEmpty();
        if (!eventQueue.peek().attributesChecked) {
            try {
                readNext(true);
            } catch (IOException e) {
                throw new JsonFormatException("...", -1, -1, "Error counting attributes");
            }
            eventQueue.peek().attributesChecked = true;
        }
        int result = eventQueue.peek().getAttributeCount();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getAttributeCount", result);
        return result;
    }

    @Override
    public int getEventType() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getEventType");
        assert !eventQueue.isEmpty();
        int result = eventQueue.peek().getEventType();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getEventType", result);
        return result;
    }

    @Override
    public int getNamespaceCount() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getNamespaceCount");
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getNamespaceCount", 0);
        return 0;
    }

    @Override
    public int getTextLength() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getTextLength");
        assert !eventQueue.isEmpty();
        int result = eventQueue.peek().getTextLength();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getTextLength", result);
        return result;
    }

    @Override
    public int getTextStart() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getTextStart");
        assert !eventQueue.isEmpty();
        int result = eventQueue.peek().getTextStart();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getTextStart", result);
        return result;
    }

    @Override
    public int next() throws XMLStreamException {
        endDocumentCheck();
        try {
            readNext();
            final int nextEventType = eventQueue.peek().getEventType();
            if (nextEventType == XMLStreamConstants.END_DOCUMENT) {
                endDocumentReached = true;
            }
            return nextEventType;
        } catch (IOException ex) {
            Logger.getLogger(JsonXmlStreamReader.class.getName()).log(Level.SEVERE, null, ex);
            throw new XMLStreamException(ex);
        }
    }

    @Override
    public int nextTag() throws XMLStreamException {
        endDocumentCheck();
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
    public void close() throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasName() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean hasNext() throws XMLStreamException {
        return !endDocumentReached;
    }

    @Override
    public boolean hasText() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isCharacters() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "isCharacters");
        assert !eventQueue.isEmpty();
        boolean result = eventQueue.peek().isCharacters();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "isCharacters", result);
        return result;
    }

    @Override
    public boolean isEndElement() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "isEndElement");
        assert !eventQueue.isEmpty();
        boolean result = eventQueue.peek().isEndElement();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "isEndElement", result);
        return result;
    }

    @Override
    public boolean isStandalone() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public boolean isStartElement() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "isStartElement");
        assert !eventQueue.isEmpty();
        boolean result = eventQueue.peek().isStartElement();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "isStartElement", result);
        return result;
    }

    @Override
    public boolean isWhiteSpace() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "isWhiteSpace");
        boolean result = false; // white space processed by lexer
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "isWhiteSpace", result);
        return result;
    }

    @Override
    public boolean standaloneSet() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public char[] getTextCharacters() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getTextCharacters");
        assert !eventQueue.isEmpty();
        char[] result = eventQueue.peek().getTextCharacters();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getTextCharacters", result);
        return result;
    }

    @Override
    public boolean isAttributeSpecified(int attribute) {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "isAttributeSpecified");
        assert !eventQueue.isEmpty();
        boolean result = eventQueue.peek().isAttributeSpecified(attribute);
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "isAttributeSpecified", result);
        return result;
    }

    @Override
    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getTextCharacters");
        assert !eventQueue.isEmpty();
        int result = eventQueue.peek().getTextCharacters(sourceStart, target, targetStart, length);
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getTextCharacters", result);
        return result;
    }

    @Override
    public String getCharacterEncodingScheme() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getElementText() throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getEncoding() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getLocalName() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getLocalName");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getLocalName();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getLocalName", result);
        return result;
    }

    @Override
    public String getNamespaceURI() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getNamespaceURI");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getName().getNamespaceURI();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getNamespaceURI", result);
        return result;
    }

    @Override
    public String getPIData() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPITarget() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getPrefix() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getPrefix");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getPrefix();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getPrefix", result);
        return result;
    }

    @Override
    public String getText() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getText");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getText();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getText", result);
        return result;
    }

    @Override
    public String getVersion() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getAttributeLocalName(int index) {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getAttributeLocalName");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getAttributeLocalName(index);
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getAttributeLocalName", result);
        return result;
    }

    @Override
    public QName getAttributeName(int index) {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getAttributeName");
        assert !eventQueue.isEmpty();
        QName result = eventQueue.peek().getAttributeName(index);
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getAttributeName", result);
        return result;
    }

    @Override
    public String getAttributeNamespace(int index) {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getAttributeNamespace");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getAttributeNamespace(index);
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getAttributeNamespace", result);
        return result;
    }

    @Override
    public String getAttributePrefix(int index) {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getAttributePrefix");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getAttributePrefix(index);
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getAttributePrefix", result);
        return result;
    }

    @Override
    public String getAttributeType(int index) {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getAttributeType");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getAttributeType(index);
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getAttributeType", result);
        return result;
    }

    @Override
    public String getAttributeValue(int index) {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getAttributeValue");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getAttributeValue(index);
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getAttributeValue", result);
        return result;
    }

    @Override
    public String getAttributeValue(String namespaceURI, String localName) {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getAttributeValue");
        assert !eventQueue.isEmpty();
        String result = eventQueue.peek().getAttributeValue(namespaceURI, localName);
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getAttributeValue", result);
        return result;
    }

    @Override
    public String getNamespacePrefix(int arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getNamespaceURI(int arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public NamespaceContext getNamespaceContext() {
        // TODO: put/take it to/from processing stack
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getNamespaceContext");
        NamespaceContext result = new JsonNamespaceContext();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getNamespaceContext", result);
        return result;
    }

    @Override
    public QName getName() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getName");
        assert !eventQueue.isEmpty();
        QName result = eventQueue.peek().getName();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getName");
        return result;
    }

    @Override
    public Location getLocation() {
        LOGGER.entering(JsonXmlStreamReader.class.getName(), "getLocation");
        assert !eventQueue.isEmpty();
        Location result = eventQueue.peek().getLocation();
        LOGGER.exiting(JsonXmlStreamReader.class.getName(), "getLocation", result);
        return result;
    }

    @Override
    public Object getProperty(String arg0) throws IllegalArgumentException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public void require(int arg0, String arg1, String arg2) throws XMLStreamException {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public String getNamespaceURI(String arg0) {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    private StartElementEvent generateSEEvent(String name) {
        StartElementEvent event = null;
        if (!"$".equals(name)) {
           event = new StartElementEvent(createQName(name), new StaxLocation(lexer));
           eventQueue.add(event);
        }
        return event;
    }

    private void generateEEEvent(String name) {
       if ((null != name) && !"$".equals(name)) {
           eventQueue.add(new EndElementEvent(createQName(name), new StaxLocation(lexer)));
       }
    }

    private QName createQName(String name) {
        if (revertedXml2JsonNs.isEmpty() || !name.contains(nsSeparatorAsSequence)) {
            return new QName(name);
        } else {
            int dotIndex = name.indexOf(nsSeparator);
            String prefix = name.substring(0, dotIndex);
            String suffix = name.substring(dotIndex + 1);
            return revertedXml2JsonNs.containsKey(prefix) ? new QName(revertedXml2JsonNs.get(prefix), suffix) : new QName(name);
        }
    }

    private void endDocumentCheck() throws NoSuchElementException {
        if (endDocumentReached) {
            throw new NoSuchElementException();
        }
    }
}
