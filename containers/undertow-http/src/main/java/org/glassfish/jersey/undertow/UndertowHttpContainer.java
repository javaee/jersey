package org.glassfish.jersey.undertow;

import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import io.undertow.util.HeaderValues;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.ContainerUtils;
import org.glassfish.jersey.server.spi.Container;

import javax.ws.rs.core.Application;
import java.net.URI;
import java.net.URISyntaxException;

public class UndertowHttpContainer implements HttpHandler, Container {
    private volatile ApplicationHandler appHandler;

    UndertowHttpContainer(final Application application) {
        appHandler = new ApplicationHandler(application);

        // No lifecycle hooks for Undertow's server, so we do this for
        // completeness but not accuracy.
        appHandler.onStartup(this);
    }

    @Override
    public ApplicationHandler getApplicationHandler() {
        return appHandler;
    }

    @Override
    public ResourceConfig getConfiguration() {
        return appHandler.getConfiguration();
    }

    @Override
    public void reload() {
        reload(getConfiguration());
    }

    @Override
    public void reload(final ResourceConfig configuration) {
        appHandler.onShutdown(this);

        appHandler = new ApplicationHandler(configuration);
        appHandler.onReload(this);
        appHandler.onStartup(this);
    }

    @Override
    public void handleRequest(final HttpServerExchange exchange) throws Exception {
        // If we ware on the IO thread (where the raw HTTP request is processed),
        // we want to dispatch to a worker. This is the preferred approach.
        if (exchange.isInIoThread()) {
            exchange.dispatch(this);
            return;
        }

        exchange.startBlocking();
        URI baseUri = getBaseUri(exchange);
        ContainerRequest request = new ContainerRequest(baseUri, getRequestUri(exchange, baseUri),
                exchange.getRequestMethod().toString(), new UndertowSecurityContext(exchange),
                new MapPropertiesDelegate());

        request.setEntityStream(exchange.getInputStream());
        request.setWriter(new UndertowResponseWriter(exchange));
        for (HeaderValues values : exchange.getRequestHeaders()) {
            String name = values.getHeaderName().toString();
            for (String value : values) {
                request.header(name, value);
            }
        }

        appHandler.handle(request);
    }

    // TODO: No easy way to get the mount point without using Undertow servlets library
    private URI getBaseUri(final HttpServerExchange exchange) {
        try {
            return new URI(exchange.getRequestScheme(), null, exchange.getHostName(),
                    exchange.getHostPort(), "/", null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private URI getRequestUri(final HttpServerExchange exchange, final URI baseUri) {
        String uri = getServerAddress(baseUri) + exchange.getRequestURI();
        String query = exchange.getQueryString();
        if (query != null && !query.isEmpty()) {
            uri += "?" + ContainerUtils.encodeUnsafeCharacters(query);
        }

        try {
            return new URI(uri);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private String getServerAddress(final URI baseUri) {
        String serverAddress = baseUri.toString();
        if (serverAddress.charAt(serverAddress.length() - 1) == '/') {
            return serverAddress.substring(0, serverAddress.length() - 1);
        }

        return serverAddress;
    }
}
