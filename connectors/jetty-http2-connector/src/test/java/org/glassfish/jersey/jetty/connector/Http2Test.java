package org.glassfish.jersey.jetty.connector;

import com.fasterxml.jackson.jaxrs.json.JacksonJsonProvider;
import org.eclipse.jetty.alpn.server.ALPNServerConnectionFactory;
import org.eclipse.jetty.http2.server.HTTP2ServerConnectionFactory;
import org.eclipse.jetty.server.*;
import org.eclipse.jetty.servlet.ServletContextHandler;
import org.eclipse.jetty.servlet.ServletHolder;
import org.eclipse.jetty.util.MultiException;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.client.proxy.WebResourceFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.servlet.ServletContainer;
import org.junit.Test;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.ClientBuilder;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertificateException;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;

/**
 * Test TLS encryption between a client and a server JAX-RS.
 */
public class Http2Test {

    private static final int PORT = 2223;

    public static void main(String[] args) throws Exception {
        TlsSecurityConfiguration tlsSecurityConfiguration = new TlsSecurityConfiguration(
                getKeyStore("jks-keystore-password".toCharArray(), "localhost.jks"),
                "localhost",
                "aXeDUspU3AvUkaf5$a",
                "TLSv1.2"
        );
        try (AutoCloseable ignored = jerseyServer(
                PORT,
                tlsSecurityConfiguration,
                DummyRestService.class)) {
            System.in.read();
        }
    }

    private static <T> T getClient(int port, KeyStore trustStore, Class<T> clazz) {
        ClientConfig config = new ClientConfig();
        config.property("jersey.config.jetty.client.enableSslHostnameVerification", Boolean.TRUE);
        config.connectorProvider(JettyHttp2Connector::new);
        return WebResourceFactory.newResource(
                clazz,
                ClientBuilder.newBuilder()
                        .register(new JacksonJsonProvider())
                        .trustStore(trustStore)
                        .withConfig(config)
                        .build()
                        .target("https://localhost:" + port)
        );
    }

    private static AutoCloseable jerseyServer(int port, TlsSecurityConfiguration tlsSecurityConfiguration, final Class<?>... serviceClasses) {
        return new AutoCloseable() {
            private final Server server;

            {
                this.server = new Server();

                HttpConfiguration http_config = new HttpConfiguration();
                http_config.setSecureScheme("https");
                http_config.setSecurePort(port);

                // HTTPS Configuration
                HttpConfiguration https_config = new HttpConfiguration(http_config);
                https_config.addCustomizer(new SecureRequestCustomizer());

                // HTTP/2 Connection Factory
                HTTP2ServerConnectionFactory h2 = new HTTP2ServerConnectionFactory(https_config);
                NegotiatingServerConnectionFactory.checkProtocolNegotiationAvailable();
                ALPNServerConnectionFactory alpn = new ALPNServerConnectionFactory();
                alpn.setDefaultProtocol("h2");

                SslContextFactory sslContextFactory = new SslContextFactory();
                sslContextFactory.setKeyStore(tlsSecurityConfiguration.keyStore);
                sslContextFactory.setCertAlias(tlsSecurityConfiguration.certificateAlias);
                sslContextFactory.setKeyManagerPassword(tlsSecurityConfiguration.certificatePassword);
                sslContextFactory.setIncludeProtocols(tlsSecurityConfiguration.protocol);

                // SSL Connection Factory
                SslConnectionFactory ssl = new SslConnectionFactory(sslContextFactory, alpn.getProtocol());

                // HTTP/2 Connector
                ServerConnector http2Connector =
                        new ServerConnector(server, ssl, alpn, h2, new HttpConnectionFactory(https_config));
                http2Connector.setPort(port);
                server.addConnector(http2Connector);

                ServletContextHandler context = new ServletContextHandler(server, "/*");

                ServletHolder servlet = new ServletHolder(new ServletContainer(new ResourceConfig() {
                    {
                        for (Class<?> serviceClass : serviceClasses) {
                            register(serviceClass);
                        }
                    }
                }));

                context.addServlet(servlet, "/*");

                try {
                    server.start();

                } catch (Exception e) {
                    try {
                        close();
                    } catch (RuntimeException closeException) {
                        MultiException multiException = new MultiException();
                        multiException.add(e);
                        multiException.add(closeException);
                        throw new IllegalStateException(multiException);
                    }
                    throw new IllegalStateException(e);
                }
            }

            @Override
            public void close() {
                if (server != null) {
                    try {
                        server.stop();
                    } catch (Exception e) {
                        throw new IllegalStateException(e);
                    } finally {
                        server.destroy();
                    }
                }
            }
        };
    }


    private static KeyStore getKeyStore(char[] password, String keystoreClasspathLocation) {
        KeyStore keystore;
        try {
            keystore = KeyStore.getInstance(KeyStore.getDefaultType());
        } catch (KeyStoreException e) {
            throw new IllegalStateException(e);
        }
        try (InputStream myKeys = Thread.currentThread().getContextClassLoader().getResourceAsStream(keystoreClasspathLocation)) {
            keystore.load(myKeys, password);
        } catch (CertificateException | NoSuchAlgorithmException | IOException e) {
            throw new IllegalStateException(e);
        }
        return keystore;
    }


