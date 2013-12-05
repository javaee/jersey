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

package org.glassfish.jersey.server.monitoring;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.spi.Contract;

/**
 * A Jersey specific provider that listens to monitoring statistics. Each time when new statistics are available,
 * the implementation of {@code MonitoringStatisticsListener} will be called and new statistics will be passed.
 * Statistics are calculated in irregular undefined intervals.
 * <p>
 * The provider must not throw any exception.
 * <p/>
 * The implementation of this interface can be registered as a standard Jersey/JAX-RS provider
 * by annotating with {@link javax.ws.rs.ext.Provider @Provider} annotation in the case of
 * class path scanning, by registering as a provider using {@link org.glassfish.jersey.server.ResourceConfig}
 * or by returning from {@link javax.ws.rs.core.Application#getClasses()}
 * or {@link javax.ws.rs.core.Application#getSingletons()}}. The provider can be registered only on the server
 * side.
 * <p/>
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface MonitoringStatisticsListener {
    /**
     * The method is called when new statistics are available and statistics are passed as a argument.
     *
     * @param statistics Newly calculated monitoring statistics.
     */
    public void onStatistics(MonitoringStatistics statistics);

}
