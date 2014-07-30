package org.glassfish.jersey.media.multipart.internal;

import java.io.File;
import javax.ws.rs.core.Application;

import jersey.repackaged.com.google.common.io.Files;
import org.glassfish.jersey.media.multipart.MultiPartFeature;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.test.JerseyTest;
import org.junit.Test;

import static org.junit.Assert.assertEquals;

public class MultiPartReaderClientSideTest extends JerseyTest {

    private final File tempDirectory;

    public MultiPartReaderClientSideTest() {
        tempDirectory = Files.createTempDir();
        System.setProperty("java.io.tmpdir", tempDirectory.getAbsolutePath());
    }

    @Override
    protected Application configure() {
        return new ResourceConfig().
                registerClasses(MultiPartBeanProvider.class).
                register(new MultiPartFeature());
    }

    /**
     * Test for JERSEY-2515 - Jersey should not leave any temporary files after verifying that it is possible
     * to create files in java.io.tmpdir.
     */
    @Test
    public void shouldNotBeAnyTemporaryFiles() {
        assertEquals(0, tempDirectory.listFiles().length);
    }

}