    @Test(timeout = 2_000)
    public void testValidTls() throws Exception {
        int port = PORT;
        TlsSecurityConfiguration tlsSecurityConfiguration = new TlsSecurityConfiguration(
                getKeyStore("jks-keystore-password".toCharArray(), "localhost.jks"),
                "localhost",
                "aXeDUspU3AvUkaf5$a",
                "TLSv1.2"
        );
        try (AutoCloseable ignored = jerseyServer(
                port,
                tlsSecurityConfiguration,
                DummyRestService.class)) {
            DummyRestApi.Data hello = getClient(port, getKeyStore("jks-password".toCharArray(), "truststore.jks"), DummyRestApi.class).hello();
            assertEquals(DummyRestService.helloMessage, hello.getData());
        }
    }

    @Test(timeout = 2_000)
    public void testInvalidAddressTls() throws Exception {
        int port = PORT;
        TlsSecurityConfiguration tlsSecurityConfiguration = new TlsSecurityConfiguration(
                getKeyStore("jks-keystore-password".toCharArray(), "other.jks"),
                "other",
                "VuqEvasaFr!mA3$W2Tr",
                "TLSv1.2"
        );
        try (AutoCloseable ignored = jerseyServer(
                port,
                tlsSecurityConfiguration,
                DummyRestService.class)) {
            getClient(port, getKeyStore("jks-password".toCharArray(), "truststore.jks"), DummyRestApi.class).hello();
            fail();
        } catch (ProcessingException e) {
            assertEquals("java.util.concurrent.ExecutionException: org.eclipse.jetty.io.EofException", e.getMessage());
        }
    }

    @Test
    public void testNoTrustStoreTls() throws Exception {
        int port = PORT;
        TlsSecurityConfiguration tlsSecurityConfiguration = new TlsSecurityConfiguration(
                getKeyStore("jks-keystore-password".toCharArray(), "localhost.jks"),
                "localhost",
                "aXeDUspU3AvUkaf5$a",
                "TLSv1.2"
        );
        try (AutoCloseable ignored = jerseyServer(
                port,
                tlsSecurityConfiguration,
                DummyRestService.class)) {
            WebResourceFactory.newResource(
                    DummyRestApi.class,
                    ClientBuilder.newBuilder()
                            .register(new JacksonJsonProvider())
                            .build()
                            .target("https://localhost:" + port)).hello();
            fail();
        } catch (ProcessingException e) {
            assertEquals("javax.net.ssl.SSLHandshakeException: sun.security.validator.ValidatorException: PKIX path building failed: sun.security.provider.certpath.SunCertPathBuilderException: unable to find valid certification path to requested target", e.getMessage());
        }
    }


    @Test
    public void testWrongPasswordTls() throws Exception {
        int port = PORT;
        TlsSecurityConfiguration tlsSecurityConfiguration = new TlsSecurityConfiguration(
                getKeyStore("jks-keystore-password".toCharArray(), "localhost.jks"),
                "localhost",
                "aXeDUspU3AvUkaf5$af",
                "TLSv1.2"
        );

        try (AutoCloseable ignored = jerseyServer(
                port,
                tlsSecurityConfiguration,
                DummyRestService.class)) {
            getClient(port, getKeyStore("jks-password".toCharArray(), "truststore.jks"), DummyRestApi.class).hello();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("java.security.UnrecoverableKeyException: Cannot recover key", e.getMessage());
        }
    }

    @Test
    public void testDeprecatedTls() throws Exception {
        int port = PORT;
        TlsSecurityConfiguration tlsSecurityConfiguration = new TlsSecurityConfiguration(
                getKeyStore("jks-keystore-password".toCharArray(), "localhost.jks"),
                "localhost",
                "aXeDUspU3AvUkaf5$af",
                "TLSv1.2"
        );

        try (AutoCloseable ignored = jerseyServer(
                port,
                tlsSecurityConfiguration,
                DummyRestService.class)) {
            getClient(port, getKeyStore("jks-password".toCharArray(), "truststore.jks"), DummyRestApi.class).hello();
            fail();
        } catch (IllegalStateException e) {
            assertEquals("java.security.UnrecoverableKeyException: Cannot recover key", e.getMessage());
        }
    }

    private static class TlsSecurityConfiguration {
        private final KeyStore keyStore;
        private final String certificateAlias;
        private final String certificatePassword;
        private final String protocol;

        private TlsSecurityConfiguration(KeyStore keyStore, String certificateAlias, String certificatePassword, String protocol) {
            this.keyStore = keyStore;
            this.certificateAlias = certificateAlias;
            this.certificatePassword = certificatePassword;
            this.protocol = protocol;
        }
    }
}
