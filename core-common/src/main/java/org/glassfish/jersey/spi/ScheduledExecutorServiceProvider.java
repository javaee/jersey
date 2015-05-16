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

import java.util.concurrent.ScheduledExecutorService;

/**
 * An extension contract for providing pluggable scheduled executor service providers to be used by
 * Jersey client or server runtime whenever a specific scheduler is needed to schedule execution of a
 * Jersey runtime processing task.
 * <p>
 * This mechanism allows Jersey to run in environments that have specific thread management and provisioning requirements,
 * such as application servers, cloud environments etc.
 * Dedicated Jersey extension modules or applications running in such environment may provide a custom
 * implementation of the {@code ScheduledExecutorServiceProvider} interface to customize the default
 * Jersey runtime thread management & provisioning strategy in order to comply with the threading requirements,
 * models and policies specific to each particular environment.
 * </p>
 * Jersey runtime expects that a concrete scheduled executor service provider implementation class is annotated with a
 * {@link javax.inject.Qualifier qualifier} annotation. This qualifier is then used to create a qualified injection point
 * for injecting the scheduled executor service instance provided by the annotated provider. {@link javax.inject.Named Named}
 * providers are also supported. For example:
 * </p>
 * <pre>
 * &#64;Named("my-scheduler")
 * public MySchedulerProvider implements ScheduledExecutorServiceProvider {
 *     ...
 * }
 *
 * ...
 *
 * // Injecting ScheduledExecutorService provided by the MySchedulerProvider
 * &#64;Inject &#64;Named("my-scheduler") ScheduledExecutorService myScheduler;
 * </pre>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see ExecutorServiceProvider
 * @see ScheduledThreadPoolExecutorProvider
 * @since 2.18
 */
@Contract
public interface ScheduledExecutorServiceProvider extends ExecutorServiceProvider {

    /**
     * Get a scheduled executor service to be used by Jersey client or server runtime to schedule execution of
     * specific tasks.
     * <p>
     * <p>
     * This method is <em>usually</em> invoked just once at either Jersey client or server application runtime initialization,
     * it <em>may</em> however be invoked multiple times. Once the instance of the provided scheduled executor service is not
     * needed anymore by Jersey application runtime, it will be {@link #dispose disposed}.
     * This typically happens in one of the following situations:
     * </p>
     * <ul>
     * <li>Jersey client instance is closed (client runtime is shut down).</li>
     * <li>Jersey container running a server-side Jersey application is shut down.</li>
     * <li>Jersey server-side application is un-deployed.</li>
     * </ul>
     *
     * @return a scheduled executor service. Must not return {@code null}.
     */
    @Override
    public ScheduledExecutorService getExecutorService();

}
