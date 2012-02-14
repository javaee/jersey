/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message;

/**
 * Jersey configuration properties for message & entity processing.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class MessageProperties {

    /**
     * If set to {@code true} then XML root element tag name for collections will
     * be derived from {@link javax.xml.bind.annotation.XmlRootElement @XmlRootElement}
     * annotation value and won't be de-capitalized.
     * <p />
     * The default value is {@code false}.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    public static final String JAXB_PROCESS_XML_ROOT_ELEMENT = "jersey.config.jaxb.collections.processXmlRootElement";
    /**
     * If set to {@code true} XML security features when parsing XML documents will be
     * disabled.
     * <p />
     * The default value is {@code false}.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    public static final String XML_SECURITY_DISABLE = "jersey.config.xml.security.disable";
    /**
     * If set to {@code true} indicates that produced XML output should be formatted
     * if possible (see below).
     * <p />
     * A XML message entity written by a {@link javax.ws.rs.ext.MessageBodyWriter}
     * may be formatted for the purposes of human readability provided the respective
     * {@code MessageBodyWriter} supports XML output formatting. All JAXB-based message
     * body writers support this property.
     * <p />
     * The default value is {@code false}.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    public static final String XML_FORMAT_OUTPUT = "jersey.config.xml.formatOutput";
    /**
     * Value of the property indicates the buffer size to be used for I/O operations
     * on byte and character streams. The property value is expected to be a positive
     * integer otherwise it will be ignored.
     * <p />
     * The default value is <code>{@value #IO_DEFAULT_BUFFER_SIZE}</code>.
     * <p />
     * The name of the configuration property is <code>{@value}</code>.
     */
    public static final String IO_BUFFER_SIZE = "jersey.config.io.bufferSize";
    /**
     * The default buffer size ({@value}) for I/O operations on byte and character
     * streams.
     */
    public static final int IO_DEFAULT_BUFFER_SIZE = 8192;

    private MessageProperties() {
        // prevents instantiation
    }
}
