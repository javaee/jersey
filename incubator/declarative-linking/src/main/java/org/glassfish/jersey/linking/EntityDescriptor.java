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

import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Link;

/**
 * Describes an entity in terms of its fields, bean properties and {@link InjectLink}
 * annotated fields.
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */

class EntityDescriptor {

    // Maintains an internal static cache to optimize processing

    private static final Map<Class<?>, EntityDescriptor> descriptors = new HashMap<>();

    public static synchronized EntityDescriptor getInstance(Class<?> entityClass) {
        if (descriptors.containsKey(entityClass)) {
            return descriptors.get(entityClass);
        } else {
            EntityDescriptor descriptor = new EntityDescriptor(entityClass);
            descriptors.put(entityClass, descriptor);
            return descriptor;
        }
    }

    // instance

    private Map<String, FieldDescriptor> nonLinkFields;
    private Map<String, FieldDescriptor> linkFields;
    private List<LinkHeaderDescriptor> linkHeaders;

    /**
     * Construct an new descriptor by inspecting the supplied class.
     * @param entityClass
     */
    private EntityDescriptor(Class<?> entityClass) {
        // create a list of link headers
        this.linkHeaders = new ArrayList<>();
        findLinkHeaders(entityClass);
        this.linkHeaders = Collections.unmodifiableList(linkHeaders);

        // create a list of field names
        this.nonLinkFields = new HashMap<>();
        this.linkFields = new HashMap<>();
        findFields(entityClass);
        this.nonLinkFields = Collections.unmodifiableMap(this.nonLinkFields);
        this.linkFields = Collections.unmodifiableMap(this.linkFields);
    }

    public Collection<FieldDescriptor> getLinkFields() {
        return linkFields.values();
    }

    public Collection<FieldDescriptor> getNonLinkFields() {
        return nonLinkFields.values();
    }

    public List<LinkHeaderDescriptor> getLinkHeaders() {
        return linkHeaders;
    }

    /**
     * Find and cache the fields of the supplied class and its superclasses and
     * interfaces.
     * @param entityClass the class
     */
    private void findFields(Class<?> entityClass) {
        for (Field f : entityClass.getDeclaredFields()) {
            InjectLink a = f.getAnnotation(InjectLink.class);
            Class<?> t = f.getType();
            if (a != null) {
                if (t.equals(String.class) || t.equals(URI.class) || Link.class.isAssignableFrom(t)) {
                    if (!linkFields.containsKey(f.getName())) {
                        linkFields.put(f.getName(), new InjectLinkFieldDescriptor(f, a, t));
                    }
                } else {
                    // TODO unsupported type
                }
            } else if (f.isAnnotationPresent(InjectLinks.class)) {

                if (List.class.isAssignableFrom(t)
                        || t.isArray() && Link.class.isAssignableFrom(t.getComponentType())) {

                    InjectLinks a2 = f.getAnnotation(InjectLinks.class);
                    linkFields.put(f.getName(), new InjectLinksFieldDescriptor(f, a2, t));
                } else {
                    throw new IllegalArgumentException("Can only inject links onto a List<Link> or Link[] object");
                }

            } else {
                // see issue http://java.net/jira/browse/JERSEY-625
                if ((f.getModifiers() & Modifier.STATIC) > 0
                        || f.getName().startsWith("java.")
                        || f.getName().startsWith("javax.")) {
                    continue;
                }
                nonLinkFields.put(f.getName(), new FieldDescriptor(f));
            }
        }

        // look for nonLinkFields in superclasses
        Class<?> sc = entityClass.getSuperclass();
        if (sc != null && sc != Object.class) {
            findFields(sc);
        }

        // look for nonLinkFields in interfaces
        for (Class<?> ic : entityClass.getInterfaces()) {
            findFields(ic);
        }
    }

    private void findLinkHeaders(Class<?> entityClass) {
        InjectLink linkHeaderAnnotation = entityClass.getAnnotation(InjectLink.class);
        if (linkHeaderAnnotation != null) {
            linkHeaders.add(new LinkHeaderDescriptor(linkHeaderAnnotation));
        }
        InjectLinks linkHeadersAnnotation = entityClass.getAnnotation(InjectLinks.class);
        if (linkHeadersAnnotation != null) {
            for (InjectLink linkHeader : linkHeadersAnnotation.value()) {
                linkHeaders.add(new LinkHeaderDescriptor(linkHeader));
            }
        }

        // look in superclasses
        Class<?> sc = entityClass.getSuperclass();
        if (sc != null && sc != Object.class) {
            findLinkHeaders(sc);
        }

        // look in interfaces
        for (Class<?> ic : entityClass.getInterfaces()) {
            findLinkHeaders(ic);
        }
    }
}
