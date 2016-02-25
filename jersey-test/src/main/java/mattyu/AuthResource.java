package mattyu;

import org.glassfish.jersey.client.oauth2.ClientIdentifier;
import org.glassfish.jersey.client.oauth2.OAuth2ClientSupport;
import org.glassfish.jersey.client.oauth2.OAuth2CodeGrantFlow;
import org.glassfish.jersey.client.oauth2.TokenResult;

import javax.servlet.http.HttpServletRequest;
import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.QueryParam;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.NewCookie;
import javax.ws.rs.core.Response;
import java.net.URI;

/**
 * Hello world!
 *
 */

@Path("/oauth")
public class AuthResource {

    public static final String HUB_ID = "0-0-0-0-0";
    public static final String HUB_SECRET = "exwwEnO9OUcu";
    public static final String HUB_AUTH_URL = "http://mattys-macbook-air.local:8787/hub/api/rest/oauth2/auth";
    public static final String HUB_TOKEN_URL = "http://mattys-macbook-air.local:8787/hub/api/rest/oauth2/token";
    public static final String AUTH_TOKEN_REDIRECT = "http://mattys-macbook-air.local:9999/v1/oauth/token";

    @GET
    @Path("/auth")
    public Response authResponse(@Context HttpServletRequest request) {
        ClientIdentifier clientId = new ClientIdentifier(HUB_ID, HUB_SECRET);
        OAuth2CodeGrantFlow flow = OAuth2ClientSupport.authorizationCodeGrantFlowBuilder(clientId, HUB_AUTH_URL, HUB_TOKEN_URL)
                .scope(HUB_ID)
                .redirectUri(AUTH_TOKEN_REDIRECT)
                .build();
        String authUri = flow.start();

        request.getSession().setAttribute("FLOW", flow);

        return Response.seeOther(URI.create(authUri))
                .cookie(new NewCookie("JSESSIONID", request.getSession().getId()))
                .build();
    }

    @GET
    @Path("/token")
    public Response tokenResponse(@Context HttpServletRequest request, @QueryParam("code") String code,
                                  @QueryParam("state") String state) {
        OAuth2CodeGrantFlow flow = (OAuth2CodeGrantFlow) request.getSession().getAttribute("FLOW");
        TokenResult token = null;
        if (flow != null) {
            token = flow.finish(code, state);
        }
        return Response.ok("Token: " + token.getAccessToken()).build();
    }

}
