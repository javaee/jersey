package org.glassfish.jersey.tests.e2e.server;

import java.util.Formatter;

import javax.ws.rs.Consumes;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.client.Configuration;
import javax.ws.rs.client.Entity;

import javax.xml.bind.annotation.XmlRootElement;

import org.glassfish.jersey.media.json.JsonJacksonFeature;
import org.glassfish.jersey.media.json.JsonJaxbModule;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;

import org.junit.Ignore;
import org.junit.Test;
import static org.junit.Assert.assertEquals;

/**
 * End to end test class for testing {@code ResourceConfig} features.
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class ResourceConfigTest extends JerseyTest {

    @XmlRootElement
    public static class Property {

        private String name;
        private String value;

        public Property() {
        }

        public Property(String name, String value) {
            this.name = name;
            this.value = value;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public String getValue() {
            return value;
        }

        public void setValue(String value) {
            this.value = value;
        }

        @Override
        public boolean equals(Object obj) {
            if (!(obj instanceof Property)) {
                return false;
            }
            final Property other = (Property) obj;
            if (this.name != other.name && (this.name == null || !this.name.equals(other.name))) {
                return false;
            }
            if (this.value != other.value && (this.value == null || !this.value.equals(other.value))) {
                return false;
            }
            return true;
        }

        @Override
        public int hashCode() {
            int hash = 7;
            hash = hash * 47 + (name == null ? 0 : name.hashCode());
            hash = hash * 47 + (value == null ? 0 : value.hashCode());
            return hash;
        }

        @Override
        public String toString() {
            return new Formatter().format("Property(name=%s,value=%s)", name, value).toString();
        }

    }

    @Path("/")
    @Produces("application/json")
    @Consumes("application/json")
    public static class Resource {

        @POST
        public Property post(final Property property) {
            return property;
        }

    }

    /**
     * Application similar to the one that needs to register a custom {@code Module} to run properly and is supposed to be present
     * in WAR files.
     */
    public static class Jersey1094 extends ResourceConfig {

        public Jersey1094() {
            addClasses(Resource.class);
            addModules(new JsonJaxbModule());
        }

    }

    @Override
    protected ResourceConfig configure() {
        // Simulate the creation of the ResourceConfig as if it was created during servlet initialization
        return ResourceConfig.forApplicationClass(Jersey1094.class);
    }

    @Override
    protected void configureClient(Configuration c) {
        c.register(new JsonJacksonFeature());
    }

    /**
     * Tests whether the {@code ApplicationHandler} is able to register and use custom modules provided by an extension of
     * {@code ResourceConfig} if only a class reference of this extension is passed in the {@code ResourceConfig} to the
     * {@code ApplicationHandler}.
     * <p/>
     * Test is trying to simulate the behaviour of Jersey as if the application was deployed into a servlet container
     * with a servlet defined in {@code web.xml} file using {@code init-param} {@code javax.ws.rs.Application}.
     */
    @Test
    public void testJersey1094() throws Exception {
        Property testInstance = new Property("myProp", "myVal");

        final Property returnedValue = target()
                .path("/")
                .request("application/json")
                .post(Entity.entity(testInstance, "application/json"), Property.class);

        assertEquals(testInstance, returnedValue);
    }

    @Test
    @Ignore
    public void testJersey1094ReloadResourceConfig() throws Exception {
        // TODO test reloading resource config in the container (once it is supported)
    }

}
