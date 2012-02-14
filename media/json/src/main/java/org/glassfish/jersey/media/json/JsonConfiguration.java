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

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.glassfish.jersey.media.json.internal.LocalizationMessages;

/**
 * An immutable configuration of JSON notation and options. {@code JsonConfiguration}
 * instance can be used for configuring the JSON notation on {@link JsonJaxbContext}.
 *
 * @author Jakub Podlesak
 */
public class JsonConfiguration {

    /**
     * Enumeration of supported JSON notations.
     */
    public enum Notation {

        /**
         * The mapped (default) JSON notation.
         * <p>Example JSON expression:<pre>
         * {"columns":[{"id":"userid","label":"UserID"},{"id":"name","label":"User Name"}],"rows":{"userid":"1621","name":"Grotefend"}}
         * </pre>
         */
        MAPPED,
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
        BADGERFISH,
        /**
         * The natural JSON notation, leveraging closely-coupled JAXB RI integration.
         * <p>Example JSON expression:<pre>
         * {"columns":[{"id":"userid","label":"UserID"},{"id":"name","label":"User Name"}],"rows":[{"userid":1621,"name":"Grotefend"}]}
         * </pre>
         */
        NATURAL
    };
    private final Notation notation;
    private final Collection<String> arrays;
    private final Collection<String> attrsAsElems;
    private final Collection<String> nonStrings;
    private final boolean rootUnwrapping;
    private final boolean humanReadableFormatting;
    private final Map<String, String> jsonXml2JsonNs;
    private final boolean usePrefixAtNaturalAttributes;
    private final Character namespaceSeparator;

    /**
     * Builder class for constructing {@link JsonConfiguration} options
     */
    public static class Builder {

        private final Notation notation;
        protected Collection<String> arrays = new HashSet<String>(0);
        protected Collection<String> attrsAsElems = new HashSet<String>(0);
        protected Collection<String> nonStrings = new HashSet<String>(0);
        protected boolean rootUnwrapping = true;
        protected boolean humanReadableFormatting = false;
        protected Map<String, String> jsonXml2JsonNs = new HashMap<String, String>(0);
        protected boolean usePrefixAtNaturalAttributes = false;
        protected Character namespaceSeparator = '.';

        private Builder(Notation notation) {
            this.notation = notation;
        }

        /**
         * Constructs a new immutable {@link JsonConfiguration} object based on options set on this Builder
         *
         * @return a non-null {@link JsonConfiguration} instance
         */
        public JsonConfiguration build() {
            return new JsonConfiguration(this);
        }

        private void copyAttributes(JsonConfiguration jc) {
            arrays.addAll(jc.getArrays());
            attrsAsElems.addAll(jc.getAttributeAsElements());
            nonStrings.addAll(jc.getNonStrings());
            rootUnwrapping = jc.isRootUnwrapping();
            humanReadableFormatting = jc.isHumanReadableFormatting();
            jsonXml2JsonNs.putAll(jc.getXml2JsonNs());
            usePrefixAtNaturalAttributes = jc.isUsingPrefixesAtNaturalAttributes();
            namespaceSeparator = jc.getNsSeparator();
        }
    }

    /**
     * Builder class for constructing {@link JsonConfiguration} options
     * for the {@link Notation#NATURAL} convention.
     */
    public static class NaturalBuilder extends Builder {

        private NaturalBuilder(Notation notation) {
            super(notation);
        }

        /**
         * Setter for XML root element unwrapping.
         * This property is valid for the {@link JsonConfiguration.Notation#MAPPED}
         * and {@link JsonConfiguration.Notation#NATURAL} notations only.
         * <p>
         * If set to true, JSON code corresponding to the XML root element will be stripped out
         * <p>
         * The default value is false.
         * @param rootUnwrapping if set to true, JSON code corresponding to the
         *        XML root element will be stripped out.
         * @return the natural builder.
         */
        public NaturalBuilder rootUnwrapping(boolean rootUnwrapping) {
            this.rootUnwrapping = rootUnwrapping;
            return this;
        }

