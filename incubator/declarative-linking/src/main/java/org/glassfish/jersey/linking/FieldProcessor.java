/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2016 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.logging.Level;
import java.util.logging.Logger;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.UriInfo;
import javax.xml.bind.annotation.XmlTransient;

import org.glassfish.jersey.linking.mapping.ResourceMappingContext;

/**
 * Utility class that can inject links into {@link org.glassfish.jersey.linking.InjectLink} annotated fields in
 * an entity.
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
class FieldProcessor<T> {

    private EntityDescriptor instanceDescriptor;
    private static final Logger log = Logger.getLogger(FieldProcessor.class.getName());

    public FieldProcessor(Class<T> c) {
        instanceDescriptor = EntityDescriptor.getInstance(c);
    }

    /**
     * Inject any {@link org.glassfish.jersey.linking.InjectLink} annotated fields in the supplied entity and
     * recursively process its fields.
     * @param entity the entity object returned by the resource method
     * @param uriInfo the uriInfo for the request
     */
    public void processLinks(T entity, UriInfo uriInfo, ResourceMappingContext rmc) {
        Set<Object> processed = new HashSet<Object>();
        Object resource = uriInfo.getMatchedResources().get(0);
        processLinks(entity, resource, entity, processed, uriInfo, rmc);
    }

    /**
     * Inject any {@link org.glassfish.jersey.linking.InjectLink} annotated fields in the supplied instance. Called
     * once for the entity and then recursively for each member and field.
     * @param entity
     * @param processed a list of already processed objects, used to break
     * recursion when processing circular references.
     * @param uriInfo
     */
    private void processLinks(Object entity, Object resource, Object instance,
                              Set<Object> processed, UriInfo uriInfo,
                              ResourceMappingContext rmc) {

        try {
            if (instance == null || processed.contains(instance)) {
                return; // ignore null properties and defeat circular references
            }
            if (instance.getClass().getName().startsWith("java.lang")) {
                return;
            }
            processed.add(instance);
        } catch (RuntimeException e) {
            // fix for JERSEY-1656
            log.log(Level.INFO, LinkMessages.WARNING_LINKFILTER_PROCESSING(instance.getClass().getName()), e);
        }

        // Process any @Link annotated fields in entity
        for (FieldDescriptor field : instanceDescriptor.getLinkFields()) {

            // TODO replace with properly poly-morphic code
            if (field instanceof InjectLinkFieldDescriptor) {
                InjectLinkFieldDescriptor linkField = (InjectLinkFieldDescriptor) field;
                if (ELLinkBuilder.evaluateCondition(linkField.getCondition(), entity, resource, instance)) {
                    URI uri = ELLinkBuilder.buildURI(linkField, entity, resource, instance, uriInfo, rmc);
                    linkField.setPropertyValue(instance, uri);
                }
            } else if (field instanceof InjectLinksFieldDescriptor) {

                InjectLinksFieldDescriptor linksField = (InjectLinksFieldDescriptor) field;
                List<Link> list = new ArrayList<Link>();
                for (InjectLinkFieldDescriptor linkField : linksField.getLinksToInject()) {
                    if (ELLinkBuilder.evaluateCondition(linkField.getCondition(), entity, resource, instance)) {
                       URI uri = ELLinkBuilder.buildURI(linkField, entity, resource, instance, uriInfo, rmc);
                       Link link = linkField.getLink(uri);
                       list.add(link);
                    }
                }

                linksField.setPropertyValue(instance, list);
            }
        }

        // If entity is an array, collection, or map then process members
        Class<?> instanceClass = instance.getClass();
        if (instanceClass.isArray() && Object[].class.isAssignableFrom(instanceClass)) {
            Object array[] = (Object[]) instance;
            for (Object member : array) {
                processMember(entity, resource, member, processed, uriInfo, rmc);
            }
        } else if (instance instanceof Iterable) {
            Iterable iterable = (Iterable) instance;
            for (Object member : iterable) {
                processMember(entity, resource, member, processed, uriInfo, rmc);
            }
        } else if (instance instanceof Map) {
            Map map = (Map) instance;
            for (Object member : map.entrySet()) {
                processMember(entity, resource, member, processed, uriInfo, rmc);
            }
        }

        // Recursively process all member fields
        for (FieldDescriptor member : instanceDescriptor.getNonLinkFields()) {

            if (fieldSuitableForIntrospection(member)) {
                processMember(entity, resource, member.getFieldValue(instance), processed, uriInfo, rmc);
            }
        }

    }

    private boolean fieldSuitableForIntrospection(FieldDescriptor member) {
        return member.field == null
                || (!member.field.isSynthetic()
                    && !Modifier.isTransient(member.field.getModifiers())
                    && !member.field.getType().isPrimitive()
                    && member.field.getType() != String.class
                    && !member.field.isAnnotationPresent(InjectLinkNoFollow.class)
                    && !member.field.isAnnotationPresent(XmlTransient.class));
    }

    private void processMember(Object entity, Object resource, Object member, Set<Object> processed, UriInfo uriInfo,
      ResourceMappingContext rmc) {
        if (member != null) {
            FieldProcessor proc = new FieldProcessor(member.getClass());
            proc.processLinks(entity, resource, member, processed, uriInfo, rmc);
        }
    }

}
