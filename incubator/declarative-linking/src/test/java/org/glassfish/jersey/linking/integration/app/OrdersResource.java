/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
 *
 * GPL Classpath Exception:
 * Oracle designates this particular file as subject to the "Classpath"
 * exception as provided by Oracle in the GPL Version 2 section of the License
 * file that accompanied this code.
 *
 * Modifications:
 * If applicable, add the following below the License Header, with the fields
 * enclosed by brackets [] replaced by your own identifying information:
 * "Portions Copyright [year] [name of copyright owner]"
 *
 * Contributor(s):
 * If you wish your version of this file to be governed by only the CDDL or
 * only the GPL Version 2, indicate your decision by adding "[Contributor]
 * elects to include this software in this distribution under the [CDDL or GPL
 * Version 2] license."  If you don't indicate a single choice of license, a
 * recipient has the option to distribute your version of this file under
 * either the CDDL, the GPL Version 2 or to extend the choice of license to
 * its licensees as provided above.  However, if you add GPL Version 2 code
 * and therefore, elected the GPL Version 2 license, then the option applies
 * only if the new code is made subject to such option by the copyright
 * holder.
 */

package org.glassfish.jersey.linking.integration.app;

import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriInfo;

import org.glassfish.jersey.linking.Binding;
import org.glassfish.jersey.linking.ProvideLink;
import org.glassfish.jersey.linking.integration.representations.ExtendedOrder;
import org.glassfish.jersey.linking.integration.representations.Info;
import org.glassfish.jersey.linking.integration.representations.Order;
import org.glassfish.jersey.linking.integration.representations.OrderPage;
import org.glassfish.jersey.linking.integration.representations.OrderRequest;
import org.glassfish.jersey.linking.integration.representations.PageLinks;
import org.glassfish.jersey.linking.integration.representations.PaymentConfirmation;


@Path("/orders")
public class OrdersResource {

    @Context
    private UriInfo uriInfo;


    @ProvideLink(value = OrderPage.class, rel = "create")
    @ProvideLink(value = Info.class, rel = "create-order")
    @Consumes(MediaType.APPLICATION_JSON)
    @Produces(MediaType.APPLICATION_JSON)
    @POST
    public Response create(OrderRequest request) {
        Order order = new Order();
        order.setId("123");
        if ("water".equalsIgnoreCase(request.getDrink())) {
            order.setPrice("0.0");
        } else {
            order.setPrice("1.99");
        }

        order.getLinks().add(Link.fromUri("/").rel("root").build());
        return Response.ok(order).build();
    }

    @ProvideLink(value = Order.class, rel = "self", bindings = @Binding(name = "orderId", value = "${instance.id}"))
    @ProvideLink(value = PaymentConfirmation.class, rel = "order",
                 bindings = @Binding(name = "orderId", value = "${instance.orderId}"))
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    @Path("/{orderId}")
    public Response get(@PathParam("orderId") String orderId) {
        ExtendedOrder order = new ExtendedOrder();
        order.setId("123");
        order.setPrice("1.99");
        return Response.ok(order).build();
    }


    @ProvideLink(value = ExtendedOrder.class, rel = "delete",
                 bindings = @Binding(name = "orderId", value = "${instance.id}"))
    @DELETE
    @Path("/{orderId}")
    public Response delete(@PathParam("orderId") String orderId) {
        return Response.noContent().build();
    }


    @PageLinks(OrderPage.class)
    @Produces(MediaType.APPLICATION_JSON)
    @GET
    public Response list(@QueryParam("page") @DefaultValue("0") int page, @QueryParam("size") @DefaultValue("2")  int size) {
        OrderPage orderPage = new OrderPage();

        orderPage.setFirstPage(page == 0);
        orderPage.setLastPage(page == 2);
        orderPage.setPreviousPageAvailable(page > 0);
        orderPage.setNextPageAvailable(page < 2);
        orderPage.setNumber(page);
        orderPage.setSize(size);
        orderPage.setTotalElements(6);
        orderPage.setTotalPages(3);

        orderPage.setOrders(generateOrders(page, size));

        return Response.ok(orderPage).build();
    }

    private List<Order> generateOrders(int page, int size) {
        final int base = page * size;
        return IntStream.range(1, size + 1).map(x -> x + base).mapToObj(id -> {
            Order order = new Order();
            order.setId(Integer.toString(id));
            order.setPrice(((id & 1) == 1) ? "1.99" : "0.0");
            return order;
        }).collect(Collectors.toList());
    }
}
