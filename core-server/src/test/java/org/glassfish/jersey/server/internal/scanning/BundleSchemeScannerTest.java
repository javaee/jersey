/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.internal.scanning;

import java.net.URI;
import java.util.NoSuchElementException;

import org.glassfish.jersey.server.ResourceFinder;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;

/**
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
public class BundleSchemeScannerTest {

    private ResourceFinder bundleSchemeScanner;

    @Before
    public void setUpBundleSchemeScanner() throws Exception {
        String canonicalName = getClass().getCanonicalName();
        URI uri = getClass().getClassLoader().getResource(canonicalName.replace('.', '/') + ".class").toURI();
        bundleSchemeScanner = new BundleSchemeResourceFinderFactory().create(uri, true);
    }

    @Test
    public void hasNextReturnsTrue() throws Exception {
        Assert.assertTrue(bundleSchemeScanner.hasNext());
    }

    @Test(expected = NoSuchElementException.class)
    public void multipleNextInvocationFails() throws Exception {
        bundleSchemeScanner.next();

        Assert.assertFalse(bundleSchemeScanner.hasNext());
        bundleSchemeScanner.next(); // throw NoSuchElementException
    }

    /**
     * Iterator class {@link org.glassfish.jersey.server.internal.scanning.BundleSchemeResourceFinderFactory.BundleSchemeScanner}
     * breaks the {@link java.util.Iterator} contract as {@link org.glassfish.jersey.server.ResourceFinder#open()} causes it to
     * advance.
     *
     * @throws Exception
     */
    @Test(expected = NoSuchElementException.class)
    public void openFinishesTheIteration() throws Exception {
        Assert.assertNotNull(bundleSchemeScanner.open());
        Assert.assertFalse(bundleSchemeScanner.hasNext());

        bundleSchemeScanner.next(); // throw NoSuchElementException
    }

}
