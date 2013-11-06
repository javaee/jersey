package org.glassfish.jersey.server.internal.scanning;

import org.glassfish.jersey.server.internal.scanning.JarFileScanner;
import org.junit.Test;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import java.io.IOException;
import java.io.InputStream;
 
public class JarFileScannerTest {
    @Test
    public void testClassEnumeration() throws Exception {
        int entryCount = 0;
        InputStream embeddedJarResource = this.getClass().getClassLoader().getResourceAsStream("org/glassfish/jersey/server/internal/scanning/portlet-api-1.0.jar");
		assertNotNull("Could not find embedded portlet-api-1.0.jar", embeddedJarResource);
		
        try {
            JarFileScanner jarFileScanner = new JarFileScanner(embeddedJarResource, "javax/portlet", true);
            while (jarFileScanner.hasNext()) {
                String name = jarFileScanner.next();
 
                InputStream classStream = jarFileScanner.open();
                try {
                    entryCount++;
                } finally {
                    classStream.close();
                }
            }
 
            assertEquals("Failed to enumerate all contents of portlet-api-1.0.jar", 27, entryCount);
        } finally {
            embeddedJarResource.close();
        }
    }
}