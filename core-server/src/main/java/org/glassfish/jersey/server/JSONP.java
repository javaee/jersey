/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * This annotation can be used to define the JavaScript callback function name if the valid JSONP format is requested as an
 * acceptable {@link javax.ws.rs.core.MediaType media type} of this request. At the moment only resource methods should be
 * annotated with this annotation.
 * <p/>
 * The acceptable JavaScript media types for JSONP compatible with this annotation are:
 * <ul>
 * <li>application/x-javascript</li>
 * <li>application/javascript</li>
 * <li>application/ecmascript</li>
 * <li>text/javascript</li>
 * <li>text/x-javascript</li>
 * <li>text/ecmascript</li>
 * <li>text/jscript</li>
 * </ul>
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
@Target({ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface JSONP {

    /**
     * Name of the JavaScript callback function to which the JSON result should be wrapped into. If the {@code #isQueryParam}
     * value is {@code true} then the actual callback function name is determined from the query parameter name (defined by the
     * value of this property), otherwise the value of this property is used as the JavaScript callback function name.
     */
    String callback() default "callback";

    /**
     * A flag to determine whether the {@code #callback} should be treated as a query parameter name (present in {@link java
     * .net.URI}).
     */
    boolean isQueryParam() default false;
}
