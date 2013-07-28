package org.glassfish.jersey.server.spring.test.jerseymanaged;

import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.spring.test.AccountService;
import org.glassfish.jersey.server.spring.test.DummyHK2Service;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.*;
import javax.ws.rs.core.MediaType;
import java.math.BigDecimal;
import java.util.logging.Logger;

@Path("/jersey/account")
public class AccountJerseyResource {
    private static final Logger LOGGER = Logger.getLogger(AccountJerseyResource.class.getName());

    @Inject
    @Named("AccountService-1")
    private AccountService accountServiceInject;

    @Autowired
    @Qualifier("AccountService-2")
    private AccountService accountServiceAutowired;

    @Inject
    private DummyHK2Service dummyHk2Service;

    @GET
    public String getHello() {
        LOGGER.fine("getHello(): "+dummyHk2Service);
        return "hello";
    }

    // inject / singleton
    @GET
    @Path("inject/{accountId}")
    public BigDecimal getAccountBalance1(@PathParam("accountId") String accountId) {
        return accountServiceInject.getAccountBalance(accountId);
    }

    @PUT
    @Path("inject/{accountId}")
    @Consumes(MediaType.TEXT_PLAIN)
    public void setAccountBalance1(@PathParam("accountId") String accountId, String balance) {
        System.out.println("XX accountService: "+accountServiceAutowired);
        System.out.println("XX accountService: "+accountServiceInject);
        accountServiceInject.setAccountBalance(accountId, new BigDecimal(balance));
    }


    // inject / request

    // inject / perlookup

    // autowired / singleton

    // autowired / request

    // autowired / perlookup




}
