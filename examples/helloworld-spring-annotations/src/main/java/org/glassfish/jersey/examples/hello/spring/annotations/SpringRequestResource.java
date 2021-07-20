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

package org.glassfish.jersey.examples.hello.spring.annotations;

import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.ProcessingException;
import javax.ws.rs.Produces;
import javax.ws.rs.core.MediaType;

import javax.inject.Singleton;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

/**
 * Integration of jersey and spring.
 * This rest controller is a singleton spring bean with autowired dependencies
 * from spring
 *
 * @author Geoffroy Warin (http://geowarin.github.io)
 */
@Singleton
@Path("spring-resource")
@Service
public class SpringRequestResource {

    AtomicInteger counter = new AtomicInteger();

    @Autowired
    private GreetingService greetingService;

    @Autowired
    private List<GoodbyeService> goodbyeServicesList;
    @Autowired
    private Set<GoodbyeService> goodbyeServicesSet;

    @Autowired
    private List<GoodbyeService> goodbyeServicesIterable;


    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getHello() {
        return greetingService.greet("world " + counter.incrementAndGet());
    }

    private void checkIntegrity() {
        final Iterator<GoodbyeService> it = goodbyeServicesIterable.iterator();
        int i = 0;
        while (it.hasNext()) {

            final GoodbyeService s1 = it.next();
            final GoodbyeService s2 = goodbyeServicesList.get(i);
            if (s1 != s2) {
                throw new ProcessingException("Instance of service s1 (" + s1.getClass()
                        + ") is not equal to service s2(" + s2.getClass() + ")");
            }
            i++;
        }

        if (goodbyeServicesList.size() != goodbyeServicesSet.size()) {
            throw new ProcessingException("Size of set and size of the list differs. list=" + goodbyeServicesList.size()
                    + "; set=" + goodbyeServicesSet.size());
        }
    }

    private GoodbyeService getService(Class<?> serviceClass) {
        for (GoodbyeService service : goodbyeServicesList) {
            if (serviceClass.isAssignableFrom(service.getClass())) {
                return service;
            }
        }
        return null;
    }

    @Path("goodbye")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getGoodbye() {
        checkIntegrity();

        final GoodbyeService goodbyeService = getService(EnglishGoodbyeService.class);
        return goodbyeService.goodbye("cruel world");
    }

    @Path("norwegian-goodbye")
    @GET
    @Produces(MediaType.TEXT_PLAIN)
    public String getNorwegianGoodbye() {
        checkIntegrity();
        return getService(NorwegianGoodbyeService.class).goodbye("på badet");
    }
}
