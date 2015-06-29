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
package org.glassfish.jersey.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.inject.Qualifier;

/**
 * Injection qualifier that can be used to inject a {@link java.util.concurrent.ScheduledExecutorService}
 * instance used by Jersey to execute background timed/scheduled tasks.
 * <p>
 * A scheduled executor service instance injected using this injection qualifier can be customized by registering
 * a custom {@link org.glassfish.jersey.spi.ScheduledExecutorServiceProvider} implementation that is itself annotated
 * with the {@code &#64;BackgroundScheduler} annotation.
 * </p>
 * <p>
 * Typically, when facing a need to execute a scheduled background task, you would be creating a new
 * standalone executor service that would be using a new standalone thread pool. This would however break
 * the ability of Jersey to run in environments that have specific thread management and provisioning
 * requirements. In order to simplify and unify programming model for scheduling background tasks in
 * Jersey runtime, Jersey provides an this qualifier to inject a common, task scheduler that is properly
 * configured to support customizable runtime thread .
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @see BackgroundSchedulerLiteral
 * @since 2.18
 */
@Target({ElementType.PARAMETER, ElementType.FIELD, ElementType.METHOD, ElementType.TYPE})
@Retention(RetentionPolicy.RUNTIME)
@Documented
@Qualifier
public @interface BackgroundScheduler {

}
