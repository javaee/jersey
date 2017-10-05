package org.glassfish.jersey.apache.connector;

import static org.junit.Assert.assertEquals;

import java.io.IOException;
import java.io.InputStream;
import java.net.InetAddress;
import java.net.Socket;
import java.net.URI;
import java.security.GeneralSecurityException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.logging.Logger;

import javax.net.ssl.HandshakeCompletedEvent;
import javax.net.ssl.HandshakeCompletedListener;
import javax.net.ssl.KeyManagerFactory;
import javax.net.ssl.SSLContext;
import javax.net.ssl.SSLSocket;
import javax.net.ssl.SSLSocketFactory;
import javax.net.ssl.TrustManagerFactory;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Client;
import javax.ws.rs.client.ClientBuilder;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.apache.http.config.Registry;
import org.apache.http.config.RegistryBuilder;
import org.apache.http.conn.socket.ConnectionSocketFactory;
import org.apache.http.conn.socket.PlainConnectionSocketFactory;
import org.apache.http.conn.ssl.NoopHostnameVerifier;
import org.apache.http.conn.ssl.SSLConnectionSocketFactory;
import org.apache.http.impl.conn.PoolingHttpClientConnectionManager;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.util.ssl.SslContextFactory;
import org.glassfish.jersey.SslConfigurator;
import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.filter.LoggingFilter;
import org.glassfish.jersey.jetty.JettyHttpContainerFactory;
import org.glassfish.jersey.server.ResourceConfig;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import com.google.common.io.ByteStreams;

public class ReuseUserTokenTest {

    private static final Logger LOGGER = Logger.getLogger(ReuseUserTokenTest.class.getName());

    private static final String KEYSTORE_FILE = "./keystore.jks";
    private static final String KEYSTORE_PWD = "asdfgh";
    private static final String TRUSTSTORE_FILE = "./truststore.jks";
    private static final String TRUSTSTORE_PWD = "asdfgh";

    private final Object serverGuard = new Object();
    private Server server = null;

    @Before
    public void setUp() throws Exception {
        synchronized (serverGuard) {
            if (server != null) {
                throw new IllegalStateException(
                        "Test run sync issue: Another instance of the SSL-secured HTTP test server has been already started.");
            }

            SslContextFactory sslContextFactory = new SslContextFactory();
            sslContextFactory.setSslContext(createSSLContext());
            sslContextFactory.setNeedClientAuth(true);

            ResourceConfig config = new ResourceConfig()
                .register(new LoggingFilter(LOGGER, true))
                .register(TestResource.class);

            server = JettyHttpContainerFactory.createServer(getBaseUri(), sslContextFactory, config);
        }
    }

    @After
    public void tearDown() throws Exception {
        synchronized (serverGuard) {
            if (server == null) {
                throw new IllegalStateException("Test run sync issue: There is no SSL-secured HTTP test server to stop.");
            }
            server.stop();
            server = null;
        }
    }

    private SSLContext createSSLContext() throws IOException, GeneralSecurityException {
        InputStream trustStore = ReuseUserTokenTest.class.getResourceAsStream(TRUSTSTORE_FILE);
        InputStream keyStore = ReuseUserTokenTest.class.getResourceAsStream(KEYSTORE_FILE);

        return SslConfigurator.newInstance()
                .trustStoreBytes(ByteStreams.toByteArray(trustStore))
                .trustStorePassword(TRUSTSTORE_PWD)
                .trustManagerFactoryAlgorithm(TrustManagerFactory.getDefaultAlgorithm())
                .keyStoreBytes(ByteStreams.toByteArray(keyStore))
                .keyPassword(KEYSTORE_PWD)
                .keyManagerFactoryAlgorithm(KeyManagerFactory.getDefaultAlgorithm())
                .securityProtocol("TLSv1.2")
                .createSSLContext();
    }

    private URI getBaseUri() {
        return UriBuilder.fromUri("https://localhost/").port(9998).build();
    }

    private WebTarget getWebTarget(final Client client) {
        return client.target(getBaseUri());
    }

    private static void close(Client client) {
        if (client != null) {
            client.close();
        }
    }

    private Client createClient(HandshakeCompletedListener handshakeCompletedListener, boolean reuseUserToken)
            throws IOException, GeneralSecurityException {
        SSLContext sslContext = createSSLContext();
        SSLConnectionSocketFactory sslSocketFactory = new SSLConnectionSocketFactory(new CustomSSLSocketFactory(sslContext,
                handshakeCompletedListener), NoopHostnameVerifier.INSTANCE);

        Registry<ConnectionSocketFactory> socketFactoryRegistry = RegistryBuilder.<ConnectionSocketFactory>create()
                .register("http", PlainConnectionSocketFactory.getSocketFactory())
                .register("https", sslSocketFactory).build();

        PoolingHttpClientConnectionManager connectionManager = new PoolingHttpClientConnectionManager(socketFactoryRegistry);
        connectionManager.setDefaultMaxPerRoute(20);
        connectionManager.setMaxTotal(50);

        ClientConfig config = new ClientConfig()
                .register(new LoggingFilter(LOGGER, true))
                .property(ApacheClientProperties.REUSE_USER_TOKEN, reuseUserToken)
                .property(ApacheClientProperties.CONNECTION_MANAGER, connectionManager)
                .connectorProvider(new ApacheConnectorProvider());

        return ClientBuilder.newBuilder()
                .withConfig(config)
                .sslContext(sslContext)
                .hostnameVerifier(NoopHostnameVerifier.INSTANCE)
                .build();
    }

