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

package org.glassfish.jersey.linking;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;
import javax.ws.rs.Path;
import javax.ws.rs.core.Link;
import org.glassfish.jersey.server.model.AnnotatedMethod;
  
import org.glassfish.jersey.server.model.MethodList;
  
/**
 * Utility class for working with {@link InjectLink} annotated fields
 * 
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
class InjectLinkFieldDescriptor extends FieldDescriptor implements InjectLinkDescriptor {

    private InjectLink link;
    private Class<?> type;
    private Map<String, String> bindings;

    public InjectLinkFieldDescriptor(Field f, InjectLink l, Class<?> t) {
        super(f);
        link = l;
        type = t;
        bindings = new HashMap<String, String>();
        for (Binding binding: l.bindings()) {
            bindings.put(binding.name(), binding.value());
        }
    }
    
    public void setPropertyValue(Object instance, URI uri) {
        setAccessibleField(field);
        try {
            
            Object value = null;
            if (URI.class.equals(type)) {
                value = uri;
            }
            else if (Link.class.isAssignableFrom(type)) {

                // Make a link with the correct bindings
                //
                
                value = getLink(uri);
            }
            else if (String.class.equals(type)) {
                value = uri.toString();
            }
            else  {
                throw new IllegalArgumentException("Field type "  + type + " not one of supported String,URI and Link");
            }
            
            field.set(instance, value);
            
            
            
            
        } catch (IllegalArgumentException ex) {
            Logger.getLogger(InjectLinkFieldDescriptor.class.getName()).log(Level.SEVERE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(InjectLinkFieldDescriptor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    public InjectLink.Style getLinkStyle() {
        return link.style();
    }

    public String getLinkTemplate() {
        return getLinkTemplate(link);
    }

    public static String getLinkTemplate(InjectLink link) {
        String template = null;
        if (!link.resource().equals(Class.class)) {
            // extract template from specified class' @Path annotation
            Path path = link.resource().getAnnotation(Path.class);
            template = path==null ? "" : path.value(); 
            if (link.method().length() > 0) { 
                // append value of method's @Path annotation
                MethodList methods = new MethodList(link.resource());
                methods = methods.withAnnotation(Path.class);
                Iterator<AnnotatedMethod> iterator = methods.iterator();
                while (iterator.hasNext()) {
                    AnnotatedMethod method = iterator.next();
                    if (!method.getMethod().getName().equals(link.method()))
                        continue;
                    Path methodPath = method.getAnnotation(Path.class);
                    String methodTemplate = methodPath.value();
                    StringBuilder builder = new StringBuilder();
                    builder.append(template);
                    if (!(template.endsWith("/") || methodTemplate.startsWith("/")))
                        builder.append("/");
                    builder.append(methodTemplate);
                    template = builder.toString();
                    break;
                }
            }
        } else {
            template = link.value();
        }
        return template;
    }
    
    public Link getLink(URI uri) {
        return InjectLink.Util.buildLinkFromUri(uri, link);
    }
    
    public String getBinding(String name) {
        return bindings.get(name);
    }

    public String getCondition() {
        return link.condition();
    }
}
