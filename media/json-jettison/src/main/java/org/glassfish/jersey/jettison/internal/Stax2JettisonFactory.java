/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.io.BufferedReader;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;

import javax.xml.stream.XMLStreamException;
import javax.xml.stream.XMLStreamReader;
import javax.xml.stream.XMLStreamWriter;

import org.glassfish.jersey.jettison.JettisonConfig;
import org.glassfish.jersey.message.internal.ReaderWriter;

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
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @author Michal Gajdos
 */
public class Stax2JettisonFactory {

    private Stax2JettisonFactory() {
    }

    public static XMLStreamWriter createWriter(final Writer writer,
                                               final JettisonConfig config) throws IOException {
        switch (config.getNotation()) {
            case BADGERFISH:
                return new BadgerFishXMLStreamWriter(writer);
            case MAPPED_JETTISON:
                Configuration jmConfig;
                if (null == config.getXml2JsonNs()) {
                    jmConfig = new Configuration();
                } else {
                    jmConfig = new Configuration(config.getXml2JsonNs());
                }

                final MappedXMLStreamWriter result = new MappedXMLStreamWriter(new MappedNamespaceConvention(jmConfig), writer);

                for (String array : config.getArrayElements()) {
                    result.serializeAsArray(array);
                }

                return result;
            default:
                return null;
        }
    }

    public static XMLStreamReader createReader(final Reader reader,
                                               final JettisonConfig config) throws XMLStreamException {
        Reader nonEmptyReader = ensureNonEmptyReader(reader);

        switch (config.getNotation()) {
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
                    return new BadgerFishXMLStreamReader(
                            new JSONObject(new JSONTokener(ReaderWriter.readFromAsString(nonEmptyReader))));
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
