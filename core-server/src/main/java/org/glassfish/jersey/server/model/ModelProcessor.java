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

package org.glassfish.jersey.server.model;

import javax.ws.rs.ConstrainedTo;
import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;

import org.glassfish.jersey.spi.Contract;

/**
 * Contract for a model processors that processes {@link ResourceModel resource models} during application initialization
 * and {@link Resource resource} returned by sub resource locators. Even though {@link ModelProcessor model processors} can
 * completely change the resource model, the standard use case it to enhance the current resource model by
 * additional methods and resources (like for example adding OPTIONS http methods for every URI endpoint).
 * <p/>
 * More model processors can be registered. These providers will be execute in the chain so that each model
 * processor will be executed with resource model processed by the previous model processor. The first model
 * processor in the chain will be invoked with the initial resource model from which the application was initiated.
 * <p/>
 * Model processors implementations can define {@link javax.annotation.Priority binding priority}
 * to define the order in which they are executed (processors with a lower priority is invoked
 * before processor with a higher priority). The highest possible priority (Integer.MAX_VALUE) is used for
 * model processor which enhance resource models by the default OPTIONS method defined by JAX-RS specification and therefore
 * this priority should not be used.
 * <p/>
 * Note that if model processor adds a resources that are intended to be supportive resources like
 * {@code OPTIONS} method providing information about the resource, it should properly define the
 * {@link org.glassfish.jersey.server.model.ResourceMethod#isExtended() extended} flag of such a new method.
 * See {@link org.glassfish.jersey.server.model.ExtendedResource} for more information.
 *
 * @author Miroslav Fuksa
 *
 */
@Contract
@ConstrainedTo(RuntimeType.SERVER)
public interface ModelProcessor {
    /**
     * Process {@code resourceModel} and return the processed model. Returning input {@code resourceModel} will cause
     * no effect on the final resource model.
     *
     * @param resourceModel Input resource model to be processed.
     * @param configuration Runtime configuration.
     * @return Processed resource model containing root resources. Non root resources will be ignored.
     */
    public ResourceModel processResourceModel(ResourceModel resourceModel, Configuration configuration);

    /**
     * Process {@code subResourceModel} which was returned a sub resource locator.
     * <p/>
     * The {@code subResourceModel} contains only one {@link Resource resource} representing model that should be processed
     * by further matching. The method must return also exactly one resource in the model. Returning input
     * {@code subResourceModel} instance will cause no effect on the final sub resource model.
     *
     * @param subResourceModel {@link Resource Sub resource} which is based on sub resource returned from sub resource locator.
     * @param configuration Runtime configuration.
     * @return Processed resource model with one {@link Resource resource} which should be used for handling sub resource.
     */
    public ResourceModel processSubResource(ResourceModel subResourceModel, Configuration configuration);
}
