package org.glassfish.jersey.client.oauth2.workflows.steps;

import org.glassfish.jersey.client.oauth2.workflows.OAuth2Workflow;
import org.glassfish.jersey.client.oauth2.TokenResult;
import org.glassfish.jersey.client.oauth2.internal.LocalizationMessages;

import javax.ws.rs.ProcessingException;
import javax.ws.rs.client.Entity;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import java.util.Map;

/**
 * {@code OAuth2WorkflowStep} to request access_token
 * @author Deepak Pol on 3/11/16.
 */
public class RequestingAccessToken implements OAuth2WorkflowStep {


    private OAuth2Workflow workflowContext;

    public RequestingAccessToken(OAuth2Workflow workflowContext){
        this.workflowContext = workflowContext;
    }

    /**
     * Uses access token API to request for a new token.
     * The actual {@code OAuth2Workflow} implementation should ensure that
     * {@code OAuth2Workflow.getAccessTokenProperties} returns valid and sufficient
     * information to complete the request for the flow.
     * All the parameters are passed as Form submission, OAuth2 providers need to
     * ensure that the parameters through Form submission are accepted
     */
    @Override
    public void execute() {

        Map<String, String> accessTokenProperties = workflowContext.getAccessTokenProperties();
        final Form form = new Form();
        for (final Map.Entry<String, String> entry : accessTokenProperties.entrySet()) {
            form.param(entry.getKey(), entry.getValue());
        }

        final Response response =
                workflowContext.getClient()
                .target(workflowContext.getAccessTokenUri())
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(form, MediaType.APPLICATION_FORM_URLENCODED_TYPE));

        if (response.getStatus() != 200) {
            throw new ProcessingException(LocalizationMessages.ERROR_FLOW_REQUEST_ACCESS_TOKEN(response.getStatus()));
        }
        TokenResult tokenResult = response.readEntity(TokenResult.class);
        workflowContext.setState(new AccessTokenAvailable(workflowContext, tokenResult));
    }
}
