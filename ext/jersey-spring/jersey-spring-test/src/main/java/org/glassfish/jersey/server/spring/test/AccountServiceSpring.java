package org.glassfish.jersey.server.spring.test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

public class AccountServiceSpring implements AccountService {
    private Map<String, BigDecimal> accounts = new HashMap<String, BigDecimal>();

    @Override
    public void setAccountBalance(String accountId, BigDecimal balance) {
        accounts.put(accountId, balance);
    }

    @Override
    public BigDecimal getAccountBalance(String accountId) {
        return accounts.get(accountId);
    }
}
