/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
 * An extension contract for providing pluggable executor service providers to be used by
 * Jersey client or server runtime whenever a specific executor service is needed to execute a Jersey runtime processing task.
 * <p>
 * This mechanism allows Jersey to run in environments that have specific thread management and provisioning requirements,
 * such as application servers, cloud environments etc.
 * Dedicated Jersey extension modules or applications running in such environment may provide a custom
 * implementation of the {@code ExecutorServiceProvider} interface to customize the default
 * Jersey runtime thread management & provisioning strategy in order to comply with the threading requirements,
 * models and policies specific to each particular environment.
 * </p>
 * <p>
 * When Jersey runtime no longer requires the use of a provided executor service instance, it invokes the provider's
 * {@link #dispose} method to signal the provider that the executor service instance can be disposed of. In this method,
 * provider is free to implement the proper shut-down logic for the disposed executor service instance and perform other
 * necessary cleanup. Yet, some providers may wish to implement a shared executor service strategy. In such case,
 * it may not be desirable to shut down the released executor service in the {@link #dispose} method. Instead, to perform the
 * eventual shut-down procedure, the provider may either rely on an explicit invocation of it's specific clean-up method.
 * Since all Jersey providers operate in a <em>container</em> environment, a good clean-up strategy for a shared executor
 * service provider implementation is to expose a {@link javax.annotation.PreDestroy &#64;PreDestroy}-annotated method
 * that will be invoked for all instances managed by the container, before the container shuts down.
 * </p>
 * <p>
 * IMPORTANT: Please note that any pre-destroy methods may not be invoked for instances created outside of the container
 * and later registered within the container. Pre-destroy methods are only guaranteed to be invoked for those instances
 * that are created and managed by the container.
 * </p>
 * <p>
 * Jersey runtime expects that a concrete executor service provider implementation class is annotated with a
 * {@link javax.inject.Qualifier qualifier} annotation. This qualifier is then used to create a qualified injection point
 * for injecting the executor service instance provided by the annotated provider. {@link javax.inject.Named Named} providers
 * are also supported. For example:
 * </p>
 * <pre>
 * &#64;Named("my-executor")
 * public MyExecutorProvider implements ExecutorServiceProvider {
 *     ...
 * }
 *
 * ...
 *
 * // Injecting ExecutorService provided by the MyExecutorProvider
 * &#64;Inject &#64;Named("my-executor") ExecutorService myExecutor;
 * </pre>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see ScheduledExecutorServiceProvider
 * @see ThreadPoolExecutorProvider
 * @since 2.18
 */
@Contract
public interface ExecutorServiceProvider {

    /**
     * Get an executor service to be used by Jersey client or server runtime to execute specific tasks.
     * <p>
     * This method is <em>usually</em> invoked just once at either Jersey client or server application runtime initialization,
     * it <em>may</em> however be invoked multiple times. Once the instance of the provided executor service is not
     * needed anymore by Jersey application runtime, it will be {@link #dispose disposed}.
     * This typically happens in one of the following situations:
     * </p>
     * <ul>
     * <li>Jersey client instance is closed (client runtime is shut down).</li>
     * <li>Jersey container running a server-side Jersey application is shut down.</li>
     * <li>Jersey server-side application is un-deployed.</li>
     * </ul>
     *
     * @return an executor service. Must not return {@code null}.
     */
    public ExecutorService getExecutorService();

    /**
     * Invoked when Jersey runtime no longer requires use of the provided executor service.
     *
     * @param executorService executor service to be disposed.
     */
    public void dispose(ExecutorService executorService);
}
