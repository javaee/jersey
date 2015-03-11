/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.wadl.internal;

import java.io.InputStream;
import java.util.List;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.UriInfo;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import javax.xml.parsers.ParserConfigurationException;
import javax.xml.parsers.SAXParser;
import javax.xml.parsers.SAXParserFactory;
import javax.xml.transform.sax.SAXSource;

import org.glassfish.jersey.server.internal.LocalizationMessages;

import org.xml.sax.InputSource;
import org.xml.sax.SAXException;

/**
 * Utils for wadl processing.
 *
 * @author Miroslav Fuksa
 *
 */
public class WadlUtils {


    /**
     * Name of the query parameter that allows generation of full WADL including
     * {@link org.glassfish.jersey.server.model.ExtendedResource extended resource}.
     */
    public static final String DETAILED_WADL_QUERY_PARAM = "detail";

    /**
     * Unmarshal a jaxb bean into a type of {@code resultClass} from the given {@code inputStream}.
     *
     * @param inputStream Input stream that contains input xml that should be processed.
     * @param saxParserFactory Sax parser factory for unmarshalling xml.
     * @param resultClass Class of the result bean into which the content of {@code inputStream} should be unmarshalled.
     * @param <T> Type of the result jaxb bean.
     * @return Unmarshalled jaxb bean.
     *
     * @throws JAXBException In case of jaxb problem.
     * @throws ParserConfigurationException In case of problem with parsing xml.
     * @throws SAXException In case of problem with parsing xml.
     */
    public static <T> T unmarshall(InputStream inputStream, SAXParserFactory saxParserFactory,
                                   Class<T> resultClass) throws JAXBException, ParserConfigurationException, SAXException {

        JAXBContext jaxbContext = null;
        try {
            jaxbContext = JAXBContext.newInstance(resultClass);
        } catch (JAXBException ex) {
            throw new ProcessingException(LocalizationMessages.ERROR_WADL_JAXB_CONTEXT(), ex);
        }

        final SAXParser saxParser = saxParserFactory.newSAXParser();
        SAXSource source = new SAXSource(saxParser.getXMLReader(), new InputSource(inputStream));
        final Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
        final Object result = unmarshaller.unmarshal(source);
        return resultClass.cast(result);
    }

    /**
     * Return {@code true} if generation of full WADL with
     * {@link org.glassfish.jersey.server.model.ExtendedResource extended resources} is requested.
     *
     * @param uriInfo URI info of the request.
     * @return {@code true} if full detailed WADL should be generated; false otherwise.
     */
    public static boolean isDetailedWadlRequested(UriInfo uriInfo) {
        final List<String> simple = uriInfo.getQueryParameters().get(DETAILED_WADL_QUERY_PARAM);

        if (simple != null) {
            if (simple.size() == 0) {
                return true;
            }

            final String value = simple.get(0).trim();
            return value.isEmpty() || value.toUpperCase().equals("TRUE");
        }
        return false;
    }

}
