/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.jettison;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Set;


/**
 * An immutable configuration of JSON notation and options. {@code JettisonConfig}
 * instance can be used for configuring the JSON notation on {@link JettisonJaxbContext}.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class JettisonConfig {

    /**
     * Enumeration of supported JSON notations.
     */
    public enum Notation {
        /**
         * The mapped Jettison JSON notation.
         * <p>Example JSON expression:<pre>
         * {"userTable":{"columns":[{"id":"userid","label":"UserID"},{"id":"name","label":"User Name"}],"rows":{"userid":1621,"name":"Grotefend"}}}
         * </pre>
         */
        MAPPED_JETTISON,
        /**
         * The mapped Badgerfish JSON notation.
         * <p>Example JSON expression:<pre>
         * {"userTable":{"columns":[{"id":{"$":"userid"},"label":{"$":"UserID"}},{"id":{"$":"name"},"label":{"$":"User Name"}}],"rows":{"userid":{"$":"1621"},"name":{"$":"Grotefend"}}}}
         * </pre>
         */
        BADGERFISH
    }

    private final Notation notation;

    /**
     * Builder class for constructing {@link JettisonConfig} options
     */
    public abstract static class Builder {

        private final Notation notation;


        private Builder(final Notation notation) {
            this.notation = notation;
        }

        /**
         * Constructs a new immutable {@link JettisonConfig} object based on options set on this Builder
         *
         * @return a non-null {@link JettisonConfig} instance
         */
        public JettisonConfig build() {
            if (this instanceof MappedJettisonBuilder) {
                return new JettisonConfig.Mapped((MappedJettisonBuilder) this);
            }
            if (this instanceof BadgerfishBuilder) {
                return new JettisonConfig.Badgerfish((BadgerfishBuilder) this);
            }
            throw new RuntimeException(this.getClass().toString());
        }

    }

    /**
     * Builder class for constructing {@link JettisonConfig} options
     * for the {@link JettisonConfig.Notation#MAPPED_JETTISON} convention.
     */
    public static class MappedJettisonBuilder extends Builder {

        private List<String> serializeAsArray;
        private Map<String, String> xmlToJsonNamespaces;
        private List attributesAsElements;
        private List ignoredElements;
        private boolean supressAtAttributes;
        private String attributeKey;
        private boolean ignoreNamespaces;
        private boolean dropRootElement;
        private Set primitiveArrayKeys;
        private boolean writeNullAsString;
        private boolean readNullAsString;
        private boolean ignoreEmptyArrayValues;
        private boolean escapeForwardSlashAlways;
        private String jsonNamespaceSeparator;

        private MappedJettisonBuilder() {
            super(Notation.MAPPED_JETTISON);
            copyAttributes(defaultInstance);
        }

        private static final JettisonConfig.Mapped defaultInstance = new Mapped();



        private void copyAttributes(final JettisonConfig.Mapped jc) {
            xmlToJsonNamespaces = new HashMap<>(jc.getXml2JsonNs());
            serializeAsArray = new LinkedList<>(jc.getArrayElements());
            attributeKey = jc.getAttributeKey();
            attributesAsElements = new LinkedList<>(jc.getAttributesAsElements());
            dropRootElement = jc.isDropRootElement();
            escapeForwardSlashAlways = jc.isEscapeForwardSlashAlways();
            ignoredElements = new LinkedList<>(jc.getIgnoredElements());
            ignoreEmptyArrayValues = jc.isIgnoreEmptyArrayValues();
            ignoreNamespaces = jc.isIgnoreNamespaces();
            jsonNamespaceSeparator = jc.getJsonNamespaceSeparator();
            primitiveArrayKeys = new HashSet<>(jc.getPrimitiveArrayKeys());
            readNullAsString = jc.isReadNullAsString();
            supressAtAttributes = jc.supressAtAttributes;
            writeNullAsString = jc.writeNullAsString;

        }

        /**
         * Setter for XML to JSON namespace mapping.
         * This property is valid for the {@link JettisonConfig.Notation#MAPPED_JETTISON}
         * notation only.
         * <p>
         * The value is a map with zero or more
         * key/value pairs, where the key is an XML namespace and the value
         * is the prefix to use as the replacement for the XML namespace.
         * <p>
         * The default value is a map with zero key/value pairs.
         *
         * @param jsonXml2JsonNs XML to JSON namespace map.
         * @return updated builder instance.
         */
        public MappedJettisonBuilder xml2JsonNs(final Map<String, String> jsonXml2JsonNs) {
            xmlToJsonNamespaces = jsonXml2JsonNs;
            return this;
        }

        /**
         * Add element names to be treated as arrays.
         * This property is valid for the {@link JettisonConfig.Notation#MAPPED_JETTISON}
         * notation only.
         * <p>
         * Property value is a list of element names that should be treated
         * as arrays even if only a single item is present.
         * <p>
         * The default value is an empty list.
         *
         * @param arrays names to be serialized as arrays.
         * @return updated builder instance.
         */
        public MappedJettisonBuilder serializeAsArray(final String... arrays) {
            return serializeAsArray(Arrays.asList(arrays));
        }

        /**
         * Add element names to be treated as arrays.
         * This property is valid for the {@link JettisonConfig.Notation#MAPPED_JETTISON}
         * notation only.
         * <p>
         * Property value is a list of element names that should be treated
         * as arrays even if only a single item is present.
         * <p>
         * The default value is an empty list.
         *
         * @param arrays of element names to be serialized as arrays.
         * @return updated builder instance.
         */
        public MappedJettisonBuilder serializeAsArray(final List<String> arrays) {
            serializeAsArray = new LinkedList<>(arrays);
            return this;
        }
           public MappedJettisonBuilder setIgnoreNamespaces(boolean ignoreNamespaces) {
              this.ignoreNamespaces = ignoreNamespaces;
             return this;
         }
         public MappedJettisonBuilder setAttributesAsElements(List attributesAsElements) {
             this.attributesAsElements = attributesAsElements;
            return this;
         }
          public MappedJettisonBuilder setIgnoredElements(List ignoredElements) {
             this.ignoredElements = ignoredElements;
              return this;
          }
        public MappedJettisonBuilder setXmlToJsonNamespaces(Map xmlToJsonNamespaces) {
            this.xmlToJsonNamespaces = xmlToJsonNamespaces;
            return this;
        }
        public MappedJettisonBuilder setSupressAtAttributes(boolean supressAtAttributes) {
            this.supressAtAttributes = supressAtAttributes;
            return this;
        }
         public MappedJettisonBuilder setAttributeKey(String attributeKey) {
             this.attributeKey = attributeKey;
            return this;
         }
          public MappedJettisonBuilder setPrimitiveArrayKeys(Set primitiveArrayKeys) {
              this.primitiveArrayKeys = primitiveArrayKeys;
              return this;
          }
        public MappedJettisonBuilder setDropRootElement(boolean dropRootElement) {
            this.dropRootElement = dropRootElement;
            return this;
        }
          public MappedJettisonBuilder setWriteNullAsString(boolean writeNullAsString) {
            this.writeNullAsString = writeNullAsString;
            return this;
        }
        public MappedJettisonBuilder setReadNullAsString(boolean readNullString) {
            this.readNullAsString = readNullString;
            return this;
        }
        public MappedJettisonBuilder setIgnoreEmptyArrayValues(boolean ignoreEmptyArrayValues) {
            this.ignoreEmptyArrayValues = ignoreEmptyArrayValues;
            return this;
        }
        public MappedJettisonBuilder setEscapeForwardSlashAlways(boolean escapeForwardSlash) {
            this.escapeForwardSlashAlways = escapeForwardSlash;
            return this;
        }
         public MappedJettisonBuilder setJsonNamespaceSeparator(String jsonNamespaceSeparator) {
            this.jsonNamespaceSeparator = jsonNamespaceSeparator;
            return this;
        }
    }

    /**
     * Builder class for constructing {@link JettisonConfig} options
     * for the {@link JettisonConfig.Notation#BADGERFISH} convention.
     */
    public static class BadgerfishBuilder extends Builder {

        private static final JettisonConfig.Badgerfish defaultInstance = new Badgerfish();

          private BadgerfishBuilder() {
              super(Notation.BADGERFISH);
              copyAttributes(defaultInstance);
          }


        public void copyAttributes(Badgerfish jc) {

        }
    }

    protected JettisonConfig(Notation notation) {
        this.notation = notation;
    }

    /**
     * A static method for obtaining {@link JettisonConfig} instance with humanReadableFormatting
     * set according to formatted parameter.
     *
     * @param c original instance of {@link JettisonConfig}, can't be null
     * @return copy of provided {@link JettisonConfig} with humanReadableFormatting set to formatted.
     * @throws IllegalArgumentException when provided {@code JettisonConfig} is null.
     */
    public static JettisonConfig createJSONConfiguration(final JettisonConfig c) throws IllegalArgumentException {
        if (c == null) {
            throw new IllegalArgumentException("JettisonConfig can't be null");
        }

        final Builder b = copyBuilder(c);

        return b.build();
    }

    /**
     * The default {@code JettisonConfig} uses {@link JettisonConfig.Notation#MAPPED_JETTISON}
     * notation with root unwrapping option set to true.
     */
    public static final JettisonConfig DEFAULT = mappedJettison().build();

    /**
     * A static method for obtaining a builder of {@link JettisonConfig} instance, which will use {@link JettisonConfig.Notation#MAPPED_JETTISON} JSON notation.
     * After getting the builder, you can set configuration options on it and finally get an immutable {@code JettisonConfig}
     * instance using the {@link JettisonConfig.Builder#build() } method.
     *
     * @return a builder for {@code JettisonConfig} instance
     */
    public static MappedJettisonBuilder mappedJettison() {
        return new MappedJettisonBuilder();
    }

    /**
     * A static method for obtaining a builder of {@link JettisonConfig} instance, which will use {@link JettisonConfig.Notation#BADGERFISH} JSON notation.
     * After getting the builder, you can set configuration options on it and finally get an immutable {@code JettisonConfig}
     * instance using the {@link JettisonConfig.Builder#build() } method.
     *
     * @return a builder for {@code JettisonConfig} instance
     */
    public static BadgerfishBuilder badgerFish() {
        return new BadgerfishBuilder();
    }

    public static Builder copyBuilder(final JettisonConfig jc) {

        Builder result = null;

        switch (jc.notation) {
            case BADGERFISH:
                result = new BadgerfishBuilder();
                ((BadgerfishBuilder) result).copyAttributes((JettisonConfig.Badgerfish) jc);
                break;
            case MAPPED_JETTISON:
                result = new MappedJettisonBuilder();
                ((MappedJettisonBuilder) result).copyAttributes((JettisonConfig.Mapped) jc);
                break;
            default:
                throw new RuntimeException("Unsupported notation " + jc.notation.toString());
        }
        return result;
    }

    /**
     * Returns JSON notation selected for this configuration
     * @return JSON notation
     */
    public Notation getNotation() {
        return notation;
    }

    @Override
    public String toString() {
        return String.format("{notation:%s}", notation);
    }


     public static class Mapped extends JettisonConfig {
            private final List<String> serializeAsArray;
            private final Map<String, String> xmlToJsonNamespaces;
            private final List attributesAsElements;
            private final List ignoredElements;
            private final boolean supressAtAttributes;
            private final String attributeKey;
            private final boolean ignoreNamespaces;
            private final boolean dropRootElement;
            private final Set primitiveArrayKeys;
            private final boolean writeNullAsString;
            private final boolean readNullAsString;
            private final boolean ignoreEmptyArrayValues;
            private final boolean escapeForwardSlashAlways;
            private final String jsonNamespaceSeparator;


            public Mapped() {
                this(null);
            }

            public Mapped(MappedJettisonBuilder mappedJettisonBuilder) {
                super(Notation.MAPPED_JETTISON);
                List<String> serializeAsArray = Collections.EMPTY_LIST;
                Map<String, String> xmlToJsonNamespaces = Collections.EMPTY_MAP;
                List attributesAsElements = Collections.EMPTY_LIST;
                List ignoredElements = Collections.EMPTY_LIST;;
                boolean supressAtAttributes = false;
                String attributeKey = "@";
                boolean ignoreNamespaces = false;
                boolean dropRootElement = false;
                Set primitiveArrayKeys = Collections.EMPTY_SET;
                boolean writeNullAsString = true;
                boolean readNullAsString = false;
                boolean ignoreEmptyArrayValues = false;
                boolean escapeForwardSlashAlways = false;
                String jsonNamespaceSeparator = null;
                  if (mappedJettisonBuilder != null) {
                      serializeAsArray = mappedJettisonBuilder.serializeAsArray;
                      xmlToJsonNamespaces = mappedJettisonBuilder.xmlToJsonNamespaces;
                      attributesAsElements = mappedJettisonBuilder.attributesAsElements;
                      ignoredElements = mappedJettisonBuilder.ignoredElements;
                      supressAtAttributes = mappedJettisonBuilder.supressAtAttributes;
                      attributeKey = mappedJettisonBuilder.attributeKey;
                      ignoreNamespaces = mappedJettisonBuilder.ignoreNamespaces;
                      dropRootElement = mappedJettisonBuilder.dropRootElement;
                      primitiveArrayKeys = mappedJettisonBuilder.primitiveArrayKeys;
                      writeNullAsString = mappedJettisonBuilder.writeNullAsString;
                      readNullAsString = mappedJettisonBuilder.readNullAsString;
                      ignoreEmptyArrayValues = mappedJettisonBuilder.ignoreEmptyArrayValues;
                      escapeForwardSlashAlways = mappedJettisonBuilder.escapeForwardSlashAlways;
                      jsonNamespaceSeparator = mappedJettisonBuilder.jsonNamespaceSeparator;
                  }
                  this.serializeAsArray = serializeAsArray;
                  this.xmlToJsonNamespaces = xmlToJsonNamespaces;
                  this.attributesAsElements = attributesAsElements;
                  this.ignoredElements = ignoredElements;
                  this.supressAtAttributes = supressAtAttributes;
                  this.attributeKey = attributeKey;
                  this.ignoreNamespaces = ignoreNamespaces;
                  this.dropRootElement = dropRootElement;
                  this.primitiveArrayKeys = primitiveArrayKeys;
                  this.writeNullAsString = writeNullAsString;
                  this.readNullAsString = readNullAsString;
                  this.ignoreEmptyArrayValues = ignoreEmptyArrayValues;
                  this.escapeForwardSlashAlways = escapeForwardSlashAlways;
                  this.jsonNamespaceSeparator = jsonNamespaceSeparator;

            }

            /**
            * Returns a list of elements to be treated as arrays. I.e. these elements will be serialized
            * as arrays even if only a single element is included.
            * This property is valid for the {@link JettisonConfig.Notation#MAPPED_JETTISON}
            * notation only.
            * @return a list of elements representing arrays.
            * @see JettisonConfig.MappedJettisonBuilder#serializeAsArray(java.util.List)
            */
           public List<String> getArrayElements() {
                return Collections.unmodifiableList(serializeAsArray);
           }

            /**
             * Returns a map for XML to JSON namespace mapping
             * This property is valid for the {@link JettisonConfig.Notation#MAPPED_JETTISON}
             * notation only.
             * @return a map for XML to JSON namespace mapping.
             * @see JettisonConfig.MappedJettisonBuilder#xml2JsonNs(java.util.Map)
             */
           public Map<String, String> getXml2JsonNs() {
//                return (jsonXml2JsonNs != null) ? Collections.unmodifiableMap(jsonXml2JsonNs) : null;
               return (xmlToJsonNamespaces != null) ? Collections.unmodifiableMap(xmlToJsonNamespaces) : null;
           }
           public boolean isIgnoreNamespaces() {
                return ignoreNamespaces;
           }
            public List getAttributesAsElements() {
                return attributesAsElements;
            }
            public List getIgnoredElements() {
                return ignoredElements;
            }

            public Map getXmlToJsonNamespaces() {
                return xmlToJsonNamespaces;
            }


            public boolean isSupressAtAttributes() {
                return this.supressAtAttributes;
            }

           public String getAttributeKey() {
                return this.attributeKey;
            }



            public Set getPrimitiveArrayKeys() {
                return primitiveArrayKeys;
            }

            public boolean isDropRootElement() {
                return dropRootElement;
            }

            public boolean isWriteNullAsString() {
                return writeNullAsString;
            }
            public boolean isReadNullAsString() {
                return readNullAsString;
            }
            public boolean isIgnoreEmptyArrayValues() {
                return ignoreEmptyArrayValues;
            }

            public boolean isEscapeForwardSlashAlways() {
                return escapeForwardSlashAlways;
            }
            public String getJsonNamespaceSeparator() {
                return jsonNamespaceSeparator;
            }
    }
    public static class Badgerfish extends JettisonConfig {

        protected Badgerfish() {
            this(null);
        }

        public Badgerfish(BadgerfishBuilder badgerfishBuilder) {
            super(Notation.BADGERFISH);
        }

    }
}
