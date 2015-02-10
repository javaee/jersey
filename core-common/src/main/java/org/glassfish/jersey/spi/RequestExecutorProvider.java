/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ExecutorService;

/**
 * Pluggable provider of {@link ExecutorService executor service} instance used to run
 * different parts of Jersey request and response processing code.
 * <p>
 * During Jersey runtime initialization, Jersey invokes the registered executor provider
 * to get the {@link #getRequestingExecutor() requesting executor} that will be used
 * to run the request pre-processing and request-to-response transformation code.
 * </p>
 * <p>
 * The custom provider implementing this interface should be registered in the standard way on the server.
 * The client must be created with configuration containing the provider, later registrations will be ignored.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Miroslav Fuksa
 */
@Contract
public interface RequestExecutorProvider {
    /**
     * Get request processing executor.
     *
     * This method is called only once at Jersey initialization, before the
     * first request is processed.
     *
     * @return request processing executor. Must not return {@code null}.
     */
    public ExecutorService getRequestingExecutor();

    /**
     * Release the executor previously retrieved via {@link #getRequestingExecutor} call.
     *
     * This method is called when the Jersey runtime does not need the executor anymore.
     * After this method has been called, the executor will not be used by the Jersey runtime
     * anymore.
     * <p>
     * The decision how the executor is released is left upon the provider implementation.
     * In most typical scenarios, the executor may be simply shutdown. However in cases when
     * the provider is implemented to re-use same executors across multiple components or Jersey
     * runtimes, the executor release logic may require more sophisticated implementation.
     * </p>
     *
     * @param executor executor instance to be released.
     * @since 2.5
     */
    public void releaseRequestingExecutor(ExecutorService executor);
}
