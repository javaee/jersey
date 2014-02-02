package org.glassfish.jersey.examples.entityfiltering.selectable.domain;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class Address {

    private String streetAddress;

    private String region;

    private PhoneNumber phoneNumber;

    public String getStreetAddress() {
        return streetAddress;
    }

    public void setStreetAddress(String streetAddress) {
        this.streetAddress = streetAddress;
    }

    public String getRegion() {
        return region;
    }

    public void setRegion(String region) {
        this.region = region;
    }

    public PhoneNumber getPhoneNumber() {
        return phoneNumber;
    }

    public void setPhoneNumber(PhoneNumber phoneNumber) {
        this.phoneNumber = phoneNumber;
    }

}

/*
 * Copyright 2013 Capital One Financial Corporation All Rights Reserved.
 * 
 * This software contains valuable trade secrets and proprietary information of Capital One and is protected by law. It
 * may not be copied or distributed in any form or medium, disclosed to third parties, reverse engineered or used in any
 * manner without prior written authorization from Capital One.
 */
