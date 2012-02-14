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
package org.glassfish.jersey.media.json.internal.reader;

import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;
import javax.xml.namespace.QName;
import javax.xml.stream.Location;
import javax.xml.stream.XMLStreamException;

/**
 * JSON parser event.
 *
 * @author Jakub Podlesak
 */
public abstract class JsonReaderXmlEvent {

    public static class Attribute {

        QName name;
        String value;

        public Attribute(QName name, String value) {
            this.name = name;
            this.value = value;
        }
    }
    Location location;
    QName name;
    String text;
    List<Attribute> attributes;
    boolean attributesChecked;

    public abstract int getEventType();

    public boolean isAttribute() {
        return false;
    }

    public boolean isCharacters() {
        return false;
    }

    public boolean isEndDocument() {
        return false;
    }

    public boolean isEndElement() {
        return false;
    }

    public boolean isEntityReference() {
        return false;
    }

    public boolean isNamespace() {
        return false;
    }

    public boolean isProcessingInstruction() {
        return false;
    }

    public boolean isStartDocument() {
        return false;
    }

    public boolean isStartElement() {
        return false;
    }

    public int getAttributeCount() {
        return (null != attributes) ? attributes.size() : 0;
    }

    public String getAttributeLocalName(int index) {
        return getAttributeName(index).getLocalPart();
    }

    public QName getAttributeName(int index) {
        if ((null == attributes) || (index >= attributes.size())) {
            throw new IndexOutOfBoundsException();
        }
        return attributes.get(index).name;
    }

    public String getAttributePrefix(int index) {
        return getAttributeName(index).getPrefix();
    }

    public String getAttributeType(int index) {
        return null;
    }

    public String getAttributeNamespace(int index) {
        return getAttributeName(index).getNamespaceURI();
    }

    public String getAttributeValue(int index) {
        if ((null == attributes) || (index >= attributes.size())) {
            throw new IndexOutOfBoundsException();
        }
        return attributes.get(index).value;
    }

    public String getAttributeValue(String namespaceURI, String localName) {
        if ((null == attributes) || (null == localName) || ("".equals(localName))) {
            throw new NoSuchElementException();
        }
        final QName askedFor = new QName(namespaceURI, localName);
        for (Attribute a : attributes) {
            if (askedFor.equals(a.name)) {
                return a.value;
            }
        }
        throw new NoSuchElementException();
    }

    public boolean isAttributeSpecified(int index) {
        return (null != attributes) && (attributes.size() >= index);
    }

    public String getText() {
        if (null != text) {
            return text;
        } else {
            throw new IllegalStateException();
        }
    }

    public char[] getTextCharacters() {
        if (null != text) {
            return text.toCharArray();
        } else {
            throw new IllegalStateException();
        }
    }

    public int getTextCharacters(int sourceStart, char[] target, int targetStart, int length) throws XMLStreamException {
        if (null != text) {
            System.arraycopy(text.toCharArray(), sourceStart, target, targetStart, length);
            return length;
        } else {
            throw new IllegalStateException();
        }
    }

    public int getTextStart() {
        if (null != text) {
            return 0;
        } else {
            throw new IllegalStateException();
        }
    }

    public int getTextLength() {
        if (null != text) {
            return text.length();
        } else {
            throw new IllegalStateException();
        }
    }

    public boolean hasName() {
        return null != name;
    }

    public QName getName() {
        if (null != name) {
            return name;
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public String getLocalName() {
        if (null != name) {
            return name.getLocalPart();
        } else {
            throw new UnsupportedOperationException();
        }
    }

    public String getPrefix() {
        if (null != name) {
            return name.getPrefix();
        } else {
            return null;
        }
    }

    public Location getLocation() {
        return location;
    }

    public void addAttribute(QName name, String value) {
        if (null == attributes) {
            attributes = new LinkedList<JsonReaderXmlEvent.Attribute>();
        }
        attributes.add(new Attribute(name, value));
    }
}
