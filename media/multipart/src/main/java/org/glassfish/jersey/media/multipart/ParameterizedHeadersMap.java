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
package org.glassfish.jersey.media.multipart;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MultivaluedMap;

import org.glassfish.jersey.internal.util.collection.StringKeyIgnoreCaseMultivaluedMap;
import org.glassfish.jersey.message.internal.ParameterizedHeader;

/**
 * A map of MIME headers with parametrized values.
 * <p/>
 * An implementation of {@link MultivaluedMap} where keys are instances of String and are compared ignoring case and values are
 * instances of {@link ParameterizedHeader}.
 *
 * @author Craig McClanahan
 * @author Michal Gajdos
 */
/* package */ class ParameterizedHeadersMap extends StringKeyIgnoreCaseMultivaluedMap<ParameterizedHeader> {

    /**
     * Create new parameterized headers map.
     */
    public ParameterizedHeadersMap() {
    }

    /**
     * Create new parameterized headers map from given headers.
     *
     * @param headers headers to initialize this map from.
     * @throws ParseException if an un-expected/in-correct value is found during parsing the headers.
     */
    public ParameterizedHeadersMap(final MultivaluedMap<String, String> headers) throws ParseException {
        for (Map.Entry<String, List<String>> entry : headers.entrySet()) {
            List<ParameterizedHeader> list = new ArrayList<ParameterizedHeader>(entry.getValue().size());
            for (String value : entry.getValue()) {
                list.add(new ParameterizedHeader(value));
            }
            this.put(entry.getKey(), list);
        }
    }

}
