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
package org.glassfish.jersey.server.wadl.internal;

import java.util.Set;

import javax.ws.rs.core.MediaType;

import javax.xml.namespace.QName;

import org.glassfish.jersey.server.wadl.WadlGenerator;

import com.sun.research.ws.wadl.Application;

/**
 * This class is designed to combine the Application instance with any other
 * external metadata that might be required to describe the application.
 *
 * @author Gerard Davison
 */
public class ApplicationDescription {

    private Application _application;
    private WadlGenerator.ExternalGrammarDefinition _externalGrammarDefiniton;

    ApplicationDescription(Application application, WadlGenerator.ExternalGrammarDefinition externalGrammarDefiniton) {
        super();
        this._application = application;
        this._externalGrammarDefiniton = externalGrammarDefiniton;
    }

    /**
     * @return The instance of the application object
     */
    public Application getApplication() {
        return _application;
    }

    /**
     * @param type java class to be resolved.
     * @return the QName for the given Class in the grammar.
     */
    public QName resolve(Class type) {
        return _externalGrammarDefiniton.resolve(type);
    }

    /**
     * @param path path to external metadata.
     * @return the external metadata for a given URL, generally provided as a sub resource
     *         or the root application.wadl.
     */
    public ExternalGrammar getExternalGrammar(String path) {
        return _externalGrammarDefiniton.map.get(path);
    }

    /**
     * @return A set of all the external metadata keys
     */
    public Set<String> getExternalMetadataKeys() {
        return _externalGrammarDefiniton.map.keySet();
    }

    /**
     * A simple holder class that stores a type and binary content
     * to be used to return extra metadata with
     */
    public static class ExternalGrammar {

        private MediaType _type;
        private byte[] _content;

        public ExternalGrammar(MediaType type, byte[] content) {
            super();
            this._type = type;
            this._content = content.clone();
        }

        public MediaType getType() {
            return _type;
        }

        public byte[] getContent() {
            // Defensive copy
            return _content.clone();
        }
    }
}
