/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.Collections;
import java.util.List;
import java.util.Map;

import org.glassfish.jersey.uri.PathTemplate;

import jersey.repackaged.com.google.common.collect.Lists;
import jersey.repackaged.com.google.common.collect.Maps;


/**
 * Runtime Resource model contains structured information about runtime resources.
 *
 * @author Miroslav Fuksa
 */
public class RuntimeResourceModel {

    private final List<RuntimeResource> runtimeResources;

    /**
     * Creates new runtime resource model from the list of resources.
     *
     * @param resources List of all resource that should be base for the model.
     */
    public RuntimeResourceModel(List<Resource> resources) {
        this.runtimeResources = Lists.newArrayList();
        for (RuntimeResource.Builder builder : getRuntimeResources(resources)) {
            runtimeResources.add(builder.build(null));
        }
        Collections.sort(runtimeResources, RuntimeResource.COMPARATOR);
    }

    private List<RuntimeResource.Builder> getRuntimeResources(List<Resource> resources) {
        Map<String, List<Resource>> regexMap = Maps.newHashMap();

        for (Resource resource : resources) {
            String path = resource.getPath();
            String regex = null;
            if (path != null) {
                if (path.endsWith("/")) {
                    path = path.substring(0, path.length() - 1);
                }
                regex = new PathTemplate(path).getPattern().getRegex();
            }

            List<Resource> listFromMap = regexMap.get(regex);
            if (listFromMap == null) {
                listFromMap = Lists.newArrayList();
                regexMap.put(regex, listFromMap);
            }
            listFromMap.add(resource);
        }

        List<RuntimeResource.Builder> runtimeResources = Lists.newArrayList();
        for (Map.Entry<String, List<Resource>> entry : regexMap.entrySet()) {
            final List<Resource> resourcesWithSameRegex = entry.getValue();

            List<Resource> childResources = Lists.newArrayList();
            for (final Resource res : resourcesWithSameRegex) {
                childResources.addAll(res.getChildResources());
            }

            List<RuntimeResource.Builder> childRuntimeResources = getRuntimeResources(childResources);
            runtimeResources.add(new RuntimeResource.Builder(resourcesWithSameRegex, childRuntimeResources, entry.getKey()));
        }
        return runtimeResources;
    }

    /**
     * Get list of runtime resources from this model (excluding child resources which are accessible in the
     * returned resources).
     *
     * @return List of runtime resources.
     */
    public List<RuntimeResource> getRuntimeResources() {
        return runtimeResources;
    }
}
