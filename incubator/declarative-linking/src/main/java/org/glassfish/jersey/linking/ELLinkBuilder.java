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

import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriInfo;

import javax.el.ExpressionFactory;
import javax.el.ValueExpression;

import org.glassfish.jersey.linking.mapping.ResourceMappingContext;
import org.glassfish.jersey.uri.internal.UriTemplateParser;

/**
 * A helper class to build links from EL expressions.
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
final class ELLinkBuilder {

    private ELLinkBuilder() {
    }

    private static final ExpressionFactory expressionFactory =
            ExpressionFactory.newInstance();

    /**
     * TODO javadoc.
     */
    static boolean evaluateCondition(String condition,
                                     Object entity,
                                     Object resource,
                                     Object instance) {

        if (condition == null || condition.isEmpty()) {
            return true;
        }
        LinkELContext context = new LinkELContext(entity, resource, instance);
        ValueExpression expr = expressionFactory.createValueExpression(context, condition, boolean.class);

        Object result = expr.getValue(context).toString();
        return "true".equals(result);
    }

    /**
     * TODO javadoc.
     */
    static URI buildURI(InjectLinkDescriptor link,
                        Object entity,
                        Object resource,
                        Object instance,
                        UriInfo uriInfo,
                        ResourceMappingContext rmc) {

        String template = link.getLinkTemplate(rmc);

        // first process any embedded EL expressions
        LinkELContext context = new LinkELContext(entity, resource, instance);
        ValueExpression expr = expressionFactory.createValueExpression(context,
                template, String.class);
        template = expr.getValue(context).toString();

        // now process any embedded URI template parameters
        UriBuilder ub = applyLinkStyle(template, link.getLinkStyle(), uriInfo);
        UriTemplateParser parser = new UriTemplateParser(template);
        List<String> parameterNames = parser.getNames();
        Map<String, Object> valueMap = getParameterValues(parameterNames, link, context, uriInfo);
        return ub.buildFromMap(valueMap);
    }

    private static UriBuilder applyLinkStyle(String template, InjectLink.Style style, UriInfo uriInfo) {
        UriBuilder ub = null;
        switch (style) {
            case ABSOLUTE:
                ub = uriInfo.getBaseUriBuilder().path(template);
                break;
            case ABSOLUTE_PATH:
                String basePath = uriInfo.getBaseUri().getPath();
                ub = UriBuilder.fromPath(basePath).path(template);
                break;
            case RELATIVE_PATH:
                ub = UriBuilder.fromPath(template);
                break;
        }
        return ub;
    }

    private static Map<String, Object> getParameterValues(List<String> parameterNames,
                                                          InjectLinkDescriptor linkField,
                                                          LinkELContext context,
                                                          UriInfo uriInfo) {
        Map<String, Object> values = new HashMap<>();
        for (String name : parameterNames) {
            String elExpression = linkField.getBinding(name);
            if (elExpression == null) {
                String value = uriInfo.getPathParameters().getFirst(name);
                if (value == null) {
                    value = uriInfo.getQueryParameters().getFirst(name);
                }
                if (value != null) {
                    values.put(name, value);
                    continue;
                }
                elExpression = "${" + ResponseContextResolver.INSTANCE_OBJECT + "." + name + "}";
            }
            ValueExpression expr = expressionFactory.createValueExpression(context,
                        elExpression, String.class);

            Object value = expr.getValue(context);
            values.put(name, value != null ? value.toString() : null);
         }
        return values;
    }

}
