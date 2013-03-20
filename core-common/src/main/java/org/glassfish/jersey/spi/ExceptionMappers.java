/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
 * Provides lookup of {@link ExceptionMapper} instances that can be used
 * to map exceptions to responses.
 *
 * @author Paul Sandoz
 */
public interface ExceptionMappers {

    /**
     * Get an exception mapping provider for a particular class of exception.
     * Returns the provider whose generic type is the nearest superclass of
     * {@code type}.
     *
     * @param <T> type of the exception handled by the exception mapping provider.
     * @param type the class of exception.
     * @return an {@link ExceptionMapper} for the supplied type or {@code null}
     *     if none is found.
     */
    public <T extends Throwable> ExceptionMapper<T> find(Class<T> type);

    /**
     * Get an exception mapping provider for a particular exception instance.
     * <p>
     * This method is similar to method {@link #find(Class)}. In addition it takes
     * into an account the result of the {@link ExtendedExceptionMapper#isMappable(Throwable)}
     * of any mapper that implements Jersey {@link ExtendedExceptionMapper} API.
     * If an extended exception mapper returns {@code false} from {@code isMappable(Throwable)},
     * the mapper is disregarded from the search.
     * Exception mapping providers are checked one by one until a first provider returns
     * {@code true} from the {@code isMappable(Throwable)} method or until a first provider
     * is found which best supports the exception type and does not implement {@code ExtendedExceptionMapper}
     * API (i.e. it is a standard JAX-RS {@link ExceptionMapper}). The order in which the providers are
     * checked is determined by the distance of the declared exception mapper type and the actual exception
     * type.
     * </p>
     * <p>
     * Note that if an exception mapping provider does not implement {@link ExtendedExceptionMapper}
     * it is always considered applicable for a given exception instance.
     * </p>
     *
     * @param exceptionInstance exception to be handled by the exception mapping provider.
     * @param <T> type of the exception handled by the exception mapping provider.
     * @return an {@link ExceptionMapper} for the supplied exception instance type or {@code null} if none
     *          is found.
     */
    public <T extends Throwable> ExceptionMapper<T> findMapping(T exceptionInstance);
}
