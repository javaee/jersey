package org.glassfish.jersey.server.spring.test;

import java.math.BigDecimal;

public class AccountServiceJersey implements AccountService {

    @Override
    public void setAccountBalance(String accountId, BigDecimal balance) {
        //To change body of implemented methods use File | Settings | File Templates.
    }

    @Override
    public BigDecimal getAccountBalance(String accountId) {
        return null;  //To change body of implemented methods use File | Settings | File Templates.
    }
}
