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

package org.glassfish.jersey.server.monitoring;

/**
 * MXBean interface of resource method MXBeans.
 *
 * @author Miroslav Fuksa
 */
public interface ResourceMethodMXBean {

    /**
     * Get the name of the Java method.
     *
     * @return Name of method.
     */
    public String getMethodName();

    /**
     * Get the sub resource method path of the method. This field is non-null only for
     * sub resource methods and contains path relative to resource in which the method is defined.
     *
     * @return Sub resource method path or null if the method is not a sub resource method.
     */
    public String getPath();

    /**
     * Get the HTTP method of the method.
     *
     * @return HTTP method (e.g. GET, POST, ...)
     */
    public String getHttpMethod();

    /**
     * Get the full class name of the class that declares the handling method.
     *
     * @return Full class name.
     */
    public String getDeclaringClassName();

    /**
     * Get the string with media types consumed by this method, enclosed in double quotas and
     * separated by a comma (e.g. "text/plain","text/html").
     *
     * @return Consumed media types.
     */
    public String getConsumesMediaType();

    /**
     * Get the string with media types produced by this method, enclosed in double quotas and
     * separated by a comma (e.g. "text/plain","text/html").
     *
     * @return Produced media types.
     */
    public String getProducesMediaType();

}
