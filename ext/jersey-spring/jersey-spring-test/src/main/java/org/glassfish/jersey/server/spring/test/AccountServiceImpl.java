package org.glassfish.jersey.server.spring.test;

import java.math.BigDecimal;
import java.util.HashMap;
import java.util.Map;

/**
 * AccountService implementation.
 *
 * @author Marko Asplund (marko.asplund at yahoo.com)
 */
public class AccountServiceImpl implements AccountService {
    private Map<String, BigDecimal> accounts = new HashMap<String, BigDecimal>();
    private BigDecimal defaultAccountBalance;

    @Override
    public void setAccountBalance(String accountId, BigDecimal balance) {
        accounts.put(accountId, balance);
    }

    @Override
    public BigDecimal getAccountBalance(String accountId) {
        BigDecimal balance = accounts.get(accountId);
        if(balance == null) {
            return defaultAccountBalance;
        }
        return balance;
    }

    public void setDefaultAccountBalance(String defaultAccountBalance) {
        this.defaultAccountBalance = new BigDecimal(defaultAccountBalance);
    }

}
