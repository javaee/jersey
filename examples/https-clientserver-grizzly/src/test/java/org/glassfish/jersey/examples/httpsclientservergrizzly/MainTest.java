/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.httpsclientservergrizzly;

import java.io.File;
import java.io.FileInputStream;
import java.net.Socket;
import java.security.KeyManagementException;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Principal;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientFactory;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;

import javax.net.ssl.KeyManager;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509KeyManager;
import javax.net.ssl.X509TrustManager;

import org.glassfish.jersey.client.ClientProperties;
import org.glassfish.jersey.client.filter.HttpBasicAuthFilter_Old;
import org.glassfish.jersey.filter.LoggingFilter_Old;
import org.glassfish.jersey.filter.LoggingFilter_Old;

import org.junit.After;
import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

/**
 *
 * @author Pavel Bucek (pavel.bucek at oracle.com)
 */
public class MainTest {

    @Before
    public void setUp() throws Exception {
        Server.startServer();
    }

    @After
    public void tearDown() throws Exception {
        Server.stopServer();
    }

    /**
     * Test to see that the correct Http status is returned.
     */
    @Test
    // TODO re-enable filter tests.
    @Ignore
    public void testSSLWithAuth() {

        TrustManager mytm[] = null;
        KeyManager mykm[] = null;

        try {
            mytm = new TrustManager[]{new MyX509TrustManager("./truststore_client", "asdfgh".toCharArray())};
            mykm = new KeyManager[]{new MyX509KeyManager("./keystore_client", "asdfgh".toCharArray())};
        } catch (Exception ex) {

        }

        SSLContext context = null;

        try {
            context = SSLContext.getInstance("SSL");
            context.init(mykm, mytm, null);
        } catch (NoSuchAlgorithmException nae) {

        } catch (KeyManagementException kme) {

        }

        Client client = ClientFactory.newClient();
        client.configuration().setProperty(ClientProperties.SSL_CONTEXT, context);

        // client basic auth demonstration
        client.configuration().register(new HttpBasicAuthFilter_Old("user", "password"));

        System.out.println("Client: GET " + Server.BASE_URI);

        WebTarget target = client.target(Server.BASE_URI);

        final Response response = target.path("/").request().get(Response.class);

        assertEquals(200, response.getStatus());
    }

    /**
     *
     * Test to see that HTTP 401 is returned when client tries to GET without
     * proper credentials.
     */
    @Test
    // TODO re-enable filter tests.
    @Ignore
    public void testHTTPBasicAuth1() {

        TrustManager mytm[] = null;
        KeyManager mykm[] = null;

        try {
            mytm = new TrustManager[]{new MyX509TrustManager("./truststore_client", "asdfgh".toCharArray())};
            mykm = new KeyManager[]{new MyX509KeyManager("./keystore_client", "asdfgh".toCharArray())};
        } catch (Exception ex) {
            System.out.println("Something bad happened: " + ex.getMessage());
        }

        SSLContext context = null;

        try {
            context = SSLContext.getInstance("SSL");
            context.init(mykm, mytm, null);
        } catch (NoSuchAlgorithmException nae) {
            System.out.println("NoSuchAlgorithmException: " + nae.getMessage());
        } catch (KeyManagementException kme) {
            System.out.println("KeyManagementException: " + kme.getMessage());

        }

        Client client = ClientFactory.newClient();
        client.configuration().setProperty(ClientProperties.SSL_CONTEXT, context);

        System.out.println("Client: GET " + Server.BASE_URI);

        WebTarget target = client.target(Server.BASE_URI);
        target.configuration().register(new LoggingFilter_Old());

        Response response;

        response = target.path("/").request().get(Response.class);

        assertEquals(401, response.getStatus());
    }

    /**
     * Test to see that SSLHandshakeException is thrown when client don't have
     * trusted key.
     */
    @Test
    public void testSSLAuth1() {

        TrustManager mytm[] = null;

        try {
            mytm = new TrustManager[]{new MyX509TrustManager("./truststore_client", "asdfgh".toCharArray())};
        } catch (Exception ex) {
            System.out.println("Something bad happened " + ex.getMessage());
        }

        SSLContext context = null;

        try {
            context = SSLContext.getInstance("SSL");
            context.init(null, mytm, null);
        } catch (NoSuchAlgorithmException nae) {
            System.out.println("NoSuchAlgorithmException " + nae.getMessage());
        } catch (KeyManagementException kme) {
            System.out.println("KeyManagementException happened " + kme.getMessage());
        }

        Client client = ClientFactory.newClient();
        client.configuration().setProperty(ClientProperties.SSL_CONTEXT, context);

        System.out.println("Client: GET " + Server.BASE_URI);

        WebTarget target = client.target(Server.BASE_URI);
        target.configuration().register(new LoggingFilter_Old());

        boolean caught = false;

        try {
            target.path("/").request().get(String.class);
        } catch (Exception e) {
            caught = true;
        }

        assertTrue(caught);
        // solaris throws java.net.SocketException instead of SSLHandshakeException
        // assertTrue(msg.contains("SSLHandshakeException"));
    }

}