        /**
         * If set to true, generated JSON will contain new-line characters and indentation, so that
         * the output is easy to read for people.
         * This property is valid for the  {@link JsonConfiguration.Notation#NATURAL} notation only.
         * <p>
         * The default value is false.
         * @param humanReadableFormatting
         * @return the natural builder.
         */
        public NaturalBuilder humanReadableFormatting(boolean humanReadableFormatting) {
            this.humanReadableFormatting = humanReadableFormatting;
            return this;
        }

        /**
         * JSON names corresponding to XML attributes will be written using a '@' prefix
         * This property is valid for the  {@link JsonConfiguration.Notation#NATURAL} notation only.
         * @return the natural builder.
         */
        public NaturalBuilder usePrefixesAtNaturalAttributes() {
            this.usePrefixAtNaturalAttributes = true;
            return this;
        }
    }

    /**
     * Builder class for constructing {@link JsonConfiguration} options
     * for the {@link Notation#MAPPED_JETTISON} convention.
     */
    public static class MappedJettisonBuilder extends Builder {

        private MappedJettisonBuilder(Notation notation) {
            super(notation);
            rootUnwrapping = false;
        }

        /**
         * Setter for XML to JSON namespace mapping.
         * This property is valid for the {@link JsonConfiguration.Notation#MAPPED_JETTISON}
         * and {@link JsonConfiguration.Notation#MAPPED} notations only.
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
        public MappedJettisonBuilder xml2JsonNs(Map<String, String> jsonXml2JsonNs) {
            this.jsonXml2JsonNs = jsonXml2JsonNs;
            return this;
        }
    }

    /**
     * Builder class for constructing {@link JsonConfiguration} options
     * for the {@link Notation#MAPPED} convention.
     */
    public static class MappedBuilder extends Builder {

        private MappedBuilder(Notation notation) {
            super(notation);
        }

        /**
         * Adds name(s) to JSON arrays configuration property.
         * This property is valid for the {@link JsonConfiguration.Notation#MAPPED} notation only.
         * <p>
         * The property value is a collection of strings representing JSON object names.
         * Those objects will be declared as arrays in  JSON document.
         * <p>
         * For example, consider that the property value is not set and the
         * JSON document is <code>{ ..., "arr1":"single element", ... }</code>.
         * If the property value is set to contain <code>"arr1"</code> then
         * the JSON document would become <code>{ ..., "arr1":["single element"], ... }</code>.
         * <p>
         * The default value is an empty collection.
         * @param arrays an array of strings representing JSON object names.
         * @return the mapped builder.
         */
        public MappedBuilder arrays(String... arrays) {
            this.arrays.addAll(Arrays.asList(arrays));
            return this;
        }

        /**
         * Adds name(s) toJSON attributes as elements property.
         * This property is valid for the {@link JsonConfiguration.Notation#MAPPED} notation only.
         * <p>
         * The value is a collection of string values that are
         * object names that correspond to XML attribute information items.
         * The value of an object name in the JSON document that exists in the collection
         * of object names will be declared as an element as not as an attribute if
         * the object corresponds to an XML attribute information item.
         * <p>
         * For example, consider that the property value is not set and the
         * JSON document is <code>{ ..., "@number":"12", ... }</code>.
         * If the property value is set to contain <code>"number"</code>
         * then the JSON document would be <code>{ ..., "number":"12", ... }</code>.
         * <p>
         * The default value is an empty collection.
         * @param attributeAsElements an array of string values that are
         *        object names that correspond to XML attribute information items.
         * @return the mapped builder.
         */
        public MappedBuilder attributeAsElement(String... attributeAsElements) {
            this.attrsAsElems.addAll(Arrays.asList(attributeAsElements));
            return this;
        }

