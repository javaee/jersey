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

package org.glassfish.jersey.jettison;

import java.io.InputStream;
import java.io.Reader;

import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;

/**
 * A JSON unmarshaller responsible for deserializing JSON data to a Java
 * content tree, defined by JAXB.
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public interface JettisonUnmarshaller {

    /**
     * Unmarshal JSON data from the specified <code>InputStream</code> and
     * return the resulting Java content tree.
     * <p>
     * The UTF-8 character encoding scheme will be used to decode the encoded
     * characters of the JSON data.
     *
     * @param <T> the type of the Java content tree.
     * @param is the InputStream to unmarshal JSON data from.
     * @param expectedType the expected type of the Java content tree.
     * @return the newly created root object of the Java content tree. The
     *         content tree may be an instance of a class that is
     *         mapped to a XML root element (for example, annotated with
     *         {@link javax.xml.bind.annotation.XmlRootElement}) or mapped to an XML type (for example,
     *         annotated with {@link javax.xml.bind.annotation.XmlType}).
     * @throws javax.xml.bind.JAXBException if any unexpected errors occur while unmarshalling.
     * @throws javax.xml.bind.UnmarshalException if the <code>JsonUnmarshaller</code> is unable
     *         to perform the JSON to Java binding.
     */
    <T> T unmarshalFromJSON(InputStream is, Class<T> expectedType) throws JAXBException;

    /**
     * Unmarshal JSON data from the specified <code>Reader</code> and
     * return the resulting Java content tree.
     * <p>
     * The character encoding scheme of the <code>reader</code> will be used to
     * encode the characters of the JSON data.
     *
     * @param <T> the type of the Java content tree.
     * @param reader the Reader to unmarshal JSON data from.
     * @param expectedType the expected type of the Java content tree.
     * @return the newly created root object of the Java content tree. The
     *         content tree may be an instance of a class that is
     *         mapped to a XML root element (for example, annotated with
     *         {@link javax.xml.bind.annotation.XmlRootElement}) or mapped to an XML type (for example,
     *         annotated with {@link javax.xml.bind.annotation.XmlType}).
     * @throws javax.xml.bind.JAXBException if any unexpected errors occur while unmarshalling.
     * @throws javax.xml.bind.UnmarshalException if the <code>JsonUnmarshaller</code> is unable
     *         to perform the JSON to Java binding.
     */
    <T> T unmarshalFromJSON(Reader reader, Class<T> expectedType) throws JAXBException;

    /**
     * Unmarshal JSON data from the <code>InputStream</code> by
     * <code>declaredType</code> and return the resulting content tree.
     * <p>
     * The UTF-8 character encoding scheme will be used to decode the encoded
     * characters of the JSON data.
     *
     * @param <T> the type of the Java content tree.
     * @param is the InputStream to unmarshal JSON data from.
     * @param declaredType a class that is mapped to a XML root element
     *        (for example, annotated with {@link javax.xml.bind.annotation.XmlRootElement}) or mapped to
     *        an XML type (for example, annotated with {@link javax.xml.bind.annotation.XmlType}).
     * @return the newly created root object of the Java content tree, root
     *         by a {@link javax.xml.bind.JAXBElement} instance.
     * @throws javax.xml.bind.JAXBException if any unexpected errors occur while unmarshalling.
     * @throws javax.xml.bind.UnmarshalException if the <code>JsonUnmarshaller</code> is unable
     *         to perform the JSON to Java binding.
     */
    <T> JAXBElement<T> unmarshalJAXBElementFromJSON(InputStream is, Class<T> declaredType) throws JAXBException;

    /**
     * Unmarshal JSON data from the <code>Reader</code> by
     * <code>declaredType</code> and return the resulting content tree.
     * <p>
     * The character encoding scheme of the <code>reader</code> will be used to
     * encode the characters of the JSON data.
     *
     * @param <T> the type of the Java content tree.
     * @param reader the Reader to unmarshal JSON data from.
     * @param declaredType a class that is mapped to a XML root element
     *        (for example, annotated with {@link javax.xml.bind.annotation.XmlRootElement}) or mapped to
     *        an XML type (for example, annotated with {@link javax.xml.bind.annotation.XmlType}).
     * @return the newly created root object of the Java content tree, root
     *         by a {@link javax.xml.bind.JAXBElement} instance.
     * @throws javax.xml.bind.JAXBException if any unexpected errors occur while unmarshalling.
     * @throws javax.xml.bind.UnmarshalException if the <code>JsonUnmarshaller</code> is unable
     *         to perform the JSON to Java binding.
     */
    <T> JAXBElement<T> unmarshalJAXBElementFromJSON(Reader reader, Class<T> declaredType) throws JAXBException;
}
