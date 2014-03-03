package org.glassfish.jersey.examples.hello.spring.annotations.annotations;

import org.springframework.stereotype.Component;

@Component
public class EnglishGoodbyeService implements GoodbyeService {

    @Override
    public String goodbye(final String who) {
        return String.format("goodbye, %s!", who);
    }
}
