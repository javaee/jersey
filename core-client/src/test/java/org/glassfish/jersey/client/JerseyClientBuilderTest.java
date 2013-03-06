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
package org.glassfish.jersey.client;

import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;

import javax.ws.rs.client.Client;

import javax.net.ssl.SSLContext;

import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.assertSame;
import static org.junit.Assert.fail;


/**
 * {@link JerseyClient} unit test.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyClientBuilderTest {
    private JerseyClientBuilder builder;

    @Before
    public void setUp() {
        builder = new JerseyClientBuilder();
    }

    @Test
    public void testBuildClientWithNullSslConfig() throws KeyStoreException {
        try {
            builder.sslContext(null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }

        try {
            builder.keyStore(null, "abc");
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }
        try {
            builder.keyStore(null, "abc".toCharArray());
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }

        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        try {
            builder.keyStore(ks, (String) null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }
        try {
            builder.keyStore(ks, (char[]) null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }

        try {
            builder.keyStore(null, (String) null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }
        try {
            builder.keyStore(null, (char[]) null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }

        try {
            builder.trustStore(null);
            fail("NullPointerException expected for 'null' SSL context.");
        } catch (NullPointerException npe) {
            // pass
        }
    }

    @Test
    public void testOverridingSslConfig() throws KeyStoreException, NoSuchAlgorithmException {
        KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
        SSLContext ctx = SSLContext.getInstance("SSL");
        Client client;

        client = new JerseyClientBuilder().keyStore(ks, "qwerty").sslContext(ctx).build();
        assertSame("SSL context not the same as set in the client builder.", ctx, client.getSslContext());

        client = new JerseyClientBuilder().sslContext(ctx).trustStore(ks).build();
        assertNotSame("SSL context not overridden in the client builder.", ctx, client.getSslContext());
    }
}
