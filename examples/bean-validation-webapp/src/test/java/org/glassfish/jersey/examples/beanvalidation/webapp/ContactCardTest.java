/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.examples.beanvalidation.webapp;

import java.net.URI;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.ws.rs.client.Entity;
import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Application;
import javax.ws.rs.core.GenericType;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;
import javax.ws.rs.core.UriBuilder;

import org.glassfish.jersey.client.ClientConfig;
import org.glassfish.jersey.examples.beanvalidation.webapp.domain.ContactCard;
import org.glassfish.jersey.moxy.json.MoxyJsonConfig;
import org.glassfish.jersey.moxy.json.MoxyJsonFeature;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.validation.ValidationError;
import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.TestProperties;
import org.glassfish.jersey.test.external.ExternalTestContainerFactory;

import org.eclipse.persistence.jaxb.BeanValidationMode;
import org.eclipse.persistence.jaxb.MarshallerProperties;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

/**
 * @author Michal Gajdos
 */
public class ContactCardTest extends JerseyTest {

    private static final ContactCard CARD_1;
    private static final ContactCard CARD_2;

    static {
        CARD_1 = new ContactCard();
        CARD_1.setFullName("Jersey Foo");
        CARD_1.setPhone("1337");

        CARD_2 = new ContactCard();
        CARD_2.setFullName("Jersey Bar");
        CARD_2.setEmail("jersey@bar.com");
    }

    @Override
    protected Application configure() {
        enable(TestProperties.LOG_TRAFFIC);
        enable(TestProperties.DUMP_ENTITY);

        return new MyApplication().property(ServerProperties.BV_SEND_ERROR_IN_RESPONSE, true);
    }

    @Override
    protected void configureClient(final ClientConfig config) {
        super.configureClient(config);

        config.register(MoxyJsonFeature.class);
        // Turn off BV otherwise the entities on client would be validated as well.
        config.register(new MoxyJsonConfig()
                .property(MarshallerProperties.BEAN_VALIDATION_MODE, BeanValidationMode.NONE)
                .resolver());
    }

    @Override
    protected URI getBaseUri() {
        final UriBuilder baseUriBuilder = UriBuilder.fromUri(super.getBaseUri()).path("bean-validation-webapp");
        final boolean externalFactoryInUse = getTestContainerFactory() instanceof ExternalTestContainerFactory;
        return externalFactoryInUse ? baseUriBuilder.path("api").build() : baseUriBuilder.build();
    }

    @Test
    public void testAddContact() throws Exception {
        final WebTarget target = target()
                .path("contact");
        final Response response = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(CARD_1, MediaType.APPLICATION_JSON_TYPE));

        final ContactCard contactCard = response.readEntity(ContactCard.class);

        assertEquals(200, response.getStatus());
        assertNotNull(contactCard.getId());

