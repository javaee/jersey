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
package org.glassfish.jersey.wadl.doclet;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.io.StringWriter;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.Marshaller;
import javax.xml.namespace.QName;

import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.AnnotationDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ClassDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.MethodDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.NamedValueType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ParamDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.RepresentationDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.RequestDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ResourceDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ResponseDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.WadlParamType;

import org.apache.xml.serialize.OutputFormat;
import org.apache.xml.serialize.XMLSerializer;

import com.sun.javadoc.AnnotationDesc;
import com.sun.javadoc.AnnotationDesc.ElementValuePair;
import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.DocErrorReporter;
import com.sun.javadoc.MemberDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;
import com.sun.javadoc.RootDoc;
import com.sun.javadoc.SeeTag;
import com.sun.javadoc.Tag;

/**
 * Creates a resourcedoc XML file.
 * <p/>
 * <p>
 * The ResourceDoc file contains the javadoc documentation
 * of resource classes, so that this can be used for extending generated wadl with useful
 * documentation.
 * </p>
 *
 * @author <a href="mailto:martin.grotzke@freiheit.com">Martin Grotzke</a>
 */
public class ResourceDoclet {

    private static final Pattern PATTERN_RESPONSE_REPRESENTATION = Pattern.compile("@response\\.representation\\.([\\d]+)\\..*");
    private static final String OPTION_OUTPUT = "-output";
    private static final String OPTION_CLASSPATH = "-classpath";
    private static final String OPTION_DOC_PROCESSORS = "-processors";

    private static final Logger LOG = Logger.getLogger(ResourceDoclet.class.getName());

    /**
     * Start the doclet.
     *
     * @param root the root JavaDoc document.
     * @return true if no exception is thrown.
     */
    public static boolean start(final RootDoc root) {
        final String output = getOptionArg(root.options(), OPTION_OUTPUT);

        final String classpath = getOptionArg(root.options(), OPTION_CLASSPATH);
        // LOG.info( "Have classpath: " + classpath );
        final String[] classpathElements = classpath.split(File.pathSeparator);

        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        final ClassLoader ncl = new Loader(classpathElements,
                ResourceDoclet.class.getClassLoader());
        Thread.currentThread().setContextClassLoader(ncl);

        final String docProcessorOption = getOptionArg(root.options(), OPTION_DOC_PROCESSORS);
        final String[] docProcessors = docProcessorOption != null ? docProcessorOption.split(":") : null;
        final DocProcessorWrapper docProcessor = new DocProcessorWrapper();
        try {
            if (docProcessors != null && docProcessors.length > 0) {
                final Class<?> clazz = Class.forName(docProcessors[0], true, Thread.currentThread().getContextClassLoader());
                final Class<? extends DocProcessor> dpClazz = clazz.asSubclass(DocProcessor.class);
                docProcessor.add(dpClazz.newInstance());
            }
        } catch (final Exception e) {
            LOG.log(Level.SEVERE, "Could not load docProcessors " + docProcessorOption, e);
        }

        try {
            final ResourceDocType result = new ResourceDocType();

            final ClassDoc[] classes = root.classes();
            for (final ClassDoc classDoc : classes) {
                LOG.fine("Writing class " + classDoc.qualifiedTypeName());
                final ClassDocType classDocType = new ClassDocType();
                classDocType.setClassName(classDoc.qualifiedTypeName());
                classDocType.setCommentText(classDoc.commentText());
                docProcessor.processClassDoc(classDoc, classDocType);

                for (final MethodDoc methodDoc : classDoc.methods()) {

                    final MethodDocType methodDocType = new MethodDocType();
                    methodDocType.setMethodName(methodDoc.name());
                    methodDocType.setMethodSignature(methodDoc.signature());
                    methodDocType.setCommentText(methodDoc.commentText());
                    docProcessor.processMethodDoc(methodDoc, methodDocType);

                    addParamDocs(methodDoc, methodDocType, docProcessor);

                    addRequestRepresentationDoc(methodDoc, methodDocType);

                    addResponseDoc(methodDoc, methodDocType);

                    classDocType.getMethodDocs().add(methodDocType);
                }

                result.getDocs().add(classDocType);
            }

            try {
                final Class<?>[] clazzes = getJAXBContextClasses(result, docProcessor);
                final JAXBContext c = JAXBContext.newInstance(clazzes);
                final Marshaller m = c.createMarshaller();
                m.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
                final OutputStream out = new BufferedOutputStream(new FileOutputStream(output));

                final String[] cdataElements = getCDataElements(docProcessor);
                final XMLSerializer serializer = getXMLSerializer(out, cdataElements);

                m.marshal(result, serializer);
                out.close();

                LOG.info("Wrote " + output);

            } catch (final Exception e) {
                LOG.log(Level.SEVERE, "Could not serialize ResourceDoc.", e);
                return false;
            }
        } finally {
            Thread.currentThread().setContextClassLoader(cl);
        }

        return true;
    }

    private static String[] getCDataElements(final DocProcessor docProcessor) {
        final String[] original = new String[] {"ns1^commentText", "ns2^commentText", "^commentText"};
        if (docProcessor == null) {
            return original;
        } else {
            final String[] cdataElements = docProcessor.getCDataElements();
            if (cdataElements == null || cdataElements.length == 0) {
                return original;
            } else {

                final String[] result = copyOf(original, original.length + cdataElements.length);
                for (int i = 0; i < cdataElements.length; i++) {
                    result[original.length + i] = cdataElements[i];
                }
                return result;
            }
        }
    }

    @SuppressWarnings("unchecked")
    private static <T, U> T[] copyOf(final U[] original, final int newLength) {
        final T[] copy = (original.getClass() == Object[].class)
                ? (T[]) new Object[newLength]
                : (T[]) Array.newInstance(original.getClass().getComponentType(), newLength);
        System.arraycopy(original, 0, copy, 0,
                Math.min(original.length, newLength));
        return copy;
    }

    private static Class<?>[] getJAXBContextClasses(
            final ResourceDocType result, final DocProcessor docProcessor) {
        final Class<?>[] clazzes;
        if (docProcessor == null) {
            clazzes = new Class<?>[1];
        } else {
            final Class<?>[] requiredJaxbContextClasses = docProcessor.getRequiredJaxbContextClasses();
            if (requiredJaxbContextClasses != null) {
                clazzes = new Class<?>[1 + requiredJaxbContextClasses.length];
                for (int i = 0; i < requiredJaxbContextClasses.length; i++) {
                    clazzes[i + 1] = requiredJaxbContextClasses[i];
                }
            } else {
                clazzes = new Class<?>[1];
            }
        }
        clazzes[0] = result.getClass();
        return clazzes;
    }

    private static XMLSerializer getXMLSerializer(final OutputStream os, final String[] cdataElements)
            throws InstantiationException,
            IllegalAccessException, ClassNotFoundException {
        // configure an OutputFormat to handle CDATA
        final OutputFormat of = new OutputFormat();

        // specify which of your elements you want to be handled as CDATA.
        // The use of the '^' between the namespaceURI and the localname
        // seems to be an implementation detail of the xerces code.
        // When processing xml that doesn't use namespaces, simply omit the
        // namespace prefix as shown in the third CDataElement below.
        of.setCDataElements(cdataElements);

        // set any other options you'd like
        of.setPreserveSpace(true);
        of.setIndenting(true);

        // create the serializer
        final XMLSerializer serializer = new XMLSerializer(of);

        serializer.setOutputByteStream(os);

        return serializer;
    }

