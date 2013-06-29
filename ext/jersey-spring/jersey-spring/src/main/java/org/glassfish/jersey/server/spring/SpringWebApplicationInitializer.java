package org.glassfish.jersey.server.spring;

import org.springframework.web.WebApplicationInitializer;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

/**
* Spring WebApplicationInitializer implementation initializes Spring context by
* adding a Spring ContextLoaderListener to the ServletContext.
*
* @author Marko Asplund (marko.asplund at yahoo.com)
*/
public class SpringWebApplicationInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext sc) throws ServletException {
        // TODO: check that ContextLoaderListener hasn't been explicitly registered.
        // TODO: enable configuring contextConfigLocation?
        sc.setInitParameter("contextConfigLocation", "classpath:applicationContext.xml");
        sc.addListener("org.springframework.web.context.ContextLoaderListener");
    }

}
