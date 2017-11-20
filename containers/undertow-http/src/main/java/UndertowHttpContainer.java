import io.undertow.Undertow;
import io.undertow.security.idm.Account;
import io.undertow.server.HttpHandler;
import io.undertow.server.HttpServerExchange;
import org.glassfish.jersey.internal.MapPropertiesDelegate;
import org.glassfish.jersey.server.ApplicationHandler;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.internal.ContainerUtils;
import org.glassfish.jersey.server.spi.Container;

import javax.ws.rs.core.SecurityContext;
import java.net.URI;
import java.net.URISyntaxException;
import java.security.Principal;

public class UndertowHttpContainer implements HttpHandler, Container {
    private volatile ApplicationHandler appHandler;

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
    public void reload(ResourceConfig configuration) {
        appHandler.onShutdown(this);

        appHandler = new ApplicationHandler(configuration);
        appHandler.onReload(this);
        appHandler.onStartup(this);
    }

    @Override
    public void handleRequest(HttpServerExchange exchange) throws Exception {
        // FIXME: base path is basically the mount point. How do we handle this with undertow?
        // FIXME: possibilities: get it at creation time (is this part of the xml), or use the exchange
        // FIXME: might be servlet only though?

        // TODO: include the host header
        URI baseUri = getBaseUri(exchange);
        ContainerRequest request = new ContainerRequest(baseUri, getRequestUri(exchange, baseUri),
                exchange.getRequestMethod().toString(), new UndertowSecurityContext(exchange),
                new MapPropertiesDelegate());
    }

    private URI getBaseUri(HttpServerExchange exchange) {
        try {
            return new URI(exchange.getRequestScheme(), null, exchange.getHostName(),
                    exchange.getHostPort(), "/", null, null);
        } catch (URISyntaxException e) {
            throw new IllegalArgumentException(e);
        }
    }

    private URI getRequestUri(HttpServerExchange exchange, URI baseUri) {
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

    private String getServerAddress(URI baseUri) {
        String serverAddress = baseUri.toString();
        if (serverAddress.charAt(serverAddress.length() - 1) == '/') {
            return serverAddress.substring(0, serverAddress.length() - 1);
        }

        return serverAddress;
    }

    private static final class UndertowSecurityContext implements SecurityContext {
        private HttpServerExchange exchange;

        UndertowSecurityContext(HttpServerExchange exchange) {
            this.exchange = exchange;
        }

        @Override
        public Principal getUserPrincipal() {
            Account account = getAccount();
            if (account != null) {
                return account.getPrincipal();
            }

            return null;
        }

        @Override
        public boolean isUserInRole(String role) {
            Account account = getAccount();
            return account != null && account.getRoles().contains(role);
        }

        @Override
        public boolean isSecure() {
            return exchange.isSecure();
        }

        @Override
        public String getAuthenticationScheme() {
            return exchange.getSecurityContext().getMechanismName();
        }

        private Account getAccount() {
            if (exchange.getSecurityContext() != null) {
                return exchange.getSecurityContext().getAuthenticatedAccount();
            }

            return null;
        }
    }

    public static void main(String[] args) {
        Undertow server = Undertow.builder()
                .addHttpListener(8080, "localhost")
                .setHandler(new UndertowHttpContainer())
                .build();

        server.start();
    }
}
