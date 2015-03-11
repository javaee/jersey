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

import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Utils for {@link Resource resource} testing.
 *
 * @author Miroslav Fuksa
 *
 */
public class ResourceTestUtils {

    public static void containsExactMethods(Resource resource, boolean shouldContainLocator, String... httpMethods) {
        assertEquals(shouldContainLocator, resource.getResourceLocator() != null);
        for (String httpMethod : httpMethods) {
            containsMethod(resource, httpMethod);
        }
        assertEquals(httpMethods.length, resource.getResourceMethods().size());
    }

    public static void containsMethod(Resource resource, String httpMethod) {
        for (ResourceMethod method : resource.getResourceMethods()) {
            if (method.getHttpMethod().equals(httpMethod)) {
                return;
            }
        }
        fail("Resource " + resource + " does not contain resource method " + httpMethod + "!");
    }

    public static Resource getResource(List<Resource> resources, String path) {
        for (Resource resource : resources) {
            if (resource.getPath().equals(path)) {
                return resource;
            }
        }
        fail("Resource with path '" + path + "' is not in the list of resources " + resources + "!");
        return null;
    }


    public static RuntimeResource getRuntimeResource(List<RuntimeResource> resources, String regex) {
        for (RuntimeResource resource : resources) {
            if (resource.getRegex().equals(regex)) {
                return resource;
            }
        }
        fail("RuntimeResource with regex '" + regex + "' is not in the list of runtime resources " + resources + "!");
        return null;
    }

    public static void containsExactMethods(RuntimeResource resource, boolean shouldContainLocator, String... httpMethods) {
        assertEquals(shouldContainLocator, resource.getResourceLocators().size() == 1);
        for (String httpMethod : httpMethods) {
            containsMethod(resource, httpMethod);
        }
        assertEquals(httpMethods.length, resource.getResourceMethods().size());
    }

    public static void containsMethod(RuntimeResource resource, String httpMethod) {
        for (ResourceMethod method : resource.getResourceMethods()) {
            if (method.getHttpMethod().equals(httpMethod)) {
                return;
            }
        }
        fail("RuntimeResource " + resource + " does not contain resource method " + httpMethod + "!");
    }


}
