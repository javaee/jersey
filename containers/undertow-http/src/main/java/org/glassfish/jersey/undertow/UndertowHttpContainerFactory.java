package org.glassfish.jersey.undertow;

import io.undertow.Undertow;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.spi.Container;
import org.glassfish.jersey.undertow.internal.LocalizationMessages;

import javax.net.ssl.SSLContext;
import java.net.URI;

/**
 * Factory for creating and starting Undertow server handlers. This returns
 * a handle to the started server as {@link Undertow} instances, which allows
 * the server to be stopped by invoking the {@link Undertow#stop()} method.
 * <p/>
 * To start the server in HTTPS mode an {@link SSLContext} can be provided.
 * This will be used to decrypt and encrypt information sent over the
 * connected TCP socket channel.
 *
 * @author Jonathan Como (jonathan.como at gmail.com)
 */
public class UndertowHttpContainerFactory {
    public static Undertow createServer(URI uri, ResourceConfig config) {
        return createServer(uri, null, config, true);
    }

    public static Undertow createServer(URI uri, ResourceConfig config, boolean start) {
        return createServer(uri, null, config, start);
    }

    public static Undertow createServer(URI uri, SSLContext sslContext, ResourceConfig config) {
        return createServer(uri, sslContext, config, true);
    }

    public static Undertow createServer(URI uri, SSLContext sslContext, ResourceConfig config, boolean start) {
        if (uri == null) {
            throw new IllegalArgumentException(LocalizationMessages.URI_CANNOT_BE_NULL());
       }

        int defaultPort;
        String scheme = uri.getScheme();

        if (sslContext != null) {
            defaultPort = Container.DEFAULT_HTTPS_PORT;
            if (!"https".equals(scheme)) {
                throw new IllegalArgumentException(LocalizationMessages.WRONG_SCHEME_WHEN_USING_HTTPS());
            }
        } else {
            defaultPort = Container.DEFAULT_HTTP_PORT;
            if (!"http".equals(scheme)) {
                throw new IllegalArgumentException(LocalizationMessages.WRONG_SCHEME_WHEN_USING_HTTP());
            }
        }

        int port = uri.getPort();
        if (port == -1) {
            port = defaultPort;
        }

        Undertow.Builder builder = Undertow.builder()
                .setHandler(new UndertowHttpContainer(config));

        if (sslContext != null) {
            builder.addHttpsListener(port, uri.getHost(), sslContext);
        } else {
            builder.addHttpListener(port, uri.getHost());
        }

        Undertow server = builder.build();
        if (start) {
            server.start();
        }

        return server;
    }
}
