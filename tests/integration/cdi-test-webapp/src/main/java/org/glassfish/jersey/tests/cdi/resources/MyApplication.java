/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.cdi.resources;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.logging.Logger;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import javax.enterprise.context.ApplicationScoped;
import javax.enterprise.inject.spi.BeanManager;

import javax.inject.Inject;

import javax.ws.rs.ApplicationPath;
import javax.ws.rs.core.Application;

/**
 * JAX-RS application to configure resources.
 *
 * @author Jonathan Benoit (jonathan.benoit at oracle.com)
 */
@ApplicationPath("/*")
@ApplicationScoped
public class MyApplication extends Application {

    static AtomicInteger postConstructCounter = new AtomicInteger();

    @Inject BeanManager bm;

    private static final Logger LOGGER = Logger.getLogger(MyApplication.class.getName());

    @Override
    public Set<Class<?>> getClasses() {
        final Set<Class<?>> classes = new HashSet<Class<?>>();
        classes.add(JCDIBeanDependentResource.class);
        classes.add(JDCIBeanException.class);
        classes.add(JDCIBeanDependentException.class);
        classes.add(JCDIBeanSingletonResource.class);
        classes.add(JCDIBeanPerRequestResource.class);
        classes.add(JCDIBeanExceptionMapper.class);
        classes.add(JCDIBeanDependentSingletonResource.class);
        classes.add(JCDIBeanDependentPerRequestResource.class);
        classes.add(JCDIBeanDependentExceptionMapper.class);
        classes.add(StutteringEchoResource.class);
        classes.add(StutteringEcho.class);
        classes.add(ReversingEchoResource.class);
        classes.add(CounterResource.class);
        classes.add(ConstructorInjectedResource.class);
        classes.add(ProducerResource.class);
        return classes;
    }

    // JERSEY-2531: make sure this type gets managed by CDI
    @PostConstruct
    public void postConstruct() {
        LOGGER.info(String.format("%s: POST CONSTRUCT.", this.getClass().getName()));
        postConstructCounter.incrementAndGet();
        if (bm == null) {
            throw new IllegalStateException("BeanManager should have been injected into a CDI managed bean.");
        }
        if (postConstructCounter.intValue() > 1) {
            throw new IllegalStateException("postConstruct should have been invoked only once on app scoped bean.");
        }
    }

    @PreDestroy
    public void preDestroy() {
        LOGGER.info(String.format("%s: PRE DESTROY.", this.getClass().getName()));
    }
}