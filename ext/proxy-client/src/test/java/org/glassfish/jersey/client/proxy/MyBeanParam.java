package org.glassfish.jersey.client.proxy;

import javax.ws.rs.CookieParam;
import javax.ws.rs.HeaderParam;
import javax.ws.rs.QueryParam;

/**
 * @author Lucas Sa
 */
public class MyBeanParam {
    @QueryParam("name") private String queryName;

    @CookieParam("name") private  String cookieName;

    @HeaderParam("name") private String headerName;

    private String setterQueryName;

    private String setterCookieName;

    private String setterHeaderName;

    public String getQueryName() {
        return queryName;
    }

    public void setQueryName(String queryName) {
        this.queryName = queryName;
    }

    public String getCookieName() {
        return cookieName;
    }

    public void setCookieName(String cookieName) {
        this.cookieName = cookieName;
    }

    public String getHeaderName() {
        return headerName;
    }

    public void setHeaderName(String headerName) {
        this.headerName = headerName;
    }

    public String getSetterQueryName() {
        return setterQueryName;
    }

    @QueryParam("setterName")
    public void setSetterQueryName(String setterQueryName) {
        this.setterQueryName = setterQueryName;
    }

    public String getSetterCookieName() {
        return setterCookieName;
    }

    @CookieParam("setterName")
    public void setSetterCookieName(String setterCookieName) {
        this.setterCookieName = setterCookieName;
    }

    public String getSetterHeaderName() {
        return setterHeaderName;
    }

    @HeaderParam("setterName")
    public void setSetterHeaderName(String setterHeaderName) {
        this.setterHeaderName = setterHeaderName;
    }
}
