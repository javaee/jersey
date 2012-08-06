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
package org.glassfish.jersey.moxy.json;

import java.util.Map;

import org.eclipse.persistence.oxm.XMLConstants;

/**
 * Configuration class for MOXy JSON provider.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
@SuppressWarnings("JavaDoc")
public class MoxyJsonConfiguration {

    private String attributePrefix = null;
    private boolean formattedOutput = false;
    private boolean includeRoot = false;
    private boolean marshalEmptyCollections = true;
    private Map<String, String> namespacePrefixMapper;
    private char namespaceSeparator = XMLConstants.DOT;
    private String valueWrapper;

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#getAttributePrefix()
     */
    public String getAttributePrefix() {
        return attributePrefix;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setAttributePrefix(String)
     */
    public void setAttributePrefix(final String attributePrefix) {
        this.attributePrefix = attributePrefix;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#isFormattedOutput()
     */
    public boolean isFormattedOutput() {
        return formattedOutput;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setFormattedOutput(boolean)
     */
    public void setFormattedOutput(final boolean formattedOutput) {
        this.formattedOutput = formattedOutput;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#isIncludeRoot()
     */
    public boolean isIncludeRoot() {
        return includeRoot;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setIncludeRoot(boolean)
     */
    public void setIncludeRoot(final boolean includeRoot) {
        this.includeRoot = includeRoot;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#isMarshalEmptyCollections()
     */
    public boolean isMarshalEmptyCollections() {
        return marshalEmptyCollections;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setMarshalEmptyCollections(boolean)
     */
    public void setMarshalEmptyCollections(final boolean marshalEmptyCollections) {
        this.marshalEmptyCollections = marshalEmptyCollections;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#getNamespacePrefixMapper()
     */
    public Map<String, String> getNamespacePrefixMapper() {
        return namespacePrefixMapper;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setNamespacePrefixMapper(java.util.Map)
     */
    public void setNamespacePrefixMapper(final Map<String, String> namespacePrefixMapper) {
        this.namespacePrefixMapper = namespacePrefixMapper;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#getNamespaceSeparator()
     */
    public char getNamespaceSeparator() {
        return namespaceSeparator;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setNamespaceSeparator(char)
     */
    public void setNamespaceSeparator(final char namespaceSeparator) {
        this.namespaceSeparator = namespaceSeparator;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#getValueWrapper()
     */
    public String getValueWrapper() {
        return valueWrapper;
    }

    /**
     * @see org.eclipse.persistence.jaxb.rs.MOXyJsonProvider#setValueWrapper(String)
     */
    public void setValueWrapper(final String valueWrapper) {
        this.valueWrapper = valueWrapper;
    }
}