        /**
         * Adds name(s) JSON non-string values property.
         * This property is valid for the {@link JsonConfiguration.Notation#MAPPED} notation only.
         * <p>
         * The value is collection of string values that are
         * object names.
         * The value of an object name in the JSON document that exists in the collection
         * of object names will be declared as non-string value, which is not surrounded
         * by double quotes.
         * <p>
         * For example, consider that the property value is not set and the
         * JSON document is <code>{ ..., "anumber":"12", ... }</code>.
         * If the property value is set to contain <code>"anumber"</code>
         * then the JSON document would be <code>{ ..., "anumber":12, ... }</code>.
         * <p>
         * The default value is an empty collection.
         * @param nonStrings an array of string values that are
         * object names
         * @return the mapped builder.
         */
        public MappedBuilder nonStrings(String... nonStrings) {
            this.nonStrings.addAll(Arrays.asList(nonStrings));
            return this;
        }

        /**
         * Setter for XML to JSON namespace mapping.
         * This property is valid for the {@link JsonConfiguration.Notation#MAPPED_JETTISON}
         * and {@link JsonConfiguration.Notation#MAPPED} notations only.
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
        public MappedBuilder xml2JsonNs(Map<String, String> jsonXml2JsonNs) {
            this.jsonXml2JsonNs = jsonXml2JsonNs;
            return this;
        }

        /**
         * Setter for XML namespace separator.
         * This property is valid for the {@link JsonConfiguration.Notation#MAPPED} notation only.
         * <p>
         * The value is a character used to separate a namespace identifier from the name
         *  of a XML attribute/element in JSON.
         * <p>
         * The default value is dot character ('.').
         *
         * @param separator namespace separator character.
         * @return updated builder instance.
         */
        public MappedBuilder nsSeparator(Character separator) {
            if (separator == null) {
                throw new NullPointerException("Namespace separator can not be null!");
            }
            this.namespaceSeparator = separator;
            return this;
        }

        /**
         * Setter for XML root element unwrapping.
         * This property is valid for the {@link JsonConfiguration.Notation#MAPPED}
         * and {@link JsonConfiguration.Notation#NATURAL} notations only.
         * <p>
         * If set to true, JSON code corresponding to the XML root element will be stripped out
         * <p>
         * The default value is false.
         * @param rootUnwrapping if set to true, JSON code corresponding to the
         *        XML root element will be stripped out.
         * @return the mapped builder.
         */
        public MappedBuilder rootUnwrapping(boolean rootUnwrapping) {
            this.rootUnwrapping = rootUnwrapping;
            return this;
        }
    }

    private JsonConfiguration(Builder b) {
        notation = b.notation;
        arrays = b.arrays;
        attrsAsElems = b.attrsAsElems;
        nonStrings = b.nonStrings;
        rootUnwrapping = b.rootUnwrapping;
        humanReadableFormatting = b.humanReadableFormatting;
        jsonXml2JsonNs = b.jsonXml2JsonNs;
        usePrefixAtNaturalAttributes = b.usePrefixAtNaturalAttributes;
        namespaceSeparator = b.namespaceSeparator;
    }

    /**
     * A static method for obtaining {@link JsonConfiguration} instance with humanReadableFormatting
     * set according to formatted parameter.
     *
     * @param c original instance of {@link JsonConfiguration}, can't be null
     * @param formatted
     * @return copy of provided {@link JsonConfiguration} with humanReadableFormatting set to formatted.
     * @throws IllegalArgumentException when provided JsonConfiguration is null.
     */
    public static JsonConfiguration createJSONConfigurationWithFormatted(JsonConfiguration c, boolean formatted) throws IllegalArgumentException {

        if (c == null) {
            throw new IllegalArgumentException("JSONConfiguration can't be null");
        }

        if (c.isHumanReadableFormatting() == formatted) {
            return c;
        }

        Builder b = copyBuilder(c);
        b.humanReadableFormatting = formatted;

        return b.build();
    }

