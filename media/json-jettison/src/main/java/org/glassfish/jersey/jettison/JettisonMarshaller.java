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

import java.io.OutputStream;
import java.io.Writer;

import javax.xml.bind.JAXBException;
import javax.xml.bind.PropertyException;

/**
 * A JSON marshaller responsible for serializing Java content trees, defined
 * by JAXB, to JSON data.
 *
 * @author Paul Sandoz
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public interface JettisonMarshaller {

    public static final String FORMATTED = "org.glassfish.jersey.media.json.JsonMarshaller.formatted";

    /**
     * Marshall the content tree rooted at <code>jaxbElement</code> into an
     * output stream. The content tree may be an instance of a class that is
     * mapped to a XML root element (for example, annotated with
     * {@link javax.xml.bind.annotation.XmlRootElement}) or an instance of {@link javax.xml.bind.JAXBElement}.
     * <p>
     * The UTF-8 character encoding scheme will be used to encode the characters
     * of the JSON data.
     *
     * @param jaxbElement the root of the content tree to be marshalled.
     * @param os the JSON will be added to this stream.
     * @throws javax.xml.bind.JAXBException if any unexpected problem occurs during the
     *         marshalling.
     * @throws javax.xml.bind.MarshalException if the <code>JsonMarshaller</code> is unable to
     *         marshal <code>jaxbElement</code> (or any object reachable from obj)
     * @throws IllegalArgumentException if any of the method parameters are null.
     *
     */
    void marshallToJSON(Object jaxbElement, OutputStream os) throws JAXBException;

    /**
     * Marshall the content tree rooted at <code>jaxbElement</code> into an
     * output stream. The content tree may be an instance of a class that is
     * mapped to a XML root element (for example, annotated with
     * {@link javax.xml.bind.annotation.XmlRootElement}) or an instance of {@link javax.xml.bind.JAXBElement}.
     * <p>
     * The character encoding scheme of the <code>writer</code> will be used to
     * encode the characters of the JSON data.
     *
     * @param jaxbElement the root of the content tree to be marshalled.
     * @param writer the JSON will be added to this writer.
     * @throws javax.xml.bind.JAXBException if any unexpected problem occurs during the
     *         marshalling.
     * @throws javax.xml.bind.MarshalException if the <code>JsonMarshaller</code> is unable to
     *         marshal <code>jaxbElement</code> (or any object reachable from obj)
     * @throws IllegalArgumentException If any of the method parameters are null.
     */
    void marshallToJSON(Object jaxbElement, Writer writer) throws JAXBException;

    /**
     * Set the particular property in the underlying implementation of
     * {@link JettisonMarshaller}. Attempting to set an undefined property
     * will result in a PropertyException being thrown.
     *
     * @param name the name of the property to be set. This value can either
     *              be specified using one of the constant fields or a user
     *              supplied string.
     * @param value the value of the property to be set
     *
     * @throws javax.xml.bind.PropertyException when there is an error processing the given
     *                            property or value
     * @throws IllegalArgumentException
     *      If the name parameter is null
     */
    void setProperty(String name, Object value) throws PropertyException;
}
