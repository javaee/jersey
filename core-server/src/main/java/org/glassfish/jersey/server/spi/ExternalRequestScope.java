/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.spi;

import org.glassfish.hk2.api.ServiceLocator;

/**
 * This is to allow integration with other DI providers that
 * define their own request scope. Any such provider should implement
 * this to properly open/finish the scope.
 * <p>
 * An implementation must be registered via META-INF/services mechanism.
 * Only one implementation will be utilized during runtime.
 * If more than one implementation is registered, no one will get used and
 * an error message will be logged out.
 * </p>
 *
 * @param <T> external request context type
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 * @since 2.15
 */
public interface ExternalRequestScope<T> extends AutoCloseable {

    /**
     * Invoked when a new request gets started.
     * Returned context data will be retained
     * by Jersey runtime for the whole request life-span.
     *
     * @param locator HK2 service locator
     * @return external request context data
     */
    public ExternalRequestContext<T> open(ServiceLocator locator);

    /**
     * Suspend request associated with provided context.
     * This will be called within the very same thread as previous open or resume call
     * corresponding to the actual context.
     *
     * @param c       external request context
     * @param locator HK2 service locator
     */
    public void suspend(ExternalRequestContext<T> c, ServiceLocator locator);

    /**
     * Resume request associated with provided context.
     * The external request context instance should have been
     * previously suspended.
     *
     * @param c       external request context
     * @param locator HK2 service locator
     */
    public void resume(ExternalRequestContext<T> c, ServiceLocator locator);

    /**
     * Finish the actual request. This method will be called
     * following previous open method call on the very same thread
     * or after open method call followed by (several) suspend/resume invocations,
     * where the last resume call has been invoked on the same thread
     * as the final close method invocation.
     */
    @Override
    public void close();
}
