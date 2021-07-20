/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.rx;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.ext.ContextResolver;

import org.glassfish.jersey.examples.rx.agent.AsyncAgentResource;
import org.glassfish.jersey.examples.rx.agent.CompletionStageAgentResource;
import org.glassfish.jersey.examples.rx.agent.FlowableAgentResource;
import org.glassfish.jersey.examples.rx.agent.ListenableFutureAgentResource;
import org.glassfish.jersey.examples.rx.agent.ObservableAgentResource;
import org.glassfish.jersey.examples.rx.agent.SyncAgentResource;
import org.glassfish.jersey.examples.rx.remote.CalculationResource;
import org.glassfish.jersey.examples.rx.remote.DestinationResource;
import org.glassfish.jersey.examples.rx.remote.ForecastResource;
import org.glassfish.jersey.jackson.JacksonFeature;
import org.glassfish.jersey.server.ResourceConfig;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

/**
 * @author Michal Gajdos
 */
@ApplicationPath("rx")
public class RxApplication extends ResourceConfig {

    public RxApplication() {
        // Remote (Server) Resources.
        register(DestinationResource.class);
        register(CalculationResource.class);
        register(ForecastResource.class);

        // Agent (Client) Resources.
        register(SyncAgentResource.class);
        register(AsyncAgentResource.class);
        register(ObservableAgentResource.class);
        register(FlowableAgentResource.class);
        register(ListenableFutureAgentResource.class);
        register(CompletionStageAgentResource.class);

        // Providers.
        register(JacksonFeature.class);
        register(ObjectMapperProvider.class);
    }

    public static class ObjectMapperProvider implements ContextResolver<ObjectMapper> {

        @Override
        public ObjectMapper getContext(final Class<?> type) {
            return new ObjectMapper().enable(SerializationFeature.INDENT_OUTPUT);
        }
    }
}
