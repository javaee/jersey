package org.glassfish.jersey.undertow;

import io.undertow.Undertow;
import org.glassfish.jersey.server.ResourceConfig;

import java.net.URI;

public class UndertowHttpContainerFactory {
    public static Undertow createServer(URI uri, ResourceConfig config) {
        return createServerInternal(uri, config, true);
    }

    public static Undertow createServer(URI uri, ResourceConfig config, boolean start) {
        return createServerInternal(uri, config, start);
    }

    private static Undertow createServerInternal(URI uri, ResourceConfig config, boolean start) {
        // TODO: check for null config
        // TODO: check for null URI
        Undertow server = Undertow.builder()
                .addHttpListener(uri.getPort(), uri.getHost())
                .setHandler(new UndertowHttpContainer(config))
                .build();

        if (start) {
            server.start();
        }

        return server;
    }
}
