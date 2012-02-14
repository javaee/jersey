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
import java.math.BigDecimal;
import java.math.BigInteger;

import org.codehaus.jackson.Base64Variant;
import org.codehaus.jackson.JsonGenerationException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonNode;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.JsonStreamContext;
import org.codehaus.jackson.ObjectCodec;

/**
 * Jackson root stripping JSON content generator.
 *
 * @author Jakub Podlesak
 */
public class JacksonRootStrippingGenerator extends JsonGenerator {

    final JsonGenerator generator;
    int depth = 0;
    boolean isClosed = false;
    final int treshold;

    private JacksonRootStrippingGenerator() {
        this(null, 1);
    }

    private JacksonRootStrippingGenerator(JsonGenerator generator) {
        this(generator, 1);
    }

    private JacksonRootStrippingGenerator(final JsonGenerator generator, final int treshold) {
        this.generator = generator;
        this.treshold = treshold;
    }

    public static JsonGenerator createRootStrippingGenerator(JsonGenerator g) {
        return new JacksonRootStrippingGenerator(g);
    }

    public static JsonGenerator createRootStrippingGenerator(JsonGenerator g, int treshold) {
        return new JacksonRootStrippingGenerator(g, treshold);
    }

    @Override
    @Deprecated
    public void enableFeature(Feature feature) {
        generator.enableFeature(feature);
    }

    @Override
    public JsonGenerator enable(Feature feature) {
        return generator.enable(feature);
    }

    @Override
    @Deprecated
    public void disableFeature(Feature feature) {
        generator.disableFeature(feature);
    }

    @Override
    public JsonGenerator disable(Feature feature) {
        return generator.disable(feature);
    }

    @Override
    public void setFeature(Feature feature, boolean enabled) {
        generator.setFeature(feature, enabled);
    }

    @Override
    @Deprecated
    public boolean isFeatureEnabled(Feature feature) {
        return generator.isFeatureEnabled(feature);
    }

    @Override
    public boolean isEnabled(Feature f) {
        return generator.isEnabled(f);
    }

    @Override
    public JsonGenerator useDefaultPrettyPrinter() {
        return generator.useDefaultPrettyPrinter();
    }

    @Override
    public void writeStartArray() throws IOException, JsonGenerationException {
        generator.writeStartArray();
    }

    @Override
    public void writeEndArray() throws IOException, JsonGenerationException {
        generator.writeEndArray();
    }

    @Override
    public void writeStartObject() throws IOException, JsonGenerationException {
        if (depth > treshold - 1) {
            generator.writeStartObject();
        }
        depth++;
    }

    @Override
    public void writeEndObject() throws IOException, JsonGenerationException {
        if (depth > treshold) {
            generator.writeEndObject();
        }
        depth--;
    }

    @Override
    public void writeFieldName(String name) throws IOException, JsonGenerationException {
        if (depth > treshold) {
            generator.writeFieldName(name);
        }
    }

    @Override
    public void writeString(String s) throws IOException, JsonGenerationException {
        generator.writeString(s);
    }

    @Override
    public void writeString(char[] text, int start, int length) throws IOException, JsonGenerationException {
        generator.writeString(text, start, length);
    }

    @Override
    public void writeRawUTF8String(byte[] bytes, int start, int length) throws IOException, JsonGenerationException {
        generator.writeRawUTF8String(bytes, start, length);
    }

    @Override
    public void writeUTF8String(byte[] bytes, int start, int length) throws IOException, JsonGenerationException {
        generator.writeUTF8String(bytes, start, length);
    }

    @Override
    public void writeRaw(String raw) throws IOException, JsonGenerationException {
        generator.writeRaw(raw);
    }

    @Override
    public void writeRaw(String raw, int start, int length) throws IOException, JsonGenerationException {
        generator.writeRaw(raw, start, length);
    }

    @Override
    public void writeRaw(char[] raw, int start, int count) throws IOException, JsonGenerationException {
        generator.writeRaw(raw, start, count);
    }

    @Override
    public void writeRaw(char c) throws IOException, JsonGenerationException {
        generator.writeRaw(c);
    }

    @Override
    public void writeBinary(Base64Variant variant, byte[] bytes, int start, int count) throws IOException, JsonGenerationException {
        generator.writeBinary(variant, bytes, start, count);
    }

    @Override
    public void writeNumber(int i) throws IOException, JsonGenerationException {
        generator.writeNumber(i);
    }

    @Override
    public void writeNumber(long l) throws IOException, JsonGenerationException {
        generator.writeNumber(l);
    }

    @Override
    public void writeNumber(double d) throws IOException, JsonGenerationException {
        generator.writeNumber(d);
    }

    @Override
    public void writeNumber(float f) throws IOException, JsonGenerationException {
        generator.writeNumber(f);
    }

    @Override
    public void writeNumber(BigDecimal bd) throws IOException, JsonGenerationException {
        generator.writeNumber(bd);
    }

    @Override
    public void writeNumber(String number) throws IOException, JsonGenerationException, UnsupportedOperationException {
        generator.writeNumber(number);
    }

    @Override
    public void writeBoolean(boolean b) throws IOException, JsonGenerationException {
        generator.writeBoolean(b);
    }

    @Override
    public void writeNull() throws IOException, JsonGenerationException {
        generator.writeNull();
    }

    @Override
    public void copyCurrentEvent(JsonParser parser) throws IOException, JsonProcessingException {
        generator.copyCurrentEvent(parser);
    }

    @Override
    public void copyCurrentStructure(JsonParser parser) throws IOException, JsonProcessingException {
        generator.copyCurrentStructure(parser);
    }

    @Override
    public void flush() throws IOException {
        generator.flush();
    }

    @Override
    public void close() throws IOException {
        generator.close();
        isClosed = true;
    }

    @Override
    public JsonGenerator setCodec(ObjectCodec codec) {
        return generator.setCodec(codec);
    }

    @Override
    public ObjectCodec getCodec() {
        return generator.getCodec();
    }

    @Override
    public void writeRawValue(String rawString) throws IOException, JsonGenerationException {
        generator.writeRawValue(rawString);
    }

    @Override
    public void writeRawValue(String rawString, int startIndex, int length) throws IOException, JsonGenerationException {
        generator.writeRawValue(rawString, startIndex, length);
    }

    @Override
    public void writeRawValue(char[] rawChars, int startIndex, int length) throws IOException, JsonGenerationException {
        generator.writeRawValue(rawChars, startIndex, length);
    }

    @Override
    public void writeNumber(BigInteger number) throws IOException, JsonGenerationException {
        generator.writeNumber(number);
    }

    @Override
    public void writeObject(Object o) throws IOException, JsonProcessingException {
        generator.writeObject(o);
    }

    @Override
    public void writeTree(JsonNode node) throws IOException, JsonProcessingException {
        generator.writeTree(node);
    }

    @Override
    public JsonStreamContext getOutputContext() {
        return generator.getOutputContext();
    }

    @Override
    public boolean isClosed() {
        return isClosed;
    }
}
