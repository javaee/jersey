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

package org.glassfish.jersey.spi;

import javax.ws.rs.ext.ExceptionMapper;

/**
 * Extension of a {@link ExceptionMapper exception mapper interface}. The exception mapping
 * providers can extend from this interface to add jersey specific functionality to these
 * providers.
 *
 * @author Miroslav Fuksa
 *
 * @param <T> A type of the exception processed by the exception mapper.
 */
public interface ExtendedExceptionMapper<T extends Throwable> extends ExceptionMapper<T> {
    /**
     * Determine whether this provider is able to process a supplied exception instance.
     * <p>
     * This method is called only on those exception mapping providers that are able to
     * process the type of the {@code exception} as defined by the JAX-RS
     * {@link ExceptionMapper} contract. By returning {@code false} this method can reject
     * any given exception instance and change the default JAX-RS exception mapper
     * selection behaviour.
     * </p>
     *
     * @param exception exception instance which should be processed.
     * @return {@code true} if the mapper is able to map the particular exception instance,
     *         {@code false} otherwise.
     */
    public boolean isMappable(T exception);
}
