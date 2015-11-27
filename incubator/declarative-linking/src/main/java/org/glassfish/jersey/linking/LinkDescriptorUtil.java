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

package org.glassfish.jersey.linking;

import java.lang.annotation.Annotation;
import java.util.Iterator;

import javax.ws.rs.HttpMethod;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;

import org.glassfish.jersey.linking.mapping.ResourceMappingContext;
import org.glassfish.jersey.server.model.AnnotatedMethod;
import org.glassfish.jersey.server.model.MethodList;

/**
 * 
 * @author Ryan Peterson
 *
 */
public class LinkDescriptorUtil {
    /**
     * TODO javadoc.
     */
    public static String getLinkTemplate(ResourceMappingContext rmc, InjectLink link) {
        String template = null;
        if (!link.resource().equals(Class.class)) {


            ResourceMappingContext.Mapping map = rmc.getMapping(link.resource());
            if (map != null) {
                template = map.getTemplate().getTemplate().toString();
            } else {
                // extract template from specified class' @Path annotation
                Path path = link.resource().getAnnotation(Path.class);
                template = path == null ? "" : path.value();
            }

            // extract template from specified class' @Path annotation
            if (link.method().length() > 0) {
                // append value of method's @Path annotation
                MethodList methods = new MethodList(link.resource());
                methods = methods.withMetaAnnotation(HttpMethod.class);
                Iterator<AnnotatedMethod> iterator = methods.iterator();
                while (iterator.hasNext()) {
                    AnnotatedMethod method = iterator.next();
                    if (!method.getMethod().getName().equals(link.method())) {
                        continue;
                    }
                    StringBuilder builder = new StringBuilder();
                    builder.append(template);

                    Path methodPath = method.getAnnotation(Path.class);
                    if (methodPath != null) {
                        String methodTemplate = methodPath.value();

                        if (!(template.endsWith("/") || methodTemplate.startsWith("/"))) {
                            builder.append("/");
                        }
                        builder.append(methodTemplate);
                    }

                    // append query parameters
                    StringBuilder querySubString = new StringBuilder();
                    for (Annotation paramAnns[] : method.getParameterAnnotations()) {
                        for (Annotation ann : paramAnns) {
                            if (ann.annotationType() == QueryParam.class) {
                                querySubString.append(((QueryParam) ann).value());
                                querySubString.append(',');
                            }
                        }
                    }

                    if (querySubString.length() > 0) {
                        builder.append("{?");
                        builder.append(querySubString.subSequence(0, querySubString.length() - 1));
                        builder.append("}");
                    }

                    template = builder.toString();
                    break;
                }
            }
        } else {
            template = link.value();
        }

        return template;
    }
}
