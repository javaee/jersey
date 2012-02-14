/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

import javax.annotation.Nullable;
import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

/**
 * Abstraction for resource class
 */
public class ResourceClass implements PathAnnotated, ResourceModelComponent {

    private final Class<?> resourceClass;
    private final PathValue uriPath;
    private final List<ResourceConstructor> constructors;
    private final List<AbstractResourceMethod> resourceMethods;
    private final List<SubResourceMethod> subResourceMethods;
    private final List<SubResourceLocator> subResourceLocators;

    /**
     * Creates a new instance of {@link ResourceClass}.
     *
     * @param resourceClass resource class.
     */
    public ResourceClass(Class<?> resourceClass) {
        this(resourceClass, null);
    }

    /**
     * Creates a new instance of ResourceClass.
     *
     * @param resourceClass resource class.
     * @param uriPath path associated with created resource.
     */
    public ResourceClass(Class<?> resourceClass, @Nullable PathValue uriPath) {
        this.resourceClass = resourceClass;
        this.uriPath = uriPath;
        this.constructors = new ArrayList<ResourceConstructor>(4);
        this.resourceMethods = new ArrayList<AbstractResourceMethod>(4);
        this.subResourceLocators = new ArrayList<SubResourceLocator>(4);
        this.subResourceMethods = new ArrayList<SubResourceMethod>(4);
    }

    /**
     * Create a new instance from an existing ResourceClass but
     * defining a new path.
     *
     * @param path the path.
     * @param ar the existing abstract resource.
     */
    public ResourceClass(String path, ResourceClass ar) {
        this.uriPath = new PathValue(path);

        this.resourceClass = ar.resourceClass;
        this.constructors = ar.constructors;
        this.resourceMethods = ar.resourceMethods;
        this.subResourceMethods = ar.subResourceMethods;
        this.subResourceLocators = ar.subResourceLocators;
    }

    public Class<?> getResourceClass() {
        return resourceClass;
    }

    public boolean isSubResource() {
        return uriPath == null;
    }

    public boolean isRootResource() {
        return uriPath != null;
    }

    @Override
    public PathValue getPath() {
        return uriPath;
    }

    public List<ResourceConstructor> getConstructors() {
        return constructors;
    }

    /**
     * Provides a non-null list of resource methods available on the resource
     *
     * @return non-null abstract resource method list
     */
    public List<AbstractResourceMethod> getResourceMethods() {
        return resourceMethods;
    }

    /**
     * Provides a non-null list of subresource methods available on the resource
     *
     * @return non-null abstract subresource method list
     */
    public List<SubResourceMethod> getSubResourceMethods() {
        return subResourceMethods;
    }

    /**
     * Provides a non-null list of subresource locators available on the resource
     *
     * @return non-null abstract subresource locator list
     */
    public List<SubResourceLocator> getSubResourceLocators() {
        return subResourceLocators;
    }

    @Override
    public void accept(ResourceModelVisitor visitor) {
        visitor.visitResourceClass(this);
    }

    @Override
    public String toString() {
        return "Resource("
                + ((null == getPath()) ? "" : ("\"" + getPath().getValue() + "\", - "))
                + ((getResourceClass() != null) ? getResourceClass().getSimpleName() : "<virtualResourceClass>") + ": "
                + getConstructors().size() + " constructors, "
                + getResourceMethods().size() + " res methods, "
                + getSubResourceMethods().size() + " subres methods, "
                + getSubResourceLocators().size() + " subres locators " + ")";
    }

    @Override
    public List<ResourceModelComponent> getComponents() {
        List<ResourceModelComponent> components = new LinkedList<ResourceModelComponent>();
        components.addAll(getConstructors());
        components.addAll(getResourceMethods());
        components.addAll(getSubResourceMethods());
        components.addAll(getSubResourceLocators());
        return components;
    }
}
