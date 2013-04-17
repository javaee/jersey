/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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

import java.util.concurrent.ThreadFactory;

/**
 * An extension contract for providing pluggable thread factory providers that produce thread
 * factories used by Jersey runtime whenever a new thread factory is needed to create Jersey
 * runtime threads.
 * <p>
 * This mechanism allows Jersey to run in environments that have specific thread management and
 * provisioning requirements, such as application servers etc. Dedicated Jersey extension modules
 * or applications running in such environment may provide a custom implementation of the
 * {@code RuntimeThreadProvider} interface to customize the Jersey runtime thread management
 * & provisioning strategy to comply with the threading requirements, models and policies
 * specific to each particular environment.
 * </p>
 * <p>
 * Note that only a single thread factory provider can be registered in each application.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Contract
public interface RuntimeThreadProvider {
    /**
     * Get a {@code ThreadFactory} that will be used to create threads scoped to the current request.
     * <p>
     * The method is not used by Jersey runtime at the moment but may be required in the future.
     * </p>
     *
     * @return a thread factory to be used to create current request scoped threads.
     */
    public ThreadFactory getRequestThreadFactory();

    /**
     * Get a {@code ThreadFactory} that will create threads that will be used for running background
     * tasks, independently of current request scope.
     *
     * @return a thread factory to be used to create background runtime task threads.
     */
    public ThreadFactory getBackgroundThreadFactory();
}
