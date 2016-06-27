package org.glassfish.jersey.client.oauth2.workflows.steps;

/**
 * Interface to be implemented by {@code OAuth2Workflow} steps.
 * <p>
 *     Typically a workflow is executed as sequence of steps as explained in
 *     <a href="https://tools.ietf.org/html/rfc6749#section-4">Specification</a>
 *     The {@code OAuth2WorkflowStep} implementations must implement {@link #execute()}
 *     method that -
 *     <list>
 *         <li>
 *             Executes logic required for current step for e.g. preparing authorization request
 *             or requesting access token
 *         </li>
 *         <li>
 *             Moves forward the workflow by setting next step by calling
 *             {@link org.glassfish.jersey.client.oauth2.workflows.OAuth2Workflow#setState(OAuth2WorkflowStep)}
 *         </li>
 *     </list>
 * </p>
 *
 * @author Deepak Pol on 3/11/16.
 */
public interface OAuth2WorkflowStep {

    /**
     * API to be implemented to execute the step
     */
    void execute();
}
