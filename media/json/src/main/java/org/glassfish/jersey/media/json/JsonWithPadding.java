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
package org.glassfish.jersey.media.json;

import java.io.IOException;
import org.codehaus.jackson.JsonGenerator;
import org.codehaus.jackson.JsonProcessingException;
import org.codehaus.jackson.map.JsonSerializableWithType;
import org.codehaus.jackson.map.SerializerProvider;
import org.codehaus.jackson.map.TypeSerializer;

/**
 * An entity supporting JSON with Padding (JSONP).
 * <p>
 * If an instance is returned by a resource method and the most acceptable
 * media type is one of application/javascript, application/x-javascript,
 * text/ecmascript, application/ecmascript or text/jscript then the object
 * that is contained by the instance is serialized as JSON (if supported, using
 * the application/json media type) and the result is wrapped around a
 * JavaScript callback function, whose name by default is "callback". Otherwise,
 * the object is serialized directly according to the most acceptable media type.
 * This means that an instance can be used to produce the media types
 * application/json, application/xml in addition to application/x-javascript.
 *
 * @author Jakub.Podlesak@Sun.COM
 */
public class JsonWithPadding implements JsonSerializableWithType {

    public static final String DEFAULT_CALLBACK_NAME = "callback";
    private final String callbackName;
    private final Object jsonSource;

    /**
     * Pad JSON using the default function name "callback".
     *
     * @param jsonSource the JSON to pad.
     */
    public JsonWithPadding(Object jsonSource) {
        this(jsonSource, DEFAULT_CALLBACK_NAME);
    }

    /**
     * Pad JSON using a declared callback function name.
     *
     * @param jsonSource the JSON to pad.
     * @param callbackName the callback function name.
     */
    public JsonWithPadding(Object jsonSource, String callbackName) {
        if (jsonSource == null) {
            throw new IllegalArgumentException("JSON source MUST not be null");
        }

        this.jsonSource = jsonSource;
        this.callbackName = (callbackName == null) ? DEFAULT_CALLBACK_NAME : callbackName;
    }

    /**
     * Get the callback function name.
     *
     * @return the callback function name.
     */
    public String getCallbackName() {
        return callbackName;
    }

    /**
     * Get the JSON source.
     *
     * @return the JSON source.
     */
    public Object getJsonSource() {
        return jsonSource;
    }

    @Override
    public void serialize(JsonGenerator jgen, SerializerProvider provider)
            throws IOException, JsonProcessingException {
        if (jsonSource == null) {
            provider.getNullValueSerializer().serialize(null, jgen, provider);
        } else {
            Class<?> cls = jsonSource.getClass();
            provider.findTypedValueSerializer(cls, true).serialize(jsonSource, jgen, provider);
        }
    }

    @Override
    public void serializeWithType(JsonGenerator jgen, SerializerProvider provider, TypeSerializer typeSer) throws IOException, JsonProcessingException {
        serialize(jgen, provider);
    }
}