package org.glassfish.jersey.examples.osgihttpservice.spring.impl;

import org.glassfish.jersey.examples.osgihttpservice.spring.StatusGenerator;

public class DefaultStatusGenerator implements StatusGenerator {

    @Override
    public String status() {
        return "status from generator";
    }
}
