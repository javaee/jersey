package org.glassfish.jersey.tests.integration.jersey2689;

import org.hibernate.validator.constraints.NotEmpty;

public class SampleBean {
    
    @NotEmpty
    private byte[] array;
    
    public byte[] getArray() {
        return array;
    }
    public void setArray(byte[] array) {
        this.array = array;
    }

}
