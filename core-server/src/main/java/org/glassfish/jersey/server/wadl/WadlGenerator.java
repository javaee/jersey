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

package org.glassfish.jersey.server.wadl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.MediaType;

import javax.xml.bind.JAXBException;
import javax.xml.bind.annotation.XmlRegistry;
import javax.xml.namespace.QName;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.Representation;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import com.sun.research.ws.wadl.Response;

/**
 * A WadlGenerator creates artifacts related to wadl. This is designed as an interface,
 * so that several implementations can decorate existing ones. One decorator could e.g. add
 * references to definitions within some xsd for existing representations.<br>
 * Created on: Jun 16, 2008<br>
 *
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 */
public interface WadlGenerator {

    /**
     * Sets the delegate that is decorated by this wadl generator. Is invoked directly after
     * this generator is instantiated before {@link #init()} or any setter method is invoked.
     * @param delegate the wadl generator to decorate
     */
    void setWadlGeneratorDelegate(WadlGenerator delegate);

    /**
     * Invoked before all methods related to wadl-building are invoked. This method is used in a
     * decorator like manner, and therefore has to invoke {@code this.delegate.init()}.
     *
     * @throws IllegalStateException
     * @throws JAXBException
     */
    void init() throws Exception;

    /**
     * The jaxb context path that is used when the generated wadl application is marshalled
     * to a file.
     *
     * This method is used in a decorator like manner.
     * The result return the path (or a colon-separated list of package names) containing
     * jaxb-beans that are added to wadl elements by this WadlGenerator, additionally to
     * the context path of the decorated WadlGenerator (set by {@link #setWadlGeneratorDelegate(WadlGenerator)}.<br/>
     * If you do not use custom jaxb beans, then simply return {@code _delegate.getRequiredJaxbContextPath()},
     * otherwise return the delegate's #getRequiredJaxbContextPath() together with
     * your required context path (separated by a colon):<br/>
     * <pre>_delegate.getRequiredJaxbContextPath() == null
     ? ${yourContextPath}
     : _delegate.getRequiredJaxbContextPath() + ":" + ${yourContextPath};</pre>
     *
     * If you add the path for your custom jaxb beans, don't forget to add an
     * ObjectFactory (annotated with {@link XmlRegistry}) to this package.
     * @return simply the {@code getRequiredJaxbContextPath()} of the delegate or the
     * {@code getRequiredJaxbContextPath() + ":" + ${yourContextPath}}.
     */
    String getRequiredJaxbContextPath();

    // ================  methods for building the wadl application =============

    Application createApplication();

    Resources createResources();

    Resource createResource(org.glassfish.jersey.server.model.Resource r,
                                   String path);

    com.sun.research.ws.wadl.Method createMethod(org.glassfish.jersey.server.model.Resource r,
                                                        org.glassfish.jersey.server.model.ResourceMethod m);

    Request createRequest(org.glassfish.jersey.server.model.Resource r,
                                 org.glassfish.jersey.server.model.ResourceMethod m);

    Representation createRequestRepresentation(org.glassfish.jersey.server.model.Resource r,
                                                      org.glassfish.jersey.server.model.ResourceMethod m,
                                                      MediaType mediaType);

    List<Response> createResponses(org.glassfish.jersey.server.model.Resource r,
                                          org.glassfish.jersey.server.model.ResourceMethod m);

    Param createParam(org.glassfish.jersey.server.model.Resource r,
                             org.glassfish.jersey.server.model.ResourceMethod m,
                             Parameter p);

    // ================ methods for post build actions =======================

    /**
     * Call back interface that the created external grammar can use
     * to allow other parts of the code to attach the correct grammar information.
     */
    interface Resolver {

        /**
         * Resolve a Class type to a QName.
         *
         * @param type The type of the class.
         * @return The schema type of the class if defined, null if not.
         */
        QName resolve(Class type);
    }

    /**
     * And internal storage object to store the grammar definitions and
     * any type resolvers that are created along the way.
     */
    class ExternalGrammarDefinition {

        // final public field to make a property was thinking about encapsulation
        // but decided code much simpler without
        public final Map<String, ApplicationDescription.ExternalGrammar>
                map = new HashMap<String, ApplicationDescription.ExternalGrammar>();

        private List<Resolver> typeResolvers = new ArrayList<Resolver>();

        public void addResolver(Resolver resolver) {
            assert !typeResolvers.contains(resolver) : "Already in list";
            typeResolvers.add(resolver);
        }

        /**
         * @param type the class to map
         * @return The resolved qualified name if one is defined.
         */
        public QName resolve(Class type) {
            QName name = null;
            found:
            for (Resolver resolver : typeResolvers) {
                name = resolver.resolve(type);
                if (name != null) {
                    break found;
                }
            }
            return name;
        }
    }

    /**
     * Perform any post create functions such as generating grammars.
     * @return A map of extra files to the content of those file encoded in UTF-8
     */
    ExternalGrammarDefinition createExternalGrammar();

    /**
     * Process the elements in the WADL definition to attach schema types
     * as required.
     * @param description The root description used to resolve these entries
     */
    void attachTypes(ApplicationDescription description);

}
