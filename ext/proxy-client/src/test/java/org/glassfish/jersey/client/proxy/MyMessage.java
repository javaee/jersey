package org.glassfish.jersey.client.proxy;

public class MyMessage {
    private final String value;
    private String name;

    public MyMessage(String value) {
        this.value = value;
    }

    public String getValue() {
        return value;
    }
}