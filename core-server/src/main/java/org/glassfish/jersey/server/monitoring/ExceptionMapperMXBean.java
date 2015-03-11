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

import java.util.Map;

/**
 * MXBean interface of the {@link javax.ws.rs.ext.ExceptionMapper exception mapper} statistics.
 *
 * @author Miroslav Fuksa
 */
public interface ExceptionMapperMXBean {

    /**
     * Get the statistics of execution of exception mappers.
     *
     * @return Map where keys are string class names of {@link javax.ws.rs.ext.ExceptionMapper exception mappers}
     *         and values are counts of execution of these mappers.
     */
    public Map<String, Long> getExceptionMapperCount();

    /**
     * Get count of all successful exception mappings. Successful exception mapping occurs when
     * any {@link javax.ws.rs.ext.ExceptionMapper exception mapper} returns an valid response
     * (even if response contains non-successful response status code).
     *
     * @return Count of successfully mapped exception.
     */
    public long getSuccessfulMappings();

    /**
     * Get count of all unsuccessful exception mappings. Unsuccessful exception mapping occurs when
     * any exception mapping process does not produce an valid response. The reason can be that the
     * {@link javax.ws.rs.ext.ExceptionMapper exception mapper} is not found, or is found but throws
     * exception.
     *
     * @return Count of unmapped exception.
     */
    public long getUnsuccessfulMappings();

    /**
     * Get count of exception mappings that were performed on exceptions.
     *
     * @return Count of all exception being mapped in the runtime.
     */
    public long getTotalMappings();


}
