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

package org.glassfish.jersey.media.linking.internal;

import org.glassfish.jersey.media.linking.LinkHeader;
import org.glassfish.jersey.media.linking.LinkHeaders;
import org.glassfish.jersey.media.linking.Ref;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.net.URI;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Describes an entity in terms of its fields, bean properties and {@link Ref}
 * annotated fields.
 * @author mh124079
 */
public class EntityDescriptor {

    // Maintains an internal static cache to optimize processing

    private static Map<Class<?>, EntityDescriptor> descriptors
        = new HashMap<Class<?>, EntityDescriptor>();

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
    private Map<String, RefFieldDescriptor> linkFields;
    private List<LinkDescriptor> linkHeaders;

    /**
     * Construct an new descriptor by inspecting the supplied class.
     * @param entityClass
     */
    private EntityDescriptor(Class<?> entityClass) {
        // create a list of link headers
        this.linkHeaders = new ArrayList<LinkDescriptor>();
        findLinkHeaders(entityClass);
        this.linkHeaders = Collections.unmodifiableList(linkHeaders);

        // create a list of field names
        this.nonLinkFields = new HashMap<String, FieldDescriptor>();
        this.linkFields = new HashMap<String, RefFieldDescriptor>();
        findFields(entityClass);
        this.nonLinkFields = Collections.unmodifiableMap(this.nonLinkFields);
        this.linkFields = Collections.unmodifiableMap(this.linkFields);
    }

    public Collection<RefFieldDescriptor> getLinkFields() {
        return linkFields.values();
    }

    public Collection<FieldDescriptor> getNonLinkFields() {
        return nonLinkFields.values();
    }

    public List<LinkDescriptor> getLinkHeaders() {
        return linkHeaders;
    }

    /**
     * Find and cache the fields of the supplied class and its superclasses and
     * interfaces.
     * @param entityClass the class
     */
    private void findFields(Class<?> entityClass) {
        for (Field f: entityClass.getDeclaredFields()) {
            Ref a = f.getAnnotation(Ref.class);
            if (a != null) {
                Class<?> t = f.getType();
                if (t.equals(String.class) || t.equals(URI.class)) {
                    if (!linkFields.containsKey(f.getName())) {
                        linkFields.put(f.getName(), new RefFieldDescriptor(f, a, t));
                    }
                } else {
                    // TODO unsupported type
                }
            } else {
                // see issue http://java.net/jira/browse/JERSEY-625
                if(((f.getModifiers() & Modifier.STATIC) > 0) ||
                        f.getName().startsWith("java.") ||
                        f.getName().startsWith("javax.")
                        )
                    continue;
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
        LinkHeader linkHeaderAnnotation = entityClass.getAnnotation(LinkHeader.class);
        if (linkHeaderAnnotation != null) {
            linkHeaders.add(new LinkDescriptor(linkHeaderAnnotation));
        }
        LinkHeaders linkHeadersAnnotation = entityClass.getAnnotation(LinkHeaders.class);
        if (linkHeadersAnnotation != null) {
            for (LinkHeader linkHeader: linkHeadersAnnotation.value()) {
                linkHeaders.add(new LinkDescriptor(linkHeader));
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