    /**
     * A static method for obtaining {@link JsonConfiguration} instance with rootUnwrapping
     * set according to formatted parameter.
     *
     * @param c original instance of {@link JsonConfiguration}, can't be null
     * @param rootUnwrapping
     * @return copy of provided {@link JsonConfiguration} with humanReadableFormatting set to formatted.
     * @throws IllegalArgumentException when provided JsonConfiguration is null.
     */
    public static JsonConfiguration createJSONConfigurationWithRootUnwrapping(JsonConfiguration c, boolean rootUnwrapping) throws IllegalArgumentException {

        if (c == null) {
            throw new IllegalArgumentException("JSONConfiguration can't be null");
        }

        if (c.isRootUnwrapping() == rootUnwrapping) {
            return c;
        }

        Builder b = copyBuilder(c);
        b.rootUnwrapping = rootUnwrapping;

        return b.build();
    }
    /**
     * The default JsonConfiguration uses {@link JsonConfiguration.Notation#MAPPED} notation with root unwrapping option set to true.
     */
    public static final JsonConfiguration DEFAULT = mapped().rootUnwrapping(true).build();

    /**
     * A static method for obtaining a builder of {@link JsonConfiguration} instance, which will use {@link Notation#NATURAL} JSON notation.
     * After getting the builder, you can set configuration options on it, and finally get an immutable JsonConfiguration
     * instance using the {@link Builder#build() } method.
     *
     * @return a builder for JsonConfiguration instance
     */
    public static NaturalBuilder natural() {
        // this is to make sure people trying to use NATURAL notation will get clear message what is missing, when an old JAXB RI version is used
        try {
            Class.forName("com.sun.xml.bind.annotation.OverrideAnnotationOf");
        } catch (ClassNotFoundException ex) {
            Logger.getLogger(JsonConfiguration.class.getName()).log(Level.SEVERE, LocalizationMessages.ERROR_JAXB_RI_2_1_10_MISSING());
            throw new RuntimeException(LocalizationMessages.ERROR_JAXB_RI_2_1_10_MISSING());
        }
        return new NaturalBuilder(Notation.NATURAL);
    }

    /**
     * A static method for obtaining a builder of {@link JsonConfiguration} instance, which will use {@link Notation#MAPPED} JSON notation.
     * After getting the builder, you can set configuration options on it and finally get an immutable  JsonConfiguration
     * instance the using {@link Builder#build() } method.
     *
     * @return a builder for JsonConfiguration instance
     */
    public static MappedBuilder mapped() {
        return new MappedBuilder(Notation.MAPPED);
    }

    /**
     * A static method for obtaining a builder of {@link JsonConfiguration} instance, which will use {@link Notation#MAPPED_JETTISON} JSON notation.
     * After getting the builder, you can set configuration options on it and finally get an immutable  JsonConfiguration
     * instance using the {@link Builder#build() } method.
     *
     * @return a builder for JsonConfiguration instance
     */
    public static MappedJettisonBuilder mappedJettison() {
        return new MappedJettisonBuilder(Notation.MAPPED_JETTISON);
    }

    /**
     * A static method for obtaining a builder of {@link JsonConfiguration} instance, which will use {@link Notation#BADGERFISH} JSON notation.
     * After getting the builder, you can set configuration options on it and finally get an immutable  JsonConfiguration
     * instance using the {@link Builder#build() } method.
     *
     * @return a builder for JsonConfiguration instance
     */
    public static Builder badgerFish() {
        Builder badgerFishBuilder = new Builder(Notation.BADGERFISH);
        badgerFishBuilder.rootUnwrapping = false;
        return badgerFishBuilder;
    }

