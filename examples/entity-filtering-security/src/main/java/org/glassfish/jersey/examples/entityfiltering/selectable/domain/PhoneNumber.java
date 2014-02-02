package org.glassfish.jersey.examples.entityfiltering.selectable.domain;

import javax.xml.bind.annotation.XmlRootElement;

@XmlRootElement
public class PhoneNumber {

    private String areaCode;

    private String number;

    public String getNumber() {
        return number;
    }

    public void setNumber(String number) {
        this.number = number;
    }

    public String getAreaCode() {
        return areaCode;
    }

    public void setAreaCode(String areaCode) {
        this.areaCode = areaCode;
    }

}

/*
 * Copyright 2013 Capital One Financial Corporation All Rights Reserved.
 * 
 * This software contains valuable trade secrets and proprietary information of Capital One and is protected by law. It
 * may not be copied or distributed in any form or medium, disclosed to third parties, reverse engineered or used in any
 * manner without prior written authorization from Capital One.
 */
