package org.glassfish.jersey.undertow;

import io.undertow.server.HttpServerExchange;
import io.undertow.util.HttpString;
import io.undertow.util.StatusCodes;
import org.glassfish.jersey.server.ContainerException;
import org.glassfish.jersey.server.ContainerResponse;
import org.glassfish.jersey.server.spi.ContainerResponseWriter;

import java.io.OutputStream;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

class UndertowResponseWriter implements ContainerResponseWriter {
    private final HttpServerExchange exchange;

    UndertowResponseWriter(final HttpServerExchange exchange) {
        this.exchange = exchange;
    }

    @Override
    public OutputStream writeResponseStatusAndHeaders(final long contentLength, final ContainerResponse context) throws ContainerException {
        exchange.setStatusCode(context.getStatus());

        Map<String, List<String>> headers = context.getStringHeaders();
        for (Map.Entry<String, List<String>> e : headers.entrySet()) {
            HttpString name = new HttpString(e.getKey());
            exchange.getResponseHeaders().addAll(name, e.getValue());
        }

        return exchange.getOutputStream();
    }

    @Override
    public boolean suspend(final long timeOut, final TimeUnit timeUnit, final TimeoutHandler timeoutHandler) {
        return false;
    }

    @Override
    public void setSuspendTimeout(final long timeOut, final TimeUnit timeUnit) throws IllegalStateException {
    }

    @Override
    public void commit() {
        exchange.endExchange();
    }

    @Override
    public void failure(final Throwable error) {
        if (!exchange.isResponseStarted()) {
            exchange.setStatusCode(StatusCodes.INTERNAL_SERVER_ERROR);
            exchange.getResponseSender().send("Internal Server Error");
        }

        commit();

        // Rethrow the original exception as required by JAX-RS, 3.3.4.
        if (error instanceof RuntimeException) {
            throw (RuntimeException) error;
        } else {
            throw new ContainerException(error);
        }
    }

    @Override
    public boolean enableResponseBuffering() {
        return false;
    }
}

