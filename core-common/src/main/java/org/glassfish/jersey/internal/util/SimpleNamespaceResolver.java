/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal.util;

import java.util.Iterator;

import javax.xml.XMLConstants;
import javax.xml.namespace.NamespaceContext;

/**
 * Simple namespace resolver which resolves one predefined namespace.
 *
 * @author Gerard Davison
 * @author Miroslav Fuksa
 */
public class SimpleNamespaceResolver implements NamespaceContext {
    private final String prefix;
    private final String nsURI;

    /**
     * Create a new instance of the namespace resolver initialized with the
     * fixed {@code prefix} and {@code URI} that the resolver will be capable to resolve.
     *
     * @param prefix Namespace prefix.
     * @param nsURI Namespace URI.
     */
    public SimpleNamespaceResolver(String prefix, String nsURI) {
        this.prefix = prefix;
        this.nsURI = nsURI;
    }

    @Override
    public String getNamespaceURI(String prefix) {
        if (prefix.equals(this.prefix)) {
            return this.nsURI;
        } else {
            return XMLConstants.NULL_NS_URI;
        }
    }

    @Override
    public String getPrefix(String namespaceURI) {
        if (namespaceURI.equals(this.nsURI)) {
            return this.prefix;
        } else {
            return null;
        }
    }

    @Override
    public Iterator getPrefixes(String namespaceURI) {
        return null;
    }
}
