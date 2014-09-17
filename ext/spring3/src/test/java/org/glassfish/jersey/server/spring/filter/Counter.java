package org.glassfish.jersey.server.spring.filter;

import org.springframework.stereotype.Component;

@Component
public class Counter {

    private int count = 0;

    public void inc() {
        count++;
    }

    public int getCount() {
        return count;
    }

}
