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

package org.glassfish.jersey.spi;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;


/**
 * Specifies a singleton lifecycle for a resource annotated with this annotation.
 * <p>
 * By default, the resource classes are managed in a {@link PerLookup per lookup}
 * scope which means that a new instance is created for each request. Annotating
 * the class with the {@code &#64;Singleton} annotation overrides this behaviour
 * in a sense that only one single instance is used to handle all the requests.
 * Such resources should be implemented as thread safe and must not inject
 * non-proxiable request-scoped components (e.g. {@link javax.ws.rs.HeaderParam})
 * into class fields or constructors.
 * </p>
 * <p>
 * For example, the following resource will not pass the model validation:
 * </p>
 * <pre>
 *  &#064;Singeton
 *  public class SingletonResource {
 *      // Injection of path param into a field of a singleton resource
 *      // is not allowed. Model validation error will occur.
 *      &#064;PathParam("p")
 *      String pParam;
 *
 *      &#064;GET
 *      public String get() {
 *          return "GET";
 *      }
 *  }
 * </pre>
 */
@Target({ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Singleton {
}
