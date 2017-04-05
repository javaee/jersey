/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.internal.inject;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

import javax.inject.Provider;
import javax.inject.Singleton;

import org.glassfish.jersey.message.internal.CacheControlProvider;
import org.glassfish.jersey.message.internal.CookieProvider;
import org.glassfish.jersey.message.internal.DateProvider;
import org.glassfish.jersey.message.internal.EntityTagProvider;
import org.glassfish.jersey.message.internal.LinkProvider;
import org.glassfish.jersey.message.internal.MediaTypeProvider;
import org.glassfish.jersey.message.internal.NewCookieProvider;
import org.glassfish.jersey.message.internal.StringHeaderProvider;
import org.glassfish.jersey.message.internal.UriProvider;
import org.glassfish.jersey.spi.HeaderDelegateProvider;

import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * @author Petr Bouda (petr.bouda@oracle.com)
 */
public class AbstractBinderTest {

    @Test
    public void testFirstLayer() {
        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                bind(CacheControlProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(CookieProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(DateProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(EntityTagProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(LinkProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
            }
        };

        List<Binding> bindings = new ArrayList<>(binder.getBindings());
        assertEquals(5, bindings.size());

        // Keep ordering.
        assertEquals(CacheControlProvider.class, ((ClassBinding) bindings.get(0)).getService());
        assertEquals(CookieProvider.class, ((ClassBinding) bindings.get(1)).getService());
        assertEquals(DateProvider.class, ((ClassBinding) bindings.get(2)).getService());
        assertEquals(EntityTagProvider.class, ((ClassBinding) bindings.get(3)).getService());
        assertEquals(LinkProvider.class, ((ClassBinding) bindings.get(4)).getService());
    }

    @Test
    public void testRepeatedGetBindings() {
        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                bind(CacheControlProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
            }
        };

        Collection<Binding> bindings1 = binder.getBindings();
        assertEquals(1, bindings1.size());

        Collection<Binding> bindings2 = binder.getBindings();
        assertEquals(1, bindings2.size());
    }

    @Test(expected = IllegalStateException.class)
    public void testInjectionManagerIsNotAvailable() {
        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                Provider<HeaderDelegateProvider> managedInstanceProvider =
                        createManagedInstanceProvider(HeaderDelegateProvider.class);
                managedInstanceProvider.get();
            }
        };
        binder.configure();
    }

    @Test
    public void testCreateProvider() {
        CacheControlProvider cacheControlProvider = new CacheControlProvider();
        InjectionManager injectionManager = Injections.createInjectionManager();
        injectionManager.register(Bindings.service(cacheControlProvider).to(HeaderDelegateProvider.class));

        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                Provider<HeaderDelegateProvider> managedInstanceProvider =
                        createManagedInstanceProvider(HeaderDelegateProvider.class);
                assertEquals(cacheControlProvider, managedInstanceProvider.get());
            }
        };
        binder.setInjectionManager(injectionManager);
        binder.configure();
    }

    @Test
    public void testWithInstall() {
        AbstractBinder internalBinder = new AbstractBinder() {
            @Override
            protected void configure() {
                bind(MediaTypeProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(NewCookieProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(StringHeaderProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(UriProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
            }
        };

        AbstractBinder binder = new AbstractBinder() {
            @Override
            protected void configure() {
                bind(CacheControlProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(CookieProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(DateProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(EntityTagProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);
                bind(LinkProvider.class).to(HeaderDelegateProvider.class).in(Singleton.class);

                install(internalBinder);
            }
        };

        List<Binding> bindings = new ArrayList<>(binder.getBindings());
        assertEquals(9, bindings.size());

        // Keep ordering.
        assertEquals(MediaTypeProvider.class, ((ClassBinding) bindings.get(0)).getService());
        assertEquals(NewCookieProvider.class, ((ClassBinding) bindings.get(1)).getService());
        assertEquals(StringHeaderProvider.class, ((ClassBinding) bindings.get(2)).getService());
        assertEquals(UriProvider.class, ((ClassBinding) bindings.get(3)).getService());
        assertEquals(CacheControlProvider.class, ((ClassBinding) bindings.get(4)).getService());
        assertEquals(CookieProvider.class, ((ClassBinding) bindings.get(5)).getService());
        assertEquals(DateProvider.class, ((ClassBinding) bindings.get(6)).getService());
        assertEquals(EntityTagProvider.class, ((ClassBinding) bindings.get(7)).getService());
        assertEquals(LinkProvider.class, ((ClassBinding) bindings.get(8)).getService());
    }

}