    private static void addResponseDoc(final MethodDoc methodDoc,
                                       final MethodDocType methodDocType) {

        final ResponseDocType responseDoc = new ResponseDocType();

        final Tag returnTag = getSingleTagOrNull(methodDoc, "return");
        if (returnTag != null) {
            responseDoc.setReturnDoc(returnTag.text());
        }

        final Tag[] responseParamTags = methodDoc.tags("response.param");
        for (final Tag responseParamTag : responseParamTags) {
            // LOG.info( "Have responseparam tag: " + print( responseParamTag ) );
            final WadlParamType wadlParam = new WadlParamType();
            for (final Tag inlineTag : responseParamTag.inlineTags()) {
                final String tagName = inlineTag.name();
                final String tagText = inlineTag.text();
                /* skip empty tags
                 */
                if (isEmpty(tagText)) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Skipping empty inline tag of @response.param in method "
                                + methodDoc.qualifiedName() + ": " + tagName);
                    }
                    continue;
                }
                switch (tagName) {
                    case "@name":
                        wadlParam.setName(tagText);
                        break;
                    case "@style":
                        wadlParam.setStyle(tagText);
                        break;
                    case "@type":
                        wadlParam.setType(QName.valueOf(tagText));
                        break;
                    case "@doc":
                        wadlParam.setDoc(tagText);
                        break;
                    default:
                        LOG.warning("Unknown inline tag of @response.param in method "
                                + methodDoc.qualifiedName() + ": " + tagName
                                + " (value: " + tagText + ")");
                        break;
                }
            }
            responseDoc.getWadlParams().add(wadlParam);
        }

        final Map<String, List<Tag>> tagsByStatus = getResponseRepresentationTags(methodDoc);
        for (final Entry<String, List<Tag>> entry : tagsByStatus.entrySet()) {
            final RepresentationDocType representationDoc = new RepresentationDocType();
            representationDoc.setStatus(Long.valueOf(entry.getKey()));
            for (final Tag tag : entry.getValue()) {
                if (tag.name().endsWith(".qname")) {
                    representationDoc.setElement(QName.valueOf(tag.text()));
                } else if (tag.name().endsWith(".mediaType")) {
                    representationDoc.setMediaType(tag.text());
                } else if (tag.name().endsWith(".example")) {
                    representationDoc.setExample(getSerializedExample(tag));
                } else if (tag.name().endsWith(".doc")) {
                    representationDoc.setDoc(tag.text());
                } else {
                    LOG.warning("Unknown response representation tag " + tag.name());
                }
            }
            responseDoc.getRepresentations().add(representationDoc);
        }

        methodDocType.setResponseDoc(responseDoc);
    }

    private static boolean isEmpty(final String value) {
        return value == null || value.isEmpty() || value.trim().isEmpty();
    }

    private static void addRequestRepresentationDoc(final MethodDoc methodDoc,
                                                    final MethodDocType methodDocType) {
        final Tag requestElement = getSingleTagOrNull(methodDoc, "request.representation.qname");
        final Tag requestExample = getSingleTagOrNull(methodDoc, "request.representation.example");
        if (requestElement != null || requestExample != null) {
            final RequestDocType requestDoc = new RequestDocType();
            final RepresentationDocType representationDoc = new RepresentationDocType();

            /* requestElement exists
             */
            if (requestElement != null) {
                representationDoc.setElement(QName.valueOf(requestElement.text()));
            }

            /* requestExample exists
             */
            if (requestExample != null) {
                final String example = getSerializedExample(requestExample);
                if (!isEmpty(example)) {
                    representationDoc.setExample(example);
                } else {
                    LOG.warning("Could not get serialized example for method " + methodDoc.qualifiedName());
                }
            }

            requestDoc.setRepresentationDoc(representationDoc);
            methodDocType.setRequestDoc(requestDoc);
        }
    }

    private static Map<String, List<Tag>> getResponseRepresentationTags(final MethodDoc methodDoc) {
        final Map<String, List<Tag>> tagsByStatus = new HashMap<>();
        for (final Tag tag : methodDoc.tags()) {
            final Matcher matcher = PATTERN_RESPONSE_REPRESENTATION.matcher(tag.name());
            if (matcher.matches()) {
                final String status = matcher.group(1);
                List<Tag> tags = tagsByStatus.get(status);
                if (tags == null) {
                    tags = new ArrayList<>();
                    tagsByStatus.put(status, tags);
                }
                tags.add(tag);
            }
        }
        return tagsByStatus;
    }

    /**
     * Searches an <code>@link</code> tag within the inline tags of the specified tag
     * and serializes the referenced instance.
     *
     * @param tag the tag containing the inline tags to be searched.
     * @return the {@code String} representation of the {@link com.sun.javadoc.Tag} or null if the parameter is null.
     */
    private static String getSerializedExample(final Tag tag) {
        if (tag != null) {
            final Tag[] inlineTags = tag.inlineTags();
            if (inlineTags != null && inlineTags.length > 0) {
                for (final Tag inlineTag : inlineTags) {
                    if (LOG.isLoggable(Level.FINE)) {
                        LOG.fine("Have inline tag: " + print(inlineTag));
                    }
                    if ("@link".equals(inlineTag.name())) {
                        if (LOG.isLoggable(Level.FINE)) {
                            LOG.fine("Have link: " + print(inlineTag));
                        }
                        final SeeTag linkTag = (SeeTag) inlineTag;
                        return getSerializedLinkFromTag(linkTag);
                    } else if (!isEmpty(inlineTag.text())) {
                        return inlineTag.text();
                    }
                }
            } else {
                LOG.fine("Have example: " + print(tag));
                return tag.text();
            }
        }
        return null;
    }

    private static Tag getSingleTagOrNull(final MethodDoc methodDoc, final String tagName) {
        final Tag[] tags = methodDoc.tags(tagName);
        if (tags != null && tags.length == 1) {
            return tags[0];
        }
        return null;
    }

    private static void addParamDocs(final MethodDoc methodDoc,
                                     final MethodDocType methodDocType,
                                     final DocProcessor docProcessor) {
        final Parameter[] parameters = methodDoc.parameters();
        final ParamTag[] paramTags = methodDoc.paramTags();

        /* only use both javadoc and reflection information when the number
         * of params are the same
         */
        if (parameters != null && paramTags != null
                && parameters.length == paramTags.length) {

            for (int i = 0; i < parameters.length; i++) {
                final Parameter parameter = parameters[i];

                /* TODO: this only works if the params and tags are in the same
                 * order. If the param tags are mixed up, the comments for parameters
                 * will be wrong.
                 */
                final ParamTag paramTag = paramTags[i];

                final ParamDocType paramDocType = new ParamDocType();
                paramDocType.setParamName(paramTag.parameterName());
                paramDocType.setCommentText(paramTag.parameterComment());
                docProcessor.processParamTag(paramTag, parameter, paramDocType);

                final AnnotationDesc[] annotations = parameter.annotations();
                if (annotations != null) {
                    for (final AnnotationDesc annotationDesc : annotations) {
                        final AnnotationDocType annotationDocType = new AnnotationDocType();
                        final String typeName = annotationDesc.annotationType().qualifiedName();
                        annotationDocType.setAnnotationTypeName(typeName);
                        for (final ElementValuePair elementValuePair : annotationDesc.elementValues()) {
                            final NamedValueType namedValueType = new NamedValueType();
                            namedValueType.setName(elementValuePair.element().name());
                            namedValueType.setValue(elementValuePair.value().value().toString());
                            annotationDocType.getAttributeDocs().add(namedValueType);
                        }
                        paramDocType.getAnnotationDocs().add(annotationDocType);
                    }
                }

                methodDocType.getParamDocs().add(paramDocType);

            }

        }
    }

    private static String getSerializedLinkFromTag(final SeeTag linkTag) {
        final MemberDoc referencedMember = linkTag.referencedMember();

        if (referencedMember == null) {
            throw new NullPointerException("Referenced member of @link " + print(linkTag) + " cannot be resolved.");
        }

        if (!referencedMember.isStatic()) {
            LOG.warning("Referenced member of @link " + print(linkTag) + " is not static."
                    + " Right now only references to static members are supported.");
            return null;
        }

        /* Get referenced example bean
         */
        final ClassDoc containingClass = referencedMember.containingClass();
        final Object object;
        try {
            final Field declaredField = Class.forName(containingClass.qualifiedName(), false, Thread.currentThread()
                    .getContextClassLoader()).getDeclaredField(referencedMember.name());
            if (referencedMember.isFinal()) {
                declaredField.setAccessible(true);
            }
            object = declaredField.get(null);
            LOG.log(Level.FINE, "Got object " + object);
        } catch (final Exception e) {
            LOG.info("Have classloader: " + ResourceDoclet.class.getClassLoader().getClass());
            LOG.info("Have thread classloader " + Thread.currentThread().getContextClassLoader().getClass());
            LOG.info("Have system classloader " + ClassLoader.getSystemClassLoader().getClass());
            LOG.log(Level.SEVERE, "Could not get field " + referencedMember.qualifiedName(), e);
            return null;
        }

        /* marshal the bean to xml
         */
        try {
            final JAXBContext jaxbContext = JAXBContext.newInstance(object.getClass());
            final StringWriter stringWriter = new StringWriter();
            final Marshaller marshaller = jaxbContext.createMarshaller();
            marshaller.setProperty(Marshaller.JAXB_FORMATTED_OUTPUT, true);
            marshaller.marshal(object, stringWriter);
            final String result = stringWriter.getBuffer().toString();
            LOG.log(Level.FINE, "Got marshalled output:\n" + result);
            return result;
        } catch (final Exception e) {
            LOG.log(Level.SEVERE, "Could serialize bean to xml: " + object, e);
            return null;
        }
    }

    private static String print(final Tag tag) {
        return String.valueOf(tag.getClass()) + "["
                + "firstSentenceTags=" + toCSV(tag.firstSentenceTags())
                + ", inlineTags=" + toCSV(tag.inlineTags())
                + ", kind=" + tag.kind()
                + ", name=" + tag.name()
                + ", text=" + tag.text()
                + "]";
    }

    static String toCSV(final Tag[] items) {
        if (items == null) {
            return null;
        }
        return toCSV(Arrays.asList(items));
    }

    static String toCSV(final Collection<Tag> items) {
        return toCSV(items, ", ", null);
    }

    static String toCSV(final Collection<Tag> items, final String separator, final String delimiter) {
        if (items == null) {
            return null;
        }
        if (items.isEmpty()) {
            return "";
        }
        final StringBuilder sb = new StringBuilder();
        for (final Iterator<Tag> iter = items.iterator(); iter.hasNext(); ) {
            if (delimiter != null) {
                sb.append(delimiter);
            }
            final Tag item = iter.next();
            sb.append(item.name());
            if (delimiter != null) {
                sb.append(delimiter);
            }
            if (iter.hasNext()) {
                sb.append(separator);
            }
        }
        return sb.toString();
    }

    /**
     * Return array length for given option: 1 + the number of arguments that
     * the option takes.
     *
     * @param option option
     * @return the number of args for the specified option
     */
    public static int optionLength(final String option) {
        LOG.fine("Invoked with option " + option);

        if (OPTION_OUTPUT.equals(option)
                || OPTION_CLASSPATH.equals(option)
                || OPTION_DOC_PROCESSORS.equals(option)) {
            return 2;
        }

        return 0;
    }

    /**
     * Validate options.
     *
     * @param options  options to be validated
     * @param reporter {@link com.sun.javadoc.DocErrorReporter} for collecting eventual errors
     * @return if the specified options are valid
     */
    public static boolean validOptions(final String[][] options, final DocErrorReporter reporter) {
        return validOption(OPTION_OUTPUT, "<path-to-file>", options, reporter)
                && validOption(OPTION_CLASSPATH, "<path>", options, reporter);
    }

    private static boolean validOption(final String optionName,
                                       final String reportOptionName,
                                       final String[][] options,
                                       final DocErrorReporter reporter) {
        final String option = getOptionArg(options, optionName);

        final boolean foundOption = option != null && !option.trim().isEmpty();
        if (!foundOption) {
            reporter.printError(optionName + " " + reportOptionName + " must be specified.");
        }
        return foundOption;
    }

    private static String getOptionArg(final String[][] options, final String option) {

        for (final String[] opt : options) {
            if (opt[0].equals(option)) {
                return opt[1];
            }
        }

        return null;
    }

    static class Loader extends URLClassLoader {

        public Loader(final String[] paths, final ClassLoader parent) {
            super(getURLs(paths), parent);
        }

        Loader(final String[] paths) {
            super(getURLs(paths));
        }

        private static URL[] getURLs(final String[] paths) {
            final List<URL> urls = new ArrayList<>();
            for (final String path : paths) {
                try {
                    urls.add(new File(path).toURI().toURL());
                } catch (final MalformedURLException e) {
                    throw new RuntimeException(e);
                }
            }
            return urls.toArray(new URL[urls.size()]);
        }

    }

}
