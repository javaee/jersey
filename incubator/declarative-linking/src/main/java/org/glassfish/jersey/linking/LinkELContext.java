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

import javax.el.BeanELResolver;
import javax.el.CompositeELResolver;
import javax.el.ELContext;
import javax.el.ELResolver;
import javax.el.FunctionMapper;
import javax.el.VariableMapper;

/**
 * An ELContext that encapsulates the response information for use by the
 * expression evaluator.
 *
 *
 * @author Mark Hadley
 * @author Gerard Davison (gerard.davison at oracle.com)
 */
class LinkELContext extends ELContext {

    private Object entity;
    private Object resource;
    private Object instance;

    /**
     * Convenience constructor for the common case where a context where
     * the entity and instance are the same. Equivalent to
     * <code>LinkELContext(entity, resource, entity)</code>
     *
     * @param entity
     * @param resource
     */
    public LinkELContext(Object entity, Object resource) {
        this.entity = entity;
        this.resource = resource;
        this.instance = entity;
    }

    /**
     * Construct a new context
     * @param entity the entity returned from the resource method
     * @param resource the resource class instance that returned the entity
     * @param instance the instance that contains the entity, e.g. the value of
     * a field within an entity class.
     */
    public LinkELContext(Object entity, Object resource, Object instance) {
        this.entity = entity;
        this.resource = resource;
        this.instance = instance;
    }

    @Override
    public ELResolver getELResolver() {
        CompositeELResolver resolver = new CompositeELResolver();
        resolver.add(new ResponseContextResolver(entity, resource, instance));
        resolver.add(new BeanELResolver(true));
        return resolver;
    }

    @Override
    public FunctionMapper getFunctionMapper() {
        return null;
    }

    @Override
    public VariableMapper getVariableMapper() {
        return null;
    }

}
