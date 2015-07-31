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

import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ClassDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.MethodDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ParamDocType;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;

/**
 * A doc processor is handed over javadoc elements so that it can turn this into
 * resource doc elements, even self defined.
 *
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 */
public interface DocProcessor {

    /**
     * Specify jaxb classes of instances that you add to the {@code resourcedoc} model.
     * These classes are added to the list of classes when creating the jaxb context
     * with {@code JAXBContext.newInstance( clazzes );}.
     *
     * @return a list of classes or {@code null}
     */
    Class<?>[] getRequiredJaxbContextClasses();

    /**
     * specify which of your elements you want to be handled as CDATA.
     * The use of the '^' between the {@code namespaceURI} and the {@code localname}
     * seems to be an implementation detail of the xerces code.
     * When processing xml that doesn't use namespaces, simply omit the
     * namespace prefix as shown in the third CDataElement below.
     *
     * @return an Array of element descriptors or {@code null}
     */
    String[] getCDataElements();

    /**
     * Use this method to extend the provided {@link ClassDocType} with the information from
     * the given {@link ClassDoc}.
     *
     * @param classDoc     the class javadoc
     * @param classDocType the {@link ClassDocType} to extend. This will later be processed by the
     *                     {@link org.glassfish.jersey.server.wadl.WadlGenerator}s.
     */
    void processClassDoc(ClassDoc classDoc, ClassDocType classDocType);

    /**
     * Process the provided methodDoc and add your custom information to the methodDocType.<br>
     * Use e.g. {@link MethodDocType#getAny()} to store custom elements.
     *
     * @param methodDoc     the {@link MethodDoc} representing the docs of your method.
     * @param methodDocType the related {@link MethodDocType} that will later be processed by the
     *                      {@link org.glassfish.jersey.server.wadl.WadlGenerator}s.
     */
    void processMethodDoc(MethodDoc methodDoc, MethodDocType methodDocType);

    /**
     * Use this method to extend the provided {@link ParamDocType} with the information from the
     * given {@link ParamTag} and {@link Parameter}.
     *
     * @param paramTag     the parameter javadoc
     * @param parameter    the parameter (that is documented or not)
     * @param paramDocType the {@link ParamDocType} to extend. This will later be processed by the
     *                     {@link org.glassfish.jersey.server.wadl.WadlGenerator}s.
     */
    void processParamTag(ParamTag paramTag, Parameter parameter, ParamDocType paramDocType);

}
