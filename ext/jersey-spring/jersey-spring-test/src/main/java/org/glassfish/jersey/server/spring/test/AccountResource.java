package org.glassfish.jersey.server.spring.test;

import org.springframework.stereotype.Component;

import javax.inject.Inject;
import javax.inject.Named;
import javax.ws.rs.GET;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.core.UriInfo;
import java.math.BigDecimal;

@Path("/account")
@Component
public class AccountResource {

    @Inject
    @Named("AccountService-1")
    private AccountService accountService;

    @Inject
    private DummyHK2Service dm;

    @Inject
    private UriInfo uriInfo;

    @GET
    @Path("{accountId}")
    public BigDecimal getAccountBalance(@PathParam("accountId") String accountId) {
        System.out.println(String.format("AccountJerseyResource: %s; AccountServiceSpring: %s", this, accountService));
        System.out.println("dummy: "+dm);
        return accountService.getAccountBalance(accountId);
    }

    @PUT
    @Path("{accountId}")
    public void setAccountBalance(@PathParam("accountId") String accountId, String balance) {
        System.out.println(String.format("AccountJerseyResource: %s; AccountServiceSpring: %s", this, accountService));
        System.out.println("dummy: "+dm);
        accountService.setAccountBalance(accountId, new BigDecimal(balance));
    }

}