    public static Builder copyBuilder(final JsonConfiguration jc) {

        Builder result = new Builder(jc.getNotation());

        switch (jc.notation) {
            case BADGERFISH:
                result = new Builder(jc.getNotation());
                break;
            case MAPPED_JETTISON:
                result = new MappedJettisonBuilder(jc.getNotation());
                break;
            case MAPPED:
                result = new MappedBuilder(jc.getNotation());
                break;
            case NATURAL:
                result = new NaturalBuilder(jc.getNotation());
                break;
        }

        result.copyAttributes(jc);

        return result;
    }

    /**
     * Returns JSON notation selected for this configuration
     * @return JSON notation
     */
    public Notation getNotation() {
        return notation;
    }

    /**
     * Returns JSON array names property
     * This property is valid for the {@link JsonConfiguration.Notation#MAPPED} notation only.
     * @return collection of array names
     * @see MappedBuilder#arrays(java.lang.String...)
     */
    public Collection<String> getArrays() {
        return (arrays != null) ? Collections.unmodifiableCollection(arrays) : null;
    }

    /**
     * Returns names of attributes, which will be handled as elements
     * This property is valid for the {@link JsonConfiguration.Notation#MAPPED} notation only.
     * @return attribute as element names collection
     * @see MappedBuilder#attributeAsElement(java.lang.String...)
     */
    public Collection<String> getAttributeAsElements() {
        return (attrsAsElems != null) ? Collections.unmodifiableCollection(attrsAsElems) : null;
    }

    /**
     * Returns a map for XML to JSON namespace mapping
     * This property is valid for the {@link JsonConfiguration.Notation#MAPPED} notation only.
     * @return a map for XML to JSON namespace mapping
     * @see MappedBuilder#xml2JsonNs(java.util.Map)
     */
    public Map<String, String> getXml2JsonNs() {
        return (jsonXml2JsonNs != null) ? Collections.unmodifiableMap(jsonXml2JsonNs) : null;
    }

    /**
     * Returns XML namespace separator, which is used when constructing JSON identifiers
     * for XML elements/attributes in other than default namespace
     * This property is valid for the {@link JsonConfiguration.Notation#MAPPED} notation only.
     * @return XML namespace separator character
     * @see MappedBuilder#nsSeparator(java.lang.Character)
     */
    public Character getNsSeparator() {
        return namespaceSeparator;
    }

    /**
     * Returns names of JSON objects, which will be serialized out as non-strings, i.e. without delimiting their values with double quotes
     * This property is valid for the {@link JsonConfiguration.Notation#MAPPED} notation only.
     * @return name of non-string JSON objects
     * @see MappedBuilder#nonStrings(java.lang.String...)
     */
    public Collection<String> getNonStrings() {
        return (nonStrings != null) ? Collections.unmodifiableCollection(nonStrings) : null;
    }

    /**
     * Says if the root element will be stripped off
     * This property is valid for the {@link JsonConfiguration.Notation#MAPPED}
     * and {@link Notation#NATURAL} notations.
     * @return true, if root element has to be stripped off
     * @see MappedBuilder#rootUnwrapping(boolean)
     */
    public boolean isRootUnwrapping() {
        return rootUnwrapping;
    }

    /**
     * Says if the JSON names corresponding to XML attributes should use a '@' prefix.
     * This property is valid for the {@link JsonConfiguration.Notation#NATURAL} notation only.
     * @return true, if prefixes are added
     * @see NaturalBuilder#usePrefixesAtNaturalAttributes()
     */
    public boolean isUsingPrefixesAtNaturalAttributes() {
        return usePrefixAtNaturalAttributes;
    }

    /**
     * Says if the output JSON will be formatted with new-line characters
     * and indentation so that it is easy to read for people.
     * This property is valid for the {@link JsonConfiguration.Notation#NATURAL} notation only.
     * @return true, if formatting is applied on the output JSON
     * @see NaturalBuilder#humanReadableFormatting(boolean)
     */
    public boolean isHumanReadableFormatting() {
        return humanReadableFormatting;
    }

    @Override
    public String toString() {
        return String.format("{notation:%s,rootStripping:%b}", notation, rootUnwrapping);
    }
}
