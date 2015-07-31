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

package org.glassfish.jersey.server.model.internal;

import javax.ws.rs.Path;

/**
 * Common model helper methods.
 *
 * @author Michal Gajdos
 * @author Constantino Cronemberger (ccronemberger at yahoo.com.br)
 */
public final class ModelHelper {

    /**
     * Get the class in the provided resource class ancestor hierarchy that
     * is actually annotated with the {@link javax.ws.rs.Path &#64;Path} annotation.
     *
     * @param resourceClass resource class.
     * @return resource class or it's ancestor that is annotated with the {@link javax.ws.rs.Path &#64;Path}
     *         annotation.
     */
    public static Class<?> getAnnotatedResourceClass(final Class<?> resourceClass) {

        Class<?> foundInterface = null;

        // traverse the class hierarchy to find the annotation
        // According to specification, annotation in the super-classes must take precedence over annotation in the
        // implemented interfaces
        Class<?> cls = resourceClass;
        do {
            if (cls.isAnnotationPresent(Path.class)) {
                return cls;
            }

            // if no annotation found on the class currently traversed, check for annotation in the interfaces on this
            // level - if not already previously found
            if (foundInterface == null) {
                for (final Class<?> i : cls.getInterfaces()) {
                    if (i.isAnnotationPresent(Path.class)) {
                        // store the interface reference in case no annotation will be found in the super-classes
                        foundInterface = i;
                        break;
                    }
                }
            }
        } while ((cls = cls.getSuperclass()) != null);

        if (foundInterface != null) {
            return foundInterface;
        }

        return resourceClass;
    }

    /**
     * Prevent instantiation.
     */
    private ModelHelper() {
        throw new AssertionError("Instantiation not allowed.");
    }
}
