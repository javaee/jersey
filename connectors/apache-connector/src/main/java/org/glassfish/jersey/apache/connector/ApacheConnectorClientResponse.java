package org.glassfish.jersey.apache.connector;

import java.io.IOException;

import javax.ws.rs.core.Response;
import javax.ws.rs.core.Response.StatusType;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.glassfish.jersey.client.ClientRequest;
import org.glassfish.jersey.client.ClientResponse;

/**
 * 
 * @author marcus
 * 
 */
public class ApacheConnectorClientResponse extends ClientResponse {

	private CloseableHttpResponse closeableHttpResponse = null;

	public ApacheConnectorClientResponse(ClientRequest requestContext,
			Response response) {
		super(requestContext, response);
	}

	public ApacheConnectorClientResponse(StatusType status,
			ClientRequest requestContext,
			CloseableHttpResponse closeableHttpResponse) {
		super(status, requestContext);
		this.closeableHttpResponse = closeableHttpResponse;
	}

	public ApacheConnectorClientResponse(StatusType status,
			ClientRequest requestContext) {
		super(status, requestContext);
	}

	@Override
	public void close() {
		if (closeableHttpResponse != null) {
			try {
				closeableHttpResponse.close();
			} catch (IOException e) {
				throw new RuntimeException(e);
			}
		}
		super.close();
	}
	
}
