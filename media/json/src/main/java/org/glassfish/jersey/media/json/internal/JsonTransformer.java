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

import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.Map;

import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONException;
import org.codehaus.jettison.json.JSONObject;

/**
 * JSON transformer.
 *
 * @author Jakub Podlesak
 */
final class JsonTransformer {
    @SuppressWarnings("unchecked")
    static <T> Map<String, T> asMap(String jsonObjectVal) throws JSONException {
        if (null == jsonObjectVal) {
            return null;
        }
        Map<String, T> result = new HashMap<String, T>();

        JSONObject sourceMap = new JSONObject(jsonObjectVal);
        Iterator<String> keyIterator = sourceMap.keys();
        while (keyIterator.hasNext()) {
            String key = keyIterator.next();
            result.put(key, (T)sourceMap.get(key));
        }
        return result;
    }


    @SuppressWarnings("unchecked")
    static <T> Collection<T> asCollection(String jsonArrayVal) throws JSONException {
        if (null == jsonArrayVal) {
            return null;
        }
        Collection<T> result = new LinkedList<T>();

        JSONArray arrayVal = new JSONArray(jsonArrayVal);
        for (int i = 0; i < arrayVal.length(); i++) {
            result.add((T)arrayVal.get(i));
        }
        return result;
    }

    static String asJsonArray(Collection<? extends Object> collection) {
        return (null == collection) ? "[]" : (new JSONArray(collection)).toString();
    }

    static String asJsonObject(Map map) {
        return (null == map) ? "{}" : (new JSONObject(map)).toString();
    }
}