/**
 * Taken from http://java.sun.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html
 *
 */
class MyX509TrustManager implements X509TrustManager {

     /*
      * The default PKIX X509TrustManager9.  We'll delegate
      * decisions to it, and fall back to the logic in this class if the
      * default X509TrustManager doesn't trust it.
      */
     X509TrustManager pkixTrustManager;

     MyX509TrustManager(String trustStore, char[] password) throws Exception {
         this(new File(trustStore), password);
     }

     MyX509TrustManager(File trustStore, char[] password) throws Exception {
         // create a "default" JSSE X509TrustManager.

         KeyStore ks = KeyStore.getInstance("JKS");

         ks.load(new FileInputStream(trustStore), password);

         TrustManagerFactory tmf = TrustManagerFactory.getInstance("PKIX");
         tmf.init(ks);

         TrustManager tms [] = tmf.getTrustManagers();

         /*
          * Iterate over the returned trustmanagers, look
          * for an instance of X509TrustManager.  If found,
          * use that as our "default" trust manager.
          */
         for (int i = 0; i < tms.length; i++) {
             if (tms[i] instanceof X509TrustManager) {
                 pkixTrustManager = (X509TrustManager) tms[i];
                 return;
             }
         }

         /*
          * Find some other way to initialize, or else we have to fail the
          * constructor.
          */
         throw new Exception("Couldn't initialize");
     }

     /*
      * Delegate to the default trust manager.
      */
     public void checkClientTrusted(X509Certificate[] chain, String authType)
                 throws CertificateException {
         try {
             pkixTrustManager.checkClientTrusted(chain, authType);
         } catch (CertificateException excep) {
             // do any special handling here, or rethrow exception.
         }
     }

     /*
      * Delegate to the default trust manager.
      */
     public void checkServerTrusted(X509Certificate[] chain, String authType)
                 throws CertificateException {
         try {
             pkixTrustManager.checkServerTrusted(chain, authType);
         } catch (CertificateException excep) {
             /*
              * Possibly pop up a dialog box asking whether to trust the
              * cert chain.
              */
         }
     }

     /*
      * Merely pass this through.
      */
     public X509Certificate[] getAcceptedIssuers() {
         return pkixTrustManager.getAcceptedIssuers();
     }
}

/**
 * Inspired from http://java.sun.com/javase/6/docs/technotes/guides/security/jsse/JSSERefGuide.html
 *
 */
class MyX509KeyManager implements X509KeyManager {

     /*
      * The default PKIX X509KeyManager.  We'll delegate
      * decisions to it, and fall back to the logic in this class if the
      * default X509KeyManager doesn't trust it.
      */
     X509KeyManager pkixKeyManager;

     MyX509KeyManager(String keyStore, char[] password) throws Exception {
         this(new File(keyStore), password);
     }

     MyX509KeyManager(File keyStore, char[] password) throws Exception {
         // create a "default" JSSE X509KeyManager.

         KeyStore ks = KeyStore.getInstance("JKS");
         ks.load(new FileInputStream(keyStore), password);

         KeyManagerFactory kmf = KeyManagerFactory.getInstance("SunX509", "SunJSSE");
         kmf.init(ks, "asdfgh".toCharArray());

         KeyManager kms[] = kmf.getKeyManagers();

         /*
          * Iterate over the returned keymanagers, look
          * for an instance of X509KeyManager.  If found,
          * use that as our "default" key manager.
          */
         for (int i = 0; i < kms.length; i++) {
             if (kms[i] instanceof X509KeyManager) {
                 pkixKeyManager = (X509KeyManager) kms[i];
                 return;
             }
         }

         /*
          * Find some other way to initialize, or else we have to fail the
          * constructor.
          */
         throw new Exception("Couldn't initialize");
     }

    public PrivateKey getPrivateKey(String arg0) {
        return pkixKeyManager.getPrivateKey(arg0);
    }

    public X509Certificate[] getCertificateChain(String arg0) {
        return pkixKeyManager.getCertificateChain(arg0);
    }

    public String[] getClientAliases(String arg0, Principal[] arg1) {
        return pkixKeyManager.getClientAliases(arg0, arg1);
    }

    public String chooseClientAlias(String[] arg0, Principal[] arg1, Socket arg2) {
        return pkixKeyManager.chooseClientAlias(arg0, arg1, arg2);
    }

    public String[] getServerAliases(String arg0, Principal[] arg1) {
        return pkixKeyManager.getServerAliases(arg0, arg1);
    }

    public String chooseServerAlias(String arg0, Principal[] arg1, Socket arg2) {
        return pkixKeyManager.chooseServerAlias(arg0, arg1, arg2);
    }
}

