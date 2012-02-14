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
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.Base64Variant;
import org.codehaus.jackson.JsonLocation;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParseException;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonStreamContext;
import org.codehaus.jackson.JsonToken;
import org.codehaus.jackson.ObjectCodec;
import org.codehaus.jackson.type.TypeReference;

/**
 * Jackson parser that adds a configured data root.
 *
 * @author Jakub Podlesak
 */
public class JacksonRootAddingParser extends JsonParser {

    enum State {

        START, AFTER_SO, AFTER_FN, INNER, END
    };
    String rootName;
    JsonParser parser;
    State state;
    boolean isClosed = false;

    public static JsonParser createRootAddingParser(JsonParser parser, String rootName) {
        return new JacksonRootAddingParser(parser, rootName);
    }

    private JacksonRootAddingParser() {
    }

    private JacksonRootAddingParser(JsonParser parser, String rootName) {
        this.parser = parser;
        this.state = State.START;
        this.rootName = rootName;
    }

    @Override
    public void enableFeature(Feature feature) {
        parser.enableFeature(feature);
    }

    @Override
    public void disableFeature(Feature feature) {
        parser.disableFeature(feature);
    }

    @Override
    public void setFeature(Feature feature, boolean isSet) {
        parser.setFeature(feature, isSet);
    }

    @Override
    public JsonToken nextValue() throws IOException, JsonParseException {
        JsonToken result = nextToken();
        while (!result.isScalarValue()) {
            result = nextToken();
        }
        return result;
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }

    @Override
    public byte getByteValue() throws IOException, JsonParseException {
        return parser.getByteValue();
    }

    @Override
    public short getShortValue() throws IOException, JsonParseException {
        return parser.getShortValue();
    }

    @Override
    public BigInteger getBigIntegerValue() throws IOException, JsonParseException {
        return parser.getBigIntegerValue();
    }

    @Override
    public float getFloatValue() throws IOException, JsonParseException {
        return parser.getFloatValue();
    }

    @Override
    public byte[] getBinaryValue(Base64Variant base64Variant) throws IOException, JsonParseException {
        return parser.getBinaryValue(base64Variant);
    }

    @Override
    public <T> T readValueAs(Class<T> type) throws IOException, JsonProcessingException {
        return parser.readValueAs(type);
    }

    @Override
    public <T> T readValueAs(TypeReference<?> typeRef) throws IOException, JsonProcessingException {
        return (T) parser.readValueAs(typeRef);
    }

    @Override
    public JsonNode readValueAsTree() throws IOException, JsonProcessingException {
        return parser.readValueAsTree();
    }

    @Override
    public JsonStreamContext getParsingContext() {
        throw new UnsupportedOperationException("Not supported yet.");
    }

    @Override
    public JsonToken nextToken() throws IOException, JsonParseException {
        switch (state) {
            case START:
                state = State.AFTER_SO;
                _currToken = JsonToken.START_OBJECT;
                return _currToken;
            case AFTER_SO:
                state = State.AFTER_FN;
                _currToken = JsonToken.FIELD_NAME;
                return _currToken;
            case AFTER_FN:
                state = State.INNER;
            case INNER:
                _currToken = parser.nextToken();
                if (_currToken == null) {
                    state = State.END;
                    _currToken = JsonToken.END_OBJECT;
                }
                return _currToken;
            case END:
            default:
                _currToken = null;
                return _currToken;
        }
    }

    @Override
    public JsonParser skipChildren() throws IOException, JsonParseException {
        return parser.skipChildren();
    }

    @Override
    public String getCurrentName() throws IOException, JsonParseException {
        switch (state) {
            case START:
                return null;
            case AFTER_SO:
                return null;
            case AFTER_FN:
                return rootName;
            case INNER:
                return parser.getCurrentName();
            default:
                return null;
        }
    }

    @Override
    public void close() throws IOException {
        parser.close();
    }

    @Override
    public JsonLocation getTokenLocation() {
        return parser.getTokenLocation();
    }

    @Override
    public JsonLocation getCurrentLocation() {
        return parser.getCurrentLocation();
    }

    @Override
    public String getText() throws IOException, JsonParseException {
        return parser.getText();
    }

    @Override
    public char[] getTextCharacters() throws IOException, JsonParseException {
        return parser.getTextCharacters();
    }

    @Override
    public int getTextLength() throws IOException, JsonParseException {
        return parser.getTextLength();
    }

    @Override
    public int getTextOffset() throws IOException, JsonParseException {
        return parser.getTextOffset();
    }

    @Override
    public Number getNumberValue() throws IOException, JsonParseException {
        return parser.getNumberValue();
    }

    @Override
    public NumberType getNumberType() throws IOException, JsonParseException {
        return parser.getNumberType();
    }

    @Override
    public int getIntValue() throws IOException, JsonParseException {
        return parser.getIntValue();
    }

    @Override
    public long getLongValue() throws IOException, JsonParseException {
        return parser.getLongValue();
    }

    @Override
    public double getDoubleValue() throws IOException, JsonParseException {
        return parser.getDoubleValue();
    }

    @Override
    public BigDecimal getDecimalValue() throws IOException, JsonParseException {
        return parser.getDecimalValue();
    }

    @Override
    public ObjectCodec getCodec() {
        return parser.getCodec();
    }

    @Override
    public void setCodec(ObjectCodec c) {
        parser.setCodec(c);
    }
}
