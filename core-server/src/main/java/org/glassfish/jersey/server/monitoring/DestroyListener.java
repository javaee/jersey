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

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.spi.Contract;

/**
 * A listener contract that allows any registered implementation class to receive application destroy events.
 * <p>
 * The {@link #onDestroy()} method is called when application is being destroyed and after all the pending
 * {@link MonitoringStatisticsListener#onStatistics(MonitoringStatistics) monitoring statistics events} have been
 * dispatched and processed.
 * </p>
 * <p>
 * The advantage of using {@code DestroyListener} over using {@link ApplicationEventListener} directly to check for the
 * {@link ApplicationEvent.Type#DESTROY_FINISHED} event is, that the {@link #onDestroy()}
 * method is guaranteed to be called only AFTER all the {@code MonitoringStatisticsListener#onStatistics()} events have been
 * dispatched and processed, as opposed to using the {@code ApplicationEventListener} directly, in which case some monitoring
 * statistics events may still be concurrently fired after the {@code DESTROY_FINISHED} event has been dispatched
 * (due to potential race conditions).
 * </p>
 *
 * @author Miroslav Fuksa
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 * @see MonitoringStatisticsListener
 * @since 2.12
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface DestroyListener {
    /**
     * The method is called when application is destroyed. Use this method release resources of
     * the listener. This method will be called in the thread safe way (synchronously and by a single thread)
     * according to other methods from the related {@link org.glassfish.jersey.server.monitoring.MonitoringStatisticsListener}
     * interface.
     */
    public void onDestroy();
}
