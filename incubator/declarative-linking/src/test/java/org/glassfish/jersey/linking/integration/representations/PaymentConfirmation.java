package org.glassfish.jersey.linking.integration.representations;

import java.util.ArrayList;
import java.util.List;
import javax.ws.rs.core.Link;

import org.glassfish.jersey.linking.InjectLink;
import org.glassfish.jersey.linking.InjectLinks;
import org.glassfish.jersey.linking.integration.app.PaymentResource;

public class PaymentConfirmation {

    private String id;

    private String orderId;

    @InjectLinks(
            @InjectLink(resource = PaymentResource.class, method = "getConfirmation", rel = "self")
    )
    private List<Link> links = new ArrayList<>();

    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getOrderId() {
        return orderId;
    }

    public void setOrderId(String orderId) {
        this.orderId = orderId;
    }

    public List<Link> getLinks() {
        return links;
    }

    public void setLinks(List<Link> links) {
        this.links = links;
    }
}
