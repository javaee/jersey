package org.glassfish.jersey.client.proxy;

import javax.ws.rs.CookieParam;
import javax.ws.rs.FormParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.MatrixParam;
import javax.ws.rs.PathParam;
import javax.ws.rs.QueryParam;
import java.util.List;
import java.util.SortedSet;

public class MyBeanParam {
    @QueryParam("query-param") String queryParam;
    @PathParam("path-param") String pathParam;
    @HeaderParam("header-param") String headerParam;
    @CookieParam("cookie-param") String cookieParam;
    @MatrixParam("matrix-name-sorted") SortedSet<String> matrixParam;
    @FormParam("form-param") List<String> formParam;

    @Override
    public String toString() {
        return String.format("%s:%s:%s:%s:%s:%s",
                queryParam,
                pathParam,
                headerParam,
                cookieParam,
                matrixParam,
                formParam);
    }
}
