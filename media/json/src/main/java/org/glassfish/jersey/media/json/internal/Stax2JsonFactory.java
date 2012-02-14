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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.glassfish.jersey.message.internal.ReaderWriter;
import org.glassfish.jersey.media.json.JsonConfiguration;
import org.glassfish.jersey.media.json.internal.reader.Jackson2StaxReader;
import org.glassfish.jersey.media.json.internal.reader.JacksonRootAddingParser;
import org.glassfish.jersey.media.json.internal.reader.JsonXmlStreamReader;
import org.glassfish.jersey.media.json.internal.writer.JacksonArrayWrapperGenerator;
import org.glassfish.jersey.media.json.internal.writer.JacksonRootStrippingGenerator;
import org.glassfish.jersey.media.json.internal.writer.JsonXmlStreamWriter;
import org.glassfish.jersey.media.json.internal.writer.Stax2JacksonWriter;

import org.codehaus.jackson.JsonFactory;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonParser;
import org.codehaus.jettison.badgerfish.BadgerFishXMLStreamReader;
import org.codehaus.jettison.badgerfish.BadgerFishXMLStreamWriter;
import org.codehaus.jettison.json.JSONObject;
import org.codehaus.jettison.json.JSONTokener;
import org.codehaus.jettison.mapped.Configuration;
import org.codehaus.jettison.mapped.MappedNamespaceConvention;
import org.codehaus.jettison.mapped.MappedXMLStreamReader;
import org.codehaus.jettison.mapped.MappedXMLStreamWriter;

/**
 * Factory for creating JSON-enabled StAX readers and writers.
 *
 * @author Jakub Podlesak
 */
public class Stax2JsonFactory {

    private Stax2JsonFactory() {
    }

    public static XMLStreamWriter createWriter(Writer writer, JsonConfiguration config) throws IOException {
        return createWriter(writer, config, false);
    }

    public static XMLStreamWriter createWriter(Writer writer, JsonConfiguration config, boolean writingList) throws IOException {
        switch (config.getNotation()) {
            case NATURAL:
                final JsonGenerator rawGenerator = new JsonFactory().createJsonGenerator(writer);
                if (config.isHumanReadableFormatting()) {
                    rawGenerator.useDefaultPrettyPrinter();
                }
                final JsonGenerator bodyGenerator = writingList ? JacksonArrayWrapperGenerator.createArrayWrapperGenerator(rawGenerator, config.isRootUnwrapping() ? 0 : 1) : rawGenerator;
                if (config.isRootUnwrapping()) {
                    return new Stax2JacksonWriter(JacksonRootStrippingGenerator.createRootStrippingGenerator(bodyGenerator, writingList ? 2 : 1), config);
                } else {
                    return new Stax2JacksonWriter(bodyGenerator, config);
                }
            case MAPPED:
                return JsonXmlStreamWriter.createWriter(writer, config);
            case BADGERFISH:
                return new BadgerFishXMLStreamWriter(writer);
            case MAPPED_JETTISON:
                Configuration jmConfig;
                if (null == config.getXml2JsonNs()) {
                    jmConfig = new Configuration();
                } else {
                    jmConfig = new Configuration(config.getXml2JsonNs());
                }
                return new MappedXMLStreamWriter(new MappedNamespaceConvention(jmConfig), writer);
            default:
                return null;
        }
    }

    public static XMLStreamReader createReader(Reader reader, JsonConfiguration config, String rootName) throws XMLStreamException {
        return createReader(reader, config, rootName, false);
    }

    public static XMLStreamReader createReader(Reader reader, JsonConfiguration config, String rootName, boolean readingList) throws XMLStreamException {

        Reader nonEmptyReader = ensureNonEmptyReader(reader);

        switch (config.getNotation()) {
            case NATURAL:
                try {
                    final JsonParser rawParser = new JsonFactory().createJsonParser(nonEmptyReader);
                    final JsonParser nonListParser = config.isRootUnwrapping() ? JacksonRootAddingParser.createRootAddingParser(rawParser, rootName) : rawParser;
                    if (!readingList) {
                        return new Jackson2StaxReader(nonListParser, config);
                    } else {
                        return new Jackson2StaxReader(JacksonRootAddingParser.createRootAddingParser(nonListParser, "jsonArrayRootElement"), config);
                    }
                } catch (Exception ex) {
                    throw new XMLStreamException(ex);
                }
            case MAPPED:
                try {
                    return new JsonXmlStreamReader(nonEmptyReader, rootName, config);
                } catch (IOException ex) {
                    throw new XMLStreamException(ex);
                }
            case MAPPED_JETTISON:
                try {
                    Configuration jmConfig;
                    if (null == config.getXml2JsonNs()) {
                        jmConfig = new Configuration();
                    } else {
                        jmConfig = new Configuration(config.getXml2JsonNs());
                    }
                    return new MappedXMLStreamReader(
                            new JSONObject(new JSONTokener(ReaderWriter.readFromAsString(nonEmptyReader))),
                            new MappedNamespaceConvention(jmConfig));
                } catch (Exception ex) {
                    throw new XMLStreamException(ex);
                }
            case BADGERFISH:
                try {
                    return new BadgerFishXMLStreamReader(new JSONObject(new JSONTokener(ReaderWriter.readFromAsString(nonEmptyReader))));
                } catch (Exception ex) {
                    throw new XMLStreamException(ex);
                }
        }
        // This should not occur
        throw new IllegalArgumentException("Unknown JSON config");
    }

    private static Reader ensureNonEmptyReader(Reader reader) throws XMLStreamException {
        try {
            Reader mr = reader.markSupported() ? reader : new BufferedReader(reader);
            mr.mark(1);
            if (mr.read() == -1) {
                throw new XMLStreamException("JSON expression can not be empty!");
            }
            mr.reset();
            return mr;
        } catch (IOException ex) {
            throw new XMLStreamException(ex);
        }
    }
}
