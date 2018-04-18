package org.glassfish.jersey.linking.integration.representations;

import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLinks;
import org.glassfish.jersey.linking.integration.app.InfoResource;

@InjectLinks({
        @InjectLink(resource = InfoResource.class, method = "info", rel = "self")
})
public class Info {
    private String version;

    public String getVersion() {
        return version;
    }

    public void setVersion(String version) {
        this.version = version;
    }
}
