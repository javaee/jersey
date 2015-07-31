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

package org.glassfish.jersey.server.wadl.internal.generators;

import java.io.CharArrayWriter;
import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.security.AccessController;
import java.security.PrivilegedActionException;
import java.security.PrivilegedExceptionAction;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.JAXBIntrospector;
import javax.xml.bind.SchemaOutputResolver;
import javax.xml.bind.annotation.XmlRootElement;
import javax.xml.bind.annotation.XmlSeeAlso;
import javax.xml.namespace.QName;
import javax.xml.transform.Result;
import javax.xml.transform.stream.StreamResult;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.wadl.WadlGenerator;
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription;
import org.glassfish.jersey.server.wadl.internal.WadlGeneratorImpl;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Method;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.Representation;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import com.sun.research.ws.wadl.Response;

/**
 * This {@link org.glassfish.jersey.server.wadl.WadlGenerator} generates a XML Schema content model based on
 * referenced java beans.
 * </p>
 * Created on: Jun 22, 2011<br>
 *
 * @author Gerard Davison
 * @author Miroslav Fuksa
 */
public class WadlGeneratorJAXBGrammarGenerator implements WadlGenerator {

    private static interface NameCallbackSetter {

        public void setName(QName name);
    }

    private class TypeCallbackPair {

        public TypeCallbackPair(final GenericType<?> genericType, final NameCallbackSetter nameCallbackSetter) {
            this.genericType = genericType;
            this.nameCallbackSetter = nameCallbackSetter;
        }

        GenericType<?> genericType;
        NameCallbackSetter nameCallbackSetter;
    }

    private static final Logger LOGGER = Logger.getLogger(WadlGeneratorJAXBGrammarGenerator.class.getName());
    private static final java.util.Set<Class> SPECIAL_GENERIC_TYPES =
            new HashSet<Class>() {{
                // TODO - J2 - we do not have JResponse but we should support GenericEntity
                //                    add(JResponse.class);
                add(List.class);
            }};

    // The generator we are decorating
    private WadlGenerator wadlGeneratorDelegate;

    // Any SeeAlso references
    private Set<Class> seeAlsoClasses;

    // A matched list of Parm, Parameter to list the relavent
    // entity objects that we might like to transform.
    private List<TypeCallbackPair> nameCallbacks;

    public WadlGeneratorJAXBGrammarGenerator() {
        wadlGeneratorDelegate = new WadlGeneratorImpl();
    }

    // =============== House keeping methods ================================

    public void setWadlGeneratorDelegate(final WadlGenerator delegate) {
        wadlGeneratorDelegate = delegate;
    }

    public String getRequiredJaxbContextPath() {
        return wadlGeneratorDelegate.getRequiredJaxbContextPath();
    }

    public void init() throws Exception {
        wadlGeneratorDelegate.init();
        //
        seeAlsoClasses = new HashSet<>();

        // A matched list of Parm, Parameter to list the relavent
        // entity objects that we might like to transform.
        nameCallbacks = new ArrayList<>();
    }

    // =============== Application Creation ================================

    /**
     * @return application
     * @see org.glassfish.jersey.server.wadl.WadlGenerator#createApplication()
     */
    public Application createApplication() {
        return wadlGeneratorDelegate.createApplication();
    }

    /**
     * @param ar  abstract resource
     * @param arm abstract resource method
     * @return method
     * @see org.glassfish.jersey.server.wadl.WadlGenerator#createMethod(org.glassfish.jersey.server.model.Resource,
     *      org.glassfish.jersey.server.model.ResourceMethod)
     */
    public Method createMethod(final org.glassfish.jersey.server.model.Resource ar,
                               final org.glassfish.jersey.server.model.ResourceMethod arm) {
        return wadlGeneratorDelegate.createMethod(ar, arm);
    }

    /**
     * @param ar  abstract resource
     * @param arm abstract resource method
     * @return request
     * @see org.glassfish.jersey.server.wadl.WadlGenerator#createRequest(org.glassfish.jersey.server.model.Resource,
     *      org.glassfish.jersey.server.model.ResourceMethod)
     */
    public Request createRequest(final org.glassfish.jersey.server.model.Resource ar,
                                 final org.glassfish.jersey.server.model.ResourceMethod arm) {

        return wadlGeneratorDelegate.createRequest(ar, arm);
    }

