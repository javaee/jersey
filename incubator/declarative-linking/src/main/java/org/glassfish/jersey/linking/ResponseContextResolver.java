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

import java.beans.FeatureDescriptor;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.PropertyNotWritableException;

/**
 * The initial context resolver that resolves the entity and resource
 * objects used at the start of an EL expression.
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
class ResponseContextResolver extends ELResolver {

    private Map<String, Object> responseObjects;
    public static final String ENTITY_OBJECT = "entity";
    public static final String RESOURCE_OBJECT = "resource";
    public static final String INSTANCE_OBJECT = "instance";

    public ResponseContextResolver(Object entity, Object resource, Object instance) {
        responseObjects = new HashMap<>();
        responseObjects.put(ENTITY_OBJECT, entity);
        responseObjects.put(RESOURCE_OBJECT, resource);
        responseObjects.put(INSTANCE_OBJECT, instance);
    }

    private boolean isHandled(ELContext elc, Object base, Object property) {
        if (base != null) {
            return false;
        }
        if (responseObjects.containsKey(property.toString())) {
            elc.setPropertyResolved(true);
            return true;
        }
        return false;
    }

    @Override
    public Object getValue(ELContext elc, Object base, Object property) {
        if (isHandled(elc, base, property)) {
            return responseObjects.get(property.toString());
        }
        return null;
    }

    @Override
    public Class<?> getType(ELContext elc, Object o, Object o1) {
        if (isHandled(elc, o, o1)) {
            return getValue(elc, o, o1).getClass();
        }
        return null;
    }

    @Override
    public void setValue(ELContext elc, Object o, Object o1, Object o2) {
        throw new PropertyNotWritableException(o2.toString());
    }

    @Override
    public boolean isReadOnly(ELContext elc, Object o, Object o1) {
        if (isHandled(elc, o, o1)) {
            return true;
        }
        return false;
    }

    @Override
    public Iterator<FeatureDescriptor> getFeatureDescriptors(ELContext elc, Object o) {
        return null;
    }

    @Override
    public Class<?> getCommonPropertyType(ELContext elc, Object o) {
        return Object.class;
    }
}
