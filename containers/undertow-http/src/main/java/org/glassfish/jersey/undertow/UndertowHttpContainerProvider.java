package org.glassfish.jersey.undertow;

import io.undertow.server.HttpHandler;
import org.glassfish.jersey.server.spi.ContainerProvider;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.core.Application;

/**
 * Container provider for containers based on Undertow Server {@link io.undertow.Undertow}.
 *
 * @author Jonathan Como (jonathan.como at gmail.com)
 */
public final class UndertowHttpContainerProvider implements ContainerProvider {

    @Override
    public <T> T createContainer(final Class<T> type, final Application application) throws ProcessingException {
        if (HttpHandler.class == type || UndertowHttpContainer.class == type) {
            return type.cast(new UndertowHttpContainer(application));
        }

        return null;
    }
}
