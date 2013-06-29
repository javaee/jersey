package org.glassfish.jersey.server.spring;

import org.springframework.web.WebApplicationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import java.util.logging.Logger;

/**
 * Spring WebApplicationInitializer implementation initializes Spring context by
 * adding a Spring ContextLoaderListener to the ServletContext.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class SpringWebApplicationInitializer implements WebApplicationInitializer {
    private static final Logger LOGGER = Logger.getLogger(SpringWebApplicationInitializer.class.getName());
    private static final String PAR_NAME_CTX_CONFIG_LOCATION = "contextConfigLocation";

    @Override
    public void onStartup(ServletContext sc) throws ServletException {
        LOGGER.fine("onStartup()");
        if(sc.getInitParameter(PAR_NAME_CTX_CONFIG_LOCATION) == null) {
            LOGGER.info("jersey-spring: registering Spring ContextLoaderListener");
            sc.setInitParameter(PAR_NAME_CTX_CONFIG_LOCATION, "classpath:applicationContext.xml");
            sc.addListener("org.springframework.web.context.ContextLoaderListener");
            sc.addListener("org.springframework.web.context.request.RequestContextListener");
        } else {
            LOGGER.info("jersey-spring: assuming Spring ContextLoaderListener was manually registered");
        }
    }
}
