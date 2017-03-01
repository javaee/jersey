/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2017 Oracle and/or its affiliates. All rights reserved.
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

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.List;
import java.util.Objects;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Link;

/**
 * Utility class for working with {@link InjectLinks} annotated fields.
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
class InjectLinksFieldDescriptor extends FieldDescriptor {

    private final InjectLinks link;
    private final Class<?> type;

    /**
     * C'tor
     *
     * @param f the field to inject
     * @param l the InjectLinks annotation
     * @param t the class that contains field f
     */
    InjectLinksFieldDescriptor(Field f, InjectLinks l, Class<?> t) {
        super(f);
        link = l;
        type = t;
    }

    /**
     * Injects the Link list into the instance.
     *
     * If the field is {@code null} then it is replaced with the list.
     * If the field already contains links, then the content is merged
     * with this list into a new list and injected.
     *
     * @param instance the instance that contains the field f
     * @param list the list of links to inject
     */
    public void setPropertyValue(Object instance, List<Link> list) {
        setAccessibleField(field);
        try {
            List<Link> merged = mergeWithExistingField(instance, list);

            Object value;
            if (Objects.equals(List.class, type)) {
                value = merged;
            } else if (type.isArray()) {
                value = merged.toArray((Object[]) Array.newInstance(type.getComponentType(), merged.size()));
            } else {
                throw new IllegalArgumentException("Field type " + type + " not one of supported List<Link> or Link[]");
            }

            field.set(instance, value);


        } catch (IllegalArgumentException | IllegalAccessException ex) {
            Logger.getLogger(InjectLinksFieldDescriptor.class.getName()).log(Level.SEVERE, null, ex);
        }
    }

    private List<Link> mergeWithExistingField(Object instance, List<Link> list) throws IllegalAccessException {
        Object existing = field.get(instance);
        if (existing != null) {
            if (Collection.class.isAssignableFrom(existing.getClass()) && !((Collection) existing).isEmpty()) {
                List<Link> merged  = new ArrayList<>(list);
                merged.addAll((Collection<Link>) existing);
                return merged;
            } else if (existing.getClass().isArray() && existing.getClass().isAssignableFrom(Link[].class)) {
                List<Link> merged = new ArrayList<>(list);
                merged.addAll(Arrays.asList((Link[]) existing));
                return merged;
            }
        }
        return list;
    }

    /**
     * Creates {@link InjectLinkFieldDescriptor} for each link to inject.
     */
    InjectLinkFieldDescriptor[] getLinksToInject() {
        final InjectLink[] listOfLinks = link.value();
        InjectLinkFieldDescriptor[] fields = new InjectLinkFieldDescriptor[listOfLinks.length];
        for (int i = 0; i < fields.length; i++) {
            fields[i] = new InjectLinkFieldDescriptor(field, listOfLinks[i], Link.class);
        }
        return fields;
    }
}