        final Response invalidResponse = target.request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(CARD_1, MediaType.APPLICATION_JSON_TYPE));
        assertEquals(500, invalidResponse.getStatus());
        assertTrue(getValidationMessageTemplates(invalidResponse).contains("{contact.already.exist}"));

        assertEquals(200, target.path("" + contactCard.getId()).request(MediaType.APPLICATION_JSON_TYPE).delete().getStatus());
    }

    @Test
    public void testContactDoesNotExist() throws Exception {
        final WebTarget target = target()
                .path("contact");

        // GET
        Response response = target.path("1").request(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(500, response.getStatus());

        Set<String> violationsMessageTemplates = getValidationMessageTemplates(response);
        assertEquals(1, violationsMessageTemplates.size());
        assertTrue(violationsMessageTemplates.contains("{contact.does.not.exist}"));

        // DELETE
        response = target.path("1").request(MediaType.APPLICATION_JSON_TYPE).delete();

        assertEquals(500, response.getStatus());

        violationsMessageTemplates = getValidationMessageTemplates(response);
        assertEquals(1, violationsMessageTemplates.size());
        assertTrue(violationsMessageTemplates.contains("{contact.does.not.exist}"));
    }

    @Test
    public void testContactWrongId() throws Exception {
        final WebTarget target = target()
                .path("contact");

        // GET
        Response response = target.path("-1").request(MediaType.APPLICATION_JSON_TYPE).get();

        assertEquals(400, response.getStatus());

        Set<String> violationsMessageTemplates = getValidationMessageTemplates(response);
        assertEquals(1, violationsMessageTemplates.size());
        assertTrue(violationsMessageTemplates.contains("{contact.wrong.id}"));

        // DELETE
        response = target.path("-2").request(MediaType.APPLICATION_JSON_TYPE).delete();

        assertEquals(400, response.getStatus());

        violationsMessageTemplates = getValidationMessageTemplates(response);
        assertEquals(1, violationsMessageTemplates.size());
        assertTrue(violationsMessageTemplates.contains("{contact.wrong.id}"));
    }

    private List<ValidationError> getValidationErrorList(final Response response) {
        return response.readEntity(new GenericType<List<ValidationError>>() {});
    }

    private Set<String> getValidationMessageTemplates(final Response response) {
        return getValidationMessageTemplates(getValidationErrorList(response));
    }

    private Set<String> getValidationMessageTemplates(final List<ValidationError> errors) {
        final Set<String> templates = new HashSet<>();
        for (final ValidationError error : errors) {
            templates.add(error.getMessageTemplate());
        }
        return templates;
    }

    @Test
    public void testAddInvalidContact() throws Exception {
        final ContactCard entity = new ContactCard();
        entity.setPhone("Crrrn");

        final Response response = target()
                .path("contact")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .post(Entity.entity(entity, MediaType.APPLICATION_JSON_TYPE));

        assertEquals(400, response.getStatus());

        final List<ValidationError> validationErrorList = getValidationErrorList(response);
        for (final ValidationError validationError : validationErrorList) {
            assertTrue(validationError.getPath().contains("ContactCardResource.addContact.contact."));
        }

        final Set<String> messageTemplates = getValidationMessageTemplates(validationErrorList);
        assertEquals(2, messageTemplates.size());
        assertTrue(messageTemplates.contains("{contact.wrong.name}"));
        assertTrue(messageTemplates.contains("{contact.wrong.phone}"));
    }

    @Test
    public void testSearchByUnknown() throws Exception {
        final Response response = target()
                .path("contact")
                .path("search/unknown")
                .queryParam("q", "er")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertEquals(400, response.getStatus());

        final Set<String> messageTemplates = getValidationMessageTemplates(response);
        assertEquals(1, messageTemplates.size());
        assertTrue(
                messageTemplates.contains("{org.glassfish.jersey.examples.beanvalidation.webapp.constraint.SearchType.message}"));
    }

    @Test
    public void testSearchByEmailEmpty() throws Exception {
        final Response response = target()
                .path("contact")
                .path("search/email")
                .queryParam("q", "er")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertEquals(200, response.getStatus());

        final List<ContactCard> result = response.readEntity(new GenericType<List<ContactCard>>() {});
        assertEquals(0, result.size());
    }

    @Test
    public void testSearchByPhoneInvalid() throws Exception {
        final Response response = target()
                .path("contact")
                .path("search/phone")
                .queryParam("q", (String) null)
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        assertEquals(400, response.getStatus());

        final Set<String> messageTemplates = getValidationMessageTemplates(response);
        assertEquals(1, messageTemplates.size());
        assertTrue(messageTemplates.contains("{search.string.empty}"));
    }

    @Test
    public void testSearchByName() throws Exception {
        final WebTarget target = target().path("contact");
        target.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(CARD_1, MediaType.APPLICATION_JSON_TYPE));
        target.request(MediaType.APPLICATION_JSON_TYPE).post(Entity.entity(CARD_2, MediaType.APPLICATION_JSON_TYPE));

        Response response = target.path("search/name")
                .queryParam("q", "er")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        List<ContactCard> contactCards = response.readEntity(new GenericType<List<ContactCard>>() {});

        assertEquals(200, response.getStatus());
        assertEquals(2, contactCards.size());

        for (final ContactCard contactCard : contactCards) {
            assertTrue(contactCard.getFullName().contains("er"));
        }

        response = target.path("search/name")
                .queryParam("q", "Foo")
                .request(MediaType.APPLICATION_JSON_TYPE)
                .get();

        contactCards = response.readEntity(new GenericType<List<ContactCard>>() {});

        assertEquals(200, response.getStatus());
        assertEquals(1, contactCards.size());
        assertTrue(contactCards.get(0).getFullName().contains("Foo"));

        assertEquals(200, target.request(MediaType.APPLICATION_JSON_TYPE).delete().getStatus());
    }
}
