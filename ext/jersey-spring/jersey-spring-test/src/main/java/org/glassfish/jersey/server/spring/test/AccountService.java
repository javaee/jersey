package org.glassfish.jersey.server.spring.test;

import java.math.BigDecimal;

public interface AccountService {
    void setAccountBalance(String accountId, BigDecimal balance);
    BigDecimal getAccountBalance(String accountId);
}
