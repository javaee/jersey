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
package org.glassfish.jersey.media.json.internal.testutil;

import java.util.Iterator;
import java.util.Set;
import java.util.TreeSet;
import org.codehaus.jettison.json.JSONArray;
import org.codehaus.jettison.json.JSONObject;

/**
 *
 * @author Jakub Podlesak
 */
public class JSONNormalizer {

    public static String normalizeJsonString(String s) throws Exception {
        if (s == null) {
            return null;
        }

        s = s.trim();

        if (s.startsWith("{")) {
            return normalizeJsonObjectString(s);
        } else if (s.startsWith("[")) {
            return normalizeJsonArrayString(s);
        } else {
            return JSONObject.quote(s);
        }
    }

    private static String normalizeJsonObjectString(String s) throws Exception {

        JSONObject o = new JSONObject(s);
        StringBuilder result = new StringBuilder();

        result.append('{');
        boolean firstOne = true;

        for (String k : sortedKeySet(o)) {
            if (!firstOne) {
                result.append(',');
            } else {
                firstOne = false;
            }
            result.append('"').append(JSONObject.quote(k)).append('"').append(':');
            result.append(normalizeJsonString(o.getString(k)));
        }

        return result.append('}').toString();
    }

    private static Set<String> sortedKeySet(JSONObject o) {
        Iterator i = o.keys();
        Set<String> keys = new TreeSet<String>();
        while (i.hasNext()) {
            String k = (String) i.next();
            keys.add(k);
        }
        return keys;
    }

    private static String normalizeJsonArrayString(String s) throws Exception {

        JSONArray a = new JSONArray(s);

        StringBuilder result = new StringBuilder();
        result.append('[');

        for (int i = 0; i < a.length(); i++) {
            if (i > 0) {
                result.append(',');
            }
            result.append(normalizeJsonString(a.getString(i)));
        }

        return result.append(']').toString();
    }
}
