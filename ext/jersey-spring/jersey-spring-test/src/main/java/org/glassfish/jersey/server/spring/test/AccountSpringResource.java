package org.glassfish.jersey.server.spring.test;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.logging.Logger;

/**
 * Spring managed JAX-RS resource for testing jersey-spring.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
@Path("/spring/account")
@Component
public class AccountSpringResource {
    private static final Logger LOGGER = Logger.getLogger(AccountSpringResource.class.getName());

    @Inject
    @Named("AccountService-singleton")
    private AccountService accountServiceInject;

    @Autowired
    @Qualifier("AccountService-singleton")
    private AccountService accountServiceAutowired;

//    @Autowired
//    @Qualifier("AccountService-request-1")
    private AccountService accountServiceRequest1;

//    @Autowired
//    @Qualifier("AccountService-request-1")
    private AccountService accountServiceRequest2;

    @Autowired
    @Qualifier("AccountService-prototype-1")
    private AccountService accountServicePrototype1;

    @Autowired
    @Qualifier("AccountService-prototype-1")
    private AccountService accountServicePrototype2;

    @Inject
    private HK2ServiceSingleton hk2Singleton;

//    @Inject
    private HK2ServiceRequestScoped hk2RequestScoped;

    @Inject
    private HK2ServicePerLookup hk2PerLookup;


    private String message = "n/a";

    // resource methods for testing resource class scope
    @GET
    @Path("message")
    public String getMessage() {
        return message;
    }

    @PUT
    @Path("message")
    @Consumes(MediaType.TEXT_PLAIN)
    public void setMessage(String message) {
        this.message = message;
    }

    // resource methods for testing singleton scoped beans
    @GET
    @Path("singleton/inject/{accountId}")
    public BigDecimal getAccountBalanceSingletonInject(@PathParam("accountId") String accountId) {
        return accountServiceInject.getAccountBalance(accountId);
    }

    @GET
    @Path("singleton/autowired/{accountId}")
    public BigDecimal getAccountBalanceSingletonAutowired(@PathParam("accountId") String accountId) {
        return accountServiceAutowired.getAccountBalance(accountId);
    }

    @PUT
    @Path("singleton/{accountId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public void setAccountBalanceSingleton(@PathParam("accountId") String accountId, String balance) {
        accountServiceInject.setAccountBalance(accountId, new BigDecimal(balance));
    }

    // resource methods for testing request scoped beans
    @PUT
    @Path("request/{accountId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public BigDecimal setAccountBalanceRequest(@PathParam("accountId") String accountId, String balance) {
        accountServiceRequest1.setAccountBalance(accountId, new BigDecimal(balance));
        return accountServiceRequest2.getAccountBalance(accountId);
    }

    // resource methods for testing prototype scoped beans
    @PUT
    @Path("prototype/{accountId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public BigDecimal setAccountBalancePrototype(@PathParam("accountId") String accountId, String balance) {
        accountServicePrototype1.setAccountBalance(accountId, new BigDecimal(balance));
        return accountServicePrototype2.getAccountBalance(accountId);
    }

}