    /**
     * @param ar abstract resource
     * @param am abstract method
     * @param p  parameter
     * @return parameter
     * @see org.glassfish.jersey.server.wadl.WadlGenerator#createParam(org.glassfish.jersey.server.model.Resource,
     *      org.glassfish.jersey.server.model.ResourceMethod, org.glassfish.jersey.server.model.Parameter)
     */
    public Param createParam(final org.glassfish.jersey.server.model.Resource ar,
                             final org.glassfish.jersey.server.model.ResourceMethod am, final Parameter p) {
        final Param param = wadlGeneratorDelegate.createParam(ar, am, p);

        // If the parameter is an entity we probably want to convert this to XML
        if (p.getSource() == Parameter.Source.ENTITY) {
            nameCallbacks.add(new TypeCallbackPair(
                    new GenericType(p.getType()),
                    new NameCallbackSetter() {
                        public void setName(final QName name) {
                            param.setType(name);
                        }
                    }));
        }

        return param;
    }

    /**
     * @param ar  abstract resource
     * @param arm abstract resource method
     * @param mt  media type
     * @return respresentation type
     * @see org.glassfish.jersey.server.wadl.WadlGenerator#createRequestRepresentation(org.glassfish.jersey.server.model.Resource,
     * org.glassfish.jersey.server.model.ResourceMethod, javax.ws.rs.core.MediaType)
     */
    public Representation createRequestRepresentation(
            final org.glassfish.jersey.server.model.Resource ar,
            final org.glassfish.jersey.server.model.ResourceMethod arm,
            final MediaType mt) {

        final Representation rt = wadlGeneratorDelegate.createRequestRepresentation(ar, arm, mt);

        for (final Parameter p : arm.getInvocable().getParameters()) {
            if (p.getSource() == Parameter.Source.ENTITY) {
                nameCallbacks.add(new TypeCallbackPair(
                        new GenericType(p.getType()),
                        new NameCallbackSetter() {
                            @Override
                            public void setName(final QName name) {
                                rt.setElement(name);
                            }
                        }));
            }
        }

        return rt;
    }

    /**
     * @param ar   abstract resource
     * @param path resources path
     * @return resource
     * @see org.glassfish.jersey.server.wadl.WadlGenerator#createResource(org.glassfish.jersey.server.model.Resource, String)
     */
    public Resource createResource(final org.glassfish.jersey.server.model.Resource ar, final String path) {
        for (final Class<?> resClass : ar.getHandlerClasses()) {
            final XmlSeeAlso seeAlso = resClass.getAnnotation(XmlSeeAlso.class);
            if (seeAlso != null) {
                Collections.addAll(seeAlsoClasses, seeAlso.value());
            }
        }

        return wadlGeneratorDelegate.createResource(ar, path);
    }

    /**
     * @return resources
     * @see org.glassfish.jersey.server.wadl.WadlGenerator#createResources()
     */
    public Resources createResources() {
        return wadlGeneratorDelegate.createResources();
    }

    /**
     * @param resource       abstract resource
     * @param resourceMethod abstract resource method
     * @return response
     * @see org.glassfish.jersey.server.wadl.WadlGenerator#createResponses(org.glassfish.jersey.server.model.Resource,
     *      org.glassfish.jersey.server.model.ResourceMethod)
     */
    public List<Response> createResponses(final org.glassfish.jersey.server.model.Resource resource,
                                          final org.glassfish.jersey.server.model.ResourceMethod resourceMethod) {
        final List<Response> responses = wadlGeneratorDelegate.createResponses(resource, resourceMethod);
        if (responses != null) {

            for (final Response response : responses) {
                for (final Representation representation : response.getRepresentation()) {

                    // Process each representation
                    nameCallbacks.add(new TypeCallbackPair(
                            new GenericType(resourceMethod.getInvocable().getResponseType()),
                            new NameCallbackSetter() {

                                public void setName(final QName name) {
                                    representation.setElement(name);
                                }
                            }));
                }
            }
        }
        return responses;
    }

    // ================ methods for post build actions =======================

    public ExternalGrammarDefinition createExternalGrammar() {

        // Right now lets generate some external metadata

        final Map<String, ApplicationDescription.ExternalGrammar> extraFiles = new HashMap<>();

        // Build the model as required
        final Resolver resolver = buildModelAndSchemas(extraFiles);

        // Pass onto the next delegate
        final ExternalGrammarDefinition previous = wadlGeneratorDelegate.createExternalGrammar();
        previous.map.putAll(extraFiles);
        if (resolver != null) {
            previous.addResolver(resolver);
        }

        return previous;
    }

