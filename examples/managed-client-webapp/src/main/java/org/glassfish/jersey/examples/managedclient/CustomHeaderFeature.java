/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.managedclient;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.container.DynamicFeature;
import javax.ws.rs.container.ResourceInfo;
import javax.ws.rs.core.FeatureContext;

/**
 * Dynamic feature that appends a properly configured {@link CustomHeaderFilter} instance
 * to every method that is annotated with {@link org.glassfish.jersey.examples.managedclient.CustomHeaderFeature.Require &#64;Require} internal feature
 * annotation.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class CustomHeaderFeature implements DynamicFeature {

    /**
     * A method annotation to be placed on those resource methods to which a validating
     * {@link CustomHeaderFilter} instance should be added.
     */
    @Retention(RetentionPolicy.RUNTIME)
    @Documented
    @Target(ElementType.METHOD)
    public static @interface Require {
        /**
         * Expected custom header name to be validated by the {@link CustomHeaderFilter}.
         */
        public String headerName();

        /**
         * Expected custom header value to be validated by the {@link CustomHeaderFilter}.
         */
        public String headerValue();
    }

    @Override
    public void configure(ResourceInfo resourceInfo, FeatureContext context) {
        final Require va = resourceInfo.getResourceMethod().getAnnotation(Require.class);
        if (va != null) {
            context.register(new CustomHeaderFilter(va.headerName(), va.headerValue()));
        }
    }
}
