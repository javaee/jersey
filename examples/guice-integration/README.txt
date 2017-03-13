Guice interaction example

This example demonstrates the direct integration of Jersey JAX-RS endpoints 
with Guice servlet module without any additional libraries, allowing Guice 
to take care of all the services, endpoints, injections and URL mappings, 
plus seving JAX-RS endpoints through Jersey servlet.

Jersey endpoint will be available at:
http://localhost:8080/guice-interaction/hello

Application uses Servlet 3.0 and annotations to initialize all the required parts.

Points of note by priority:

GuiceServletModule ....... Guice module reponsible for setting up all the service 
                           bindings and the Jersey servlet.

JerseyApplication ........ JAX-RS Application implementation, created and injected
                           by Guice. All the Guice-injected endpoints are returned
                           as singletons.

ResourceConfigProvider ... Guice provider implementation, wrapping JAX-RS Application
                           into ResourceConfig, required by Jersey servlet.

JerseyServlet ............ extension of Jersey ServletContainer, only adding Guice 
                           annotations to mark as Singleton (required for servlets) 
                           and inject ResourceConfig in constructor.

GuiceFilterAnnotated ..... empty extension of GuiceFilter to add Servlet 3.0 annotations.

GuiceContextListener ..... context listener to initialize Guice injector.

HelloEndpoint ............ sample JAX-RS endpoint, created and injected by Guice.

HelloService(Impl) ....... sample service, created and injected by Guice.
