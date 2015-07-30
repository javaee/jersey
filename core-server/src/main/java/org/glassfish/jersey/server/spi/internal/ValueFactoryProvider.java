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
package org.glassfish.jersey.server.spi.internal;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;

import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.spi.Contract;

import org.glassfish.hk2.api.Factory;

/**
 * Parameter value factory SPI.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 * @author Michal Gajdos
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface ValueFactoryProvider {

    /**
     * Get an injected value factory for the parameter. May return {@code null}
     * in case the parameter is not supported by the value factory provider.
     *
     * @param parameter parameter requesting the value factory instance.
     * @return injected parameter value factory. Returns {@code null} if parameter
     *         is not supported.
     */
    public Factory<?> getValueFactory(Parameter parameter);

    /**
     * Gets the priority of this provider.
     *
     * @return the priority of this provider.
     * @see PriorityType
     * @see Priority
     */
    public PriorityType getPriority();

    /**
     * Priorities are intended to be used as a means to determine the order in which objects are considered whether they are
     * suitable for a particular action or not (e.g. providing a service like creating a value factory for an injectable
     * parameter).
     * The higher the weight of a priority is the sooner should be an object with this priority examined.
     * <p/>
     * If two objects are of the same priority there is no guarantee which one comes first.
     *
     * @see org.glassfish.jersey.server.spi.internal.ValueFactoryProvider.Priority
     */
    interface PriorityType {

        /**
         * Returns the weight of this priority.
         *
         * @return weight of this priority.
         */
        public int getWeight();

    }

    /**
     * Enumeration of priorities for providers (e.g. {@code ValueFactoryProvider}). At first providers with the {@code HIGH}
     * priority are examined then those with {@code NORMAL} priority and at last the ones with the {@code LOW} priority.
     */
    enum Priority implements PriorityType {
        /**
         * Low priority.
         */
        LOW(100),
        /**
         * Normal priority.
         */
        NORMAL(200),
        /**
         * High priority.
         */
        HIGH(300);

        /**
         * Weight of this priority.
         */
        private final int weight;

        private Priority(int weight) {
            this.weight = weight;
        }

        @Override
        public int getWeight() {
            return weight;
        }

    }
}
