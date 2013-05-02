package org.glassfish.jersey.server.spring;

import javax.servlet.ServletContext;
import javax.servlet.ServletException;

import org.springframework.web.WebApplicationInitializer;

/**
* Spring WebApplicationInitializer implementation initializes Spring context by
* adding a Spring ContextLoaderListener to the ServletContext.
*
* @author Marko Asplund (marko.asplund at gmail.com)
*/
public class SpringWebApplicationInitializer implements WebApplicationInitializer {

    @Override
    public void onStartup(ServletContext sc) throws ServletException {
        sc.setInitParameter("contextConfigLocation", "classpath:applicationContext.xml");
        sc.addListener("org.springframework.web.context.ContextLoaderListener");
    }

}
