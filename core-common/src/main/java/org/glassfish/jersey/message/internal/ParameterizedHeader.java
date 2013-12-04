/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.message.internal;

import java.text.ParseException;
import java.util.Collections;
import java.util.Map;

/**
 * A general parameterized header.
 * <p/>
 * The header consists of a value and zero or more parameters. A value consists of zero or more tokens and separators up to but
 * not including a ';' separator if present. The tokens and separators of a value may be separated by zero or more white space,
 * which is ignored and is not considered part of the value. The value is separated from the parameters with a ';'. Each
 * parameter is separated with a ';'.
 *
 * @author Paul Sandoz
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class ParameterizedHeader {

    private String value;
    private Map<String, String> parameters;

    /**
     * Create a parameterized header from given string value.
     *
     * @param header header to create parameterized header from.
     * @throws ParseException if an un-expected/in-correct value is found during parsing the header.
     */
    public ParameterizedHeader(final String header) throws ParseException {
        this(HttpHeaderReader.newInstance(header));
    }

    /**
     * Create a parameterized header from given {@link HttpHeaderReader http header reader}.
     *
     * @param reader reader to initialize new parameterized header from.
     * @throws ParseException if an un-expected/in-correct value is found during parsing the header.
     */
    public ParameterizedHeader(final HttpHeaderReader reader) throws ParseException {
        reader.hasNext();

        value = "";
        while (reader.hasNext() && !reader.hasNextSeparator(';', false)) {
            reader.next();
            value += reader.getEventValue();
        }

        if (reader.hasNext()) {
            parameters = HttpHeaderReader.readParameters(reader);
        }
        if (parameters == null) {
            parameters = Collections.emptyMap();
        } else {
            parameters = Collections.unmodifiableMap(parameters);
        }
    }

    /**
     * Get the value.
     *
     * @return the value.
     */
    public String getValue() {
        return value;
    }

    /**
     * Get the parameters.
     *
     * @return the parameters
     */
    public Map<String, String> getParameters() {
        return parameters;
    }

}
