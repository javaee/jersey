/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.model;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.Severity;
import org.glassfish.jersey.internal.Errors;
import org.glassfish.jersey.message.MessageBodyWorkers;
import org.glassfish.jersey.message.internal.MediaTypes;
import org.glassfish.jersey.server.internal.LocalizationMessages;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Runtime resource model validator validating ambiguity of resource methods.
 *
 * @author Miroslav Fuksa
 */
public class RuntimeResourceModelValidator extends AbstractResourceModelVisitor {

    private final MessageBodyWorkers workers;

    /**
     * Create a new validator instance.
     *
     * @param workers Message body workers.
     */
    public RuntimeResourceModelValidator(MessageBodyWorkers workers) {
        this.workers = workers;
    }

    @Override
    public void visitRuntimeResource(RuntimeResource runtimeResource) {
        checkMethods(runtimeResource);
    }

    private void checkMethods(RuntimeResource resource) {
        final List<ResourceMethod> resourceMethods = Lists.newArrayList(resource.getResourceMethods());
        resourceMethods.addAll(resource.getResourceLocators());
        if (resourceMethods.size() >= 2) {
            for (ResourceMethod m1 : resourceMethods.subList(0, resourceMethods.size() - 1)) {
                for (ResourceMethod m2 : resourceMethods.subList(resourceMethods.indexOf(m1) + 1, resourceMethods.size())) {
                    if (m1.getHttpMethod() == null && m2.getHttpMethod() == null) {
                        Errors.error(this, LocalizationMessages.AMBIGUOUS_SRLS_PATH_PATTERN(resource.getFullPathRegex()),
                                Severity.FATAL);
                    } else if (m1.getHttpMethod() != null && m2.getHttpMethod() != null && sameHttpMethod(m1, m2)) {
                        checkIntersectingMediaTypes(resource, m1.getHttpMethod(), m1, m2);
                    }
                }
            }
        }
    }

    private void checkIntersectingMediaTypes(
            RuntimeResource runtimeResource,
            String httpMethod,
            ResourceMethod m1,
            ResourceMethod m2) {

        final List<MediaType> inputTypes1 = getEffectiveInputTypes(m1);
        final List<MediaType> inputTypes2 = getEffectiveInputTypes(m2);
        final List<MediaType> outputTypes1 = getEffectiveOutputTypes(m1);
        final List<MediaType> outputTypes2 = getEffectiveOutputTypes(m2);

        boolean consumesFails;
        boolean consumesOnlyIntersects = false;
        if (m1.getConsumedTypes().isEmpty() || m2.getConsumedTypes().isEmpty()) {
            consumesFails = inputTypes1.equals(inputTypes2);
            if (!consumesFails) {
                consumesOnlyIntersects = MediaTypes.intersect(inputTypes1, inputTypes2);
            }
        } else {
            consumesFails = MediaTypes.intersect(inputTypes1, inputTypes2);
        }

        boolean producesFails;
        boolean producesOnlyIntersects = false;
        if (m1.getProducedTypes().isEmpty() || m2.getProducedTypes().isEmpty()) {
            producesFails = outputTypes1.equals(outputTypes2);
            if (!producesFails) {
                producesOnlyIntersects = MediaTypes.intersect(outputTypes1, outputTypes2);
            }
        } else {
            producesFails = MediaTypes.intersect(outputTypes1, outputTypes2);
        }

        if (consumesFails && producesFails) {
            // fatal
            Errors.fatal(runtimeResource, LocalizationMessages.AMBIGUOUS_FATAL_RMS(httpMethod, m1.getInvocable()
                    .getHandlingMethod(), m2.getInvocable().getHandlingMethod(), runtimeResource.getRegex()));
        } else if ((producesFails && consumesOnlyIntersects)
                || (consumesFails && producesOnlyIntersects)
                || (consumesOnlyIntersects && producesOnlyIntersects)) {
            // warning
            if (m1.getInvocable().requiresEntity()) {
                Errors.hint(runtimeResource, LocalizationMessages.AMBIGUOUS_RMS_IN(
                        httpMethod, m1.getInvocable().getHandlingMethod(), m2.getInvocable().getHandlingMethod(),
                        runtimeResource.getRegex()));
            } else {
                Errors.hint(runtimeResource, LocalizationMessages.AMBIGUOUS_RMS_OUT(
                        httpMethod, m1.getInvocable().getHandlingMethod(), m2.getInvocable().getHandlingMethod(),
                        runtimeResource.getRegex()));
            }
        }
    }

    private static final List<MediaType> StarTypeList = Arrays.asList(new MediaType("*", "*"));

    private List<MediaType> getEffectiveInputTypes(final ResourceMethod resourceMethod) {
        if (!resourceMethod.getConsumedTypes().isEmpty()) {
            return resourceMethod.getConsumedTypes();
        }
        List<MediaType> result = new LinkedList<>();
        if (workers != null) {
            for (Parameter p : resourceMethod.getInvocable().getParameters()) {
                if (p.getSource() == Parameter.Source.ENTITY) {
                    result.addAll(workers.getMessageBodyReaderMediaTypes(
                            p.getRawType(), p.getType(), p.getDeclaredAnnotations()));
                }
            }
        }
        return result.isEmpty() ? StarTypeList : result;
    }

    private List<MediaType> getEffectiveOutputTypes(final ResourceMethod resourceMethod) {
        if (!resourceMethod.getProducedTypes().isEmpty()) {
            return resourceMethod.getProducedTypes();
        }
        List<MediaType> result = new LinkedList<>();
        if (workers != null) {
            final Invocable invocable = resourceMethod.getInvocable();
            result.addAll(workers.getMessageBodyWriterMediaTypes(
                    invocable.getRawResponseType(),
                    invocable.getResponseType(),
                    invocable.getHandlingMethod().getAnnotations()));
        }
        return result.isEmpty() ? StarTypeList : result;
    }

    private boolean sameHttpMethod(ResourceMethod m1, ResourceMethod m2) {
        return m1.getHttpMethod().equals(m2.getHttpMethod());
    }
}
