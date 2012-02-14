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

import javax.ws.rs.core.MediaType;
import java.util.ArrayList;
import java.util.List;

/**
 * Abstraction for a resource method defined by a HTTP method and consumed/produced media type list.
 *
 * @see ResourceClass
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public abstract class AbstractResourceMethod implements ResourceModelComponent, ConsumesProducesEnabledComponent {

    private ResourceClass resource;
    private String httpMethod;
    private List<MediaType> consumeMimeList;
    private List<MediaType> produceMimeList;
    private boolean isConsumesDeclared;
    private boolean isProducesDeclared;

    /**
     * Constructs a new resource method associated with given resource.
     * You need to add the new instance to the list of resource methods manually.
     *
     * @param resource resource class where the new resource method should belong to
     * @param httpMethod corresponding HTTP method (e.g. "GET", "PUT", etc.)
     */
    public AbstractResourceMethod(
            ResourceClass resource,
            String httpMethod) {

        this.resource = resource;
        this.httpMethod = httpMethod.toUpperCase();
        this.consumeMimeList = new ArrayList<MediaType>();
        this.produceMimeList = new ArrayList<MediaType>();
    }

    /**
     * Enclosing resource class getter.
     *
     * @return Corresponding resource class
     */
    public ResourceClass getDeclaringResource() {
        return resource;
    }

    /**
     * Getter for consumed media types.
     *
     * @see #areInputTypesDeclared()
     *
     * @return list of consumed media types
     */
    @Override
    public List<MediaType> getSupportedInputTypes() {
        return consumeMimeList;
    }

    /**
     *
     * @param declared
     */
    @Override
    public void setAreInputTypesDeclared(boolean declared) {
        isConsumesDeclared = declared;
    }

    @Override
    public boolean areInputTypesDeclared() {
        return isConsumesDeclared;
    }

    @Override
    public List<MediaType> getSupportedOutputTypes() {
        return produceMimeList;
    }

    @Override
    public void setAreOutputTypesDeclared(boolean declared) {
        isProducesDeclared = declared;
    }

    @Override
    public boolean areOutputTypesDeclared() {
        return isProducesDeclared;
    }

    public String getHttpMethod() {
        return httpMethod;
    }

    // ResourceModelComponent
    @Override
    public List<ResourceModelComponent> getComponents() {
        return null;
    }

    @Override
    public String toString() {
        return "GenericResourceMethod("
                + resource.toString() + "#" + httpMethod + ")";
    }
}
