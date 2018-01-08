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

package org.glassfish.jersey.server.mvc;

import java.lang.annotation.ElementType;
import java.lang.annotation.Inherited;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Used to annotate JAX-RS resources and resource methods to provide reference to a template for MVC support.
 * <p/>
 * In case a resource class is annotated with {@link Template} annotation then an instance of this class is considered to be
 * the model. Producible {@link javax.ws.rs.core.MediaType media types} are determined from the resource classes
 * {@link javax.ws.rs.Produces} annotation.
 * <p/>
 * In case a resource method is annotated with {@link Template} annotation then the return value of the method is the model.
 * Otherwise the processing of such a method is the same as if the  return type of the method was {@link Viewable} class.
 * Producible {@link javax.ws.rs.core.MediaType media types} are determined from the method's {@link javax.ws.rs.Produces}
 * annotation.
 * <p/>
 * To see how templates are being resolved, see {@link Viewable viewable}.
 *
 * @author Michal Gajdos
 * @see Viewable
 */
@Inherited
@Target({ElementType.TYPE, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
public @interface Template {

    /**
     * The template name that should be used to output the entity. The template name may be declared as absolute template name
     * if the name begins with a '/', otherwise the template name is declared as a relative template name.
     */
    String name() default "";
}
