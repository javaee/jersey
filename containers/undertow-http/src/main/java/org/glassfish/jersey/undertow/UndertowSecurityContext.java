package org.glassfish.jersey.undertow;

import io.undertow.security.idm.Account;
import io.undertow.server.HttpServerExchange;

import javax.ws.rs.core.SecurityContext;
import java.security.Principal;

class UndertowSecurityContext implements SecurityContext {
    private final HttpServerExchange exchange;

    UndertowSecurityContext(final HttpServerExchange exchange) {
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
    public boolean isUserInRole(final String role) {
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