    /**
     * Build the JAXB model and generate the schemas based on tha data
     *
     * @param extraFiles additional files.
     * @return class to {@link QName} resolver.
     */
    private Resolver buildModelAndSchemas(final Map<String, ApplicationDescription.ExternalGrammar> extraFiles) {

        // Lets get all candidate classes so we can create the JAX-B context
        // include any @XmlSeeAlso references.

        final Set<Class> classSet = new HashSet<>(seeAlsoClasses);

        for (final TypeCallbackPair pair : nameCallbacks) {
            final GenericType genericType = pair.genericType;
            final Class<?> clazz = genericType.getRawType();

            // Is this class itself interesting?

            if (clazz.getAnnotation(XmlRootElement.class) != null) {
                classSet.add(clazz);
            } else if (SPECIAL_GENERIC_TYPES.contains(clazz)) {

                final Type type = genericType.getType();
                if (type instanceof ParameterizedType) {
                    final Type parameterType = ((ParameterizedType) type).getActualTypeArguments()[0];
                    if (parameterType instanceof Class) {
                        classSet.add((Class) parameterType);
                    }
                }
            }
        }

        // Create a JAX-B context, and use this to generate us a bunch of
        // schema objects

        JAXBIntrospector introspector = null;

        try {
            final JAXBContext context = JAXBContext.newInstance(classSet.toArray(new Class[classSet.size()]));

            final List<StreamResult> results = new ArrayList<>();

            context.generateSchema(new SchemaOutputResolver() {

                int counter = 0;

                @Override
                public Result createOutput(final String namespaceUri, final String suggestedFileName) {
                    final StreamResult result = new StreamResult(new CharArrayWriter());
                    result.setSystemId("xsd" + (counter++) + ".xsd");
                    results.add(result);
                    return result;
                }
            });

            // Store the new files for later use
            //

            for (final StreamResult result : results) {
                final CharArrayWriter writer = (CharArrayWriter) result.getWriter();
                final byte[] contents = writer.toString().getBytes("UTF8");
                extraFiles.put(
                        result.getSystemId(),
                        new ApplicationDescription.ExternalGrammar(
                                MediaType.APPLICATION_XML_TYPE, // I don't think there is a specific media type for XML Schema
                                contents));
            }

            // Create an introspector
            //

            introspector = context.createJAXBIntrospector();

        } catch (final JAXBException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate the schema for the JAX-B elements", e);
        } catch (final IOException e) {
            LOGGER.log(Level.SEVERE, "Failed to generate the schema for the JAX-B elements due to an IO error", e);
        }

        // Create introspector

        if (introspector != null) {
            final JAXBIntrospector copy = introspector;

            return new Resolver() {

                public QName resolve(final Class type) {

                    Object parameterClassInstance = null;
                    try {
                        final Constructor<?> defaultConstructor =
                                AccessController.doPrivileged(new PrivilegedExceptionAction<Constructor<?>>() {
                                    @SuppressWarnings("unchecked")
                                    @Override
                                    public Constructor<?> run() throws NoSuchMethodException {
                                        final Constructor<?> constructor = type.getDeclaredConstructor();
                                        constructor.setAccessible(true);
                                        return constructor;
                                    }
                                });
                        parameterClassInstance = defaultConstructor.newInstance();
                    } catch (final InstantiationException | SecurityException | IllegalAccessException
                            | IllegalArgumentException | InvocationTargetException ex) {
                        LOGGER.log(Level.FINE, null, ex);
                    } catch (final PrivilegedActionException ex) {
                        LOGGER.log(Level.FINE, null, ex.getCause());
                    }

                    if (parameterClassInstance == null) {
                        return null;
                    }

                    try {
                        return copy.getElementName(parameterClassInstance);
                    } catch (final NullPointerException e) {
                        // EclipseLink throws an NPE if an object annotated with @XmlType and without the @XmlRootElement
                        // annotation is passed as a parameter of #getElementName method.
                        return null;
                    }
                }
            };
        } else {
            return null; // No resolver created
        }
    }

    public void attachTypes(final ApplicationDescription introspector) {

        // If we managed to get an introspector then lets go back an update the parameters

        if (introspector != null) {

            for (final TypeCallbackPair pair : nameCallbacks) {

                // There is a method on the RI version that works with just
                // the class name; but using the introspector for the moment
                // as it leads to cleaner code
                Class<?> parameterClass = pair.genericType.getRawType();

                // Fix those specific generic types
                if (SPECIAL_GENERIC_TYPES.contains(parameterClass)) {
                    final Type type = pair.genericType.getType();

                    if (ParameterizedType.class.isAssignableFrom(type.getClass())
                            && Class.class.isAssignableFrom(((ParameterizedType) type).getActualTypeArguments()[0].getClass())) {
                        parameterClass = (Class) ((ParameterizedType) type).getActualTypeArguments()[0];
                    } else {
                        // Works around JERSEY-830
                        LOGGER.fine("Couldn't find JAX-B element due to nested parameterized type " + type);
                        return;
                    }
                }

                final QName name = introspector.resolve(parameterClass);

                if (name != null) {
                    pair.nameCallbackSetter.setName(name);
                } else {
                    LOGGER.fine("Couldn't find JAX-B element for class " + parameterClass.getName());
                }
            }
        }
    }
}
