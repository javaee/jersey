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

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ClassDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.MethodDocType;
import org.glassfish.jersey.server.wadl.internal.generators.resourcedoc.model.ParamDocType;

import com.sun.javadoc.ClassDoc;
import com.sun.javadoc.MethodDoc;
import com.sun.javadoc.ParamTag;
import com.sun.javadoc.Parameter;

/**
 * This {@link DocProcessor} wraps multiple {@code DocProcessor}s.
 *
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 */
public class DocProcessorWrapper implements DocProcessor {

    private final List<DocProcessor> _docProcessors;

    /**
     * Create new {@code DocProcessorWrapper} instance.
     */
    public DocProcessorWrapper() {
        _docProcessors = new ArrayList<>();
    }

    /**
     * Add a new  {@code DocProcessor} instance to the list of wrapped instances.
     *
     * @param docProcessor  {@code DocProcessor} instance to wrap.
     */
    void add(DocProcessor docProcessor) {
        _docProcessors.add(docProcessor);
    }

    @Override
    public Class<?>[] getRequiredJaxbContextClasses() {
        final List<Class<?>> result = new ArrayList<>();
        for (DocProcessor docProcessor : _docProcessors) {
            final Class<?>[] requiredJaxbContextClasses = docProcessor.getRequiredJaxbContextClasses();
            if (requiredJaxbContextClasses != null && requiredJaxbContextClasses.length > 0) {
                result.addAll(Arrays.asList(requiredJaxbContextClasses));
            }
        }
        return result.toArray(new Class<?>[result.size()]);
    }

    @Override
    public String[] getCDataElements() {
        final List<String> result = new ArrayList<>();
        for (DocProcessor docProcessor : _docProcessors) {
            final String[] cdataElements = docProcessor.getCDataElements();
            if (cdataElements != null && cdataElements.length > 0) {
                result.addAll(Arrays.asList(cdataElements));
            }
        }
        return result.toArray(new String[result.size()]);
    }

    @Override
    public void processClassDoc(ClassDoc classDoc, ClassDocType classDocType) {
        for (DocProcessor docProcessor : _docProcessors) {
            docProcessor.processClassDoc(classDoc, classDocType);
        }
    }

    @Override
    public void processMethodDoc(MethodDoc methodDoc,
                                 MethodDocType methodDocType) {
        for (DocProcessor docProcessor : _docProcessors) {
            docProcessor.processMethodDoc(methodDoc, methodDocType);
        }
    }

    @Override
    public void processParamTag(ParamTag paramTag, Parameter parameter,
                                ParamDocType paramDocType) {
        for (DocProcessor docProcessor : _docProcessors) {
            docProcessor.processParamTag(paramTag, parameter, paramDocType);
        }
    }

}