    @Test
    public void testMultipleSSLHandshakes() throws IOException, GeneralSecurityException {
        Client client = null;
        try {
            CustomHandshakeCompletedListener handshakeCompletedListener = new CustomHandshakeCompletedListener();
            client = createClient(handshakeCompletedListener, false);

            for (int i = 0; i < 5; i++) {
                Response response = getWebTarget(client).request().get();
                assertEquals("Response status unexpected.", 200, response.getStatus());
                assertEquals("Response entity unexpected.", "GET", response.readEntity(String.class));
                assertEquals("Multiple ssl handshakes expected.", i + 1, handshakeCompletedListener.getHandshakeEvents().size());
                response.close();
            }
        } finally {
            close(client);
        }
    }

    @Test
    public void testSingleSSLHandshake() throws IOException, GeneralSecurityException {
        Client client = null;
        try {
            CustomHandshakeCompletedListener handshakeCompletedListener = new CustomHandshakeCompletedListener();
            client = createClient(handshakeCompletedListener, true);

            for (int i = 0; i < 100; i++) {
                Response response = getWebTarget(client).request().get();
                assertEquals("Response status unexpected.", 200, response.getStatus());
                assertEquals("Response entity unexpected.", "GET", response.readEntity(String.class));
                assertEquals("Only one ssl handshake expected.", 1, handshakeCompletedListener.getHandshakeEvents().size());
                response.close();
            }
        } finally {
            close(client);
        }
    }

    private static class CustomHandshakeCompletedListener implements HandshakeCompletedListener {

        private final List<HandshakeCompletedEvent> handshakeEvents = new CopyOnWriteArrayList<HandshakeCompletedEvent>();

        @Override
        public void handshakeCompleted(HandshakeCompletedEvent event) {
            handshakeEvents.add(event);
        }

        public List<HandshakeCompletedEvent> getHandshakeEvents() {
            return handshakeEvents;
        }

    }

    private static class CustomSSLSocketFactory extends SSLSocketFactory {

        private SSLContext sslContext;
        private String[] supportedCipherSuites;
        private String[] supportedProtocols;
        private HandshakeCompletedListener handshakeCompletedListener;

        public CustomSSLSocketFactory(SSLContext sslContext, HandshakeCompletedListener handshakeCompletedListener) {
            this.sslContext = sslContext;
            this.handshakeCompletedListener = handshakeCompletedListener;
            init();
        }

        private void init() {
            supportedCipherSuites = sslContext.getSocketFactory().getSupportedCipherSuites();
            supportedProtocols = new String[] {"TLSv1", "TLSv1.1", "TLSv1.2"};
        }

        @Override
        public String[] getDefaultCipherSuites() {
            return supportedCipherSuites;
        }

        @Override
        public String[] getSupportedCipherSuites() {
            return supportedCipherSuites;
        }

        private SSLSocket configureSocket(SSLSocket sslSocket) throws IOException {
            if (handshakeCompletedListener != null) {
                sslSocket.addHandshakeCompletedListener(handshakeCompletedListener);
            }
            sslSocket.setEnabledCipherSuites(supportedCipherSuites);
            sslSocket.setEnabledProtocols(supportedProtocols);
            sslSocket.setNeedClientAuth(true);
            sslSocket.setKeepAlive(true);

            return sslSocket;
        }

        public Socket createSocket(Socket s, String host, int port, boolean autoClose) throws IOException {
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(s, host, port, true);

            return configureSocket(sslSocket);
        }

        @Override
        public Socket createSocket(InetAddress address, int port, InetAddress localAddress, int localPort) throws IOException {
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(address, port, localAddress, localPort);

            return configureSocket(sslSocket);
        }

        @Override
        public Socket createSocket(String host, int port, InetAddress localHost, int localPort) throws IOException {
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port, localHost, localPort);

            return configureSocket(sslSocket);
        }

        @Override
        public Socket createSocket(InetAddress host, int port) throws IOException {
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);

            return configureSocket(sslSocket);
        }

        @Override
        public Socket createSocket(String host, int port) throws IOException {
            SSLSocketFactory sslSocketFactory = sslContext.getSocketFactory();
            SSLSocket sslSocket = (SSLSocket) sslSocketFactory.createSocket(host, port);

            return configureSocket(sslSocket);
        }

    }

    @Path("/")
    public static class TestResource {

        @GET
        @Produces("text/plain")
        public String get() {
            return "GET";
        }

    }

}
