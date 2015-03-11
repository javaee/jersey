/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2015 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * http://glassfish.java.net/public/CDDL+GPL_1_1.html
 * or packager/legal/LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at packager/legal/LICENSE.txt.
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

/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */
package org.glassfish.jersey.server.wadl.config;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;
import java.util.List;
import java.util.Properties;

import javax.ws.rs.core.MediaType;

import org.glassfish.jersey.server.ServerLocatorFactory;
import org.glassfish.jersey.server.model.Parameter;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.wadl.WadlGenerator;
import org.glassfish.jersey.server.wadl.internal.ApplicationDescription;

import org.glassfish.hk2.api.ServiceLocator;

import org.junit.Assert;
import org.junit.Test;

import com.sun.research.ws.wadl.Application;
import com.sun.research.ws.wadl.Method;
import com.sun.research.ws.wadl.Param;
import com.sun.research.ws.wadl.Representation;
import com.sun.research.ws.wadl.Request;
import com.sun.research.ws.wadl.Resource;
import com.sun.research.ws.wadl.Resources;
import com.sun.research.ws.wadl.Response;

/**
 * Test the {@link WadlGeneratorLoader}.
 *
 * @author Miroslav Fuksa
 * @author Martin Grotzke (martin.grotzke at freiheit.com)
 */
public class WadlGeneratorLoaderTest {

    @Test
    public void testLoadFileFromClasspathRelative() throws Exception {
        final ServiceLocator serviceLocator = ServerLocatorFactory.createLocator();
        final Properties props = new Properties();
        props.put("testFile", "classpath:testfile.xml");
        final WadlGeneratorDescription description = new WadlGeneratorDescription(MyWadlGenerator2.class, props);

        final WadlGenerator wadlGenerator = WadlGeneratorLoader.loadWadlGeneratorDescriptions(serviceLocator, description);
        Assert.assertEquals(MyWadlGenerator2.class, wadlGenerator.getClass());

        final URL resource = getClass().getResource("testfile.xml");
        Assert.assertEquals(new File(resource.toURI()).getAbsolutePath(), ((MyWadlGenerator2) wadlGenerator).getTestFile()
                .getAbsolutePath());

    }

    @Test
    public void testLoadFileFromClasspathAbsolute() throws Exception {
        final ServiceLocator serviceLocator = ServerLocatorFactory.createLocator();
        final Properties props = new Properties();
        final String path = "classpath:/" + getClass().getPackage().getName().replaceAll("\\.", "/") + "/testfile.xml";
        props.put("testFile", path);
        final WadlGeneratorDescription description = new WadlGeneratorDescription(MyWadlGenerator2.class, props);

        final WadlGenerator wadlGenerator = WadlGeneratorLoader.loadWadlGeneratorDescriptions(serviceLocator, description);
        Assert.assertEquals(MyWadlGenerator2.class, wadlGenerator.getClass());

        final URL resource = getClass().getResource("testfile.xml");
        Assert.assertEquals(new File(resource.toURI()).getAbsolutePath(), ((MyWadlGenerator2) wadlGenerator).getTestFile()
                .getAbsolutePath());

    }

    @Test
    public void testLoadFileFromAbsolutePath() throws Exception {
        final ServiceLocator serviceLocator = ServerLocatorFactory.createLocator();
        final URL resource = getClass().getResource("testfile.xml");

        final Properties props = new Properties();
        final String path = new File(resource.toURI()).getAbsolutePath();
        props.put("testFile", path);
        final WadlGeneratorDescription description = new WadlGeneratorDescription(MyWadlGenerator2.class, props);

        final WadlGenerator wadlGenerator = WadlGeneratorLoader.loadWadlGeneratorDescriptions(serviceLocator, description);
        Assert.assertEquals(MyWadlGenerator2.class, wadlGenerator.getClass());

        Assert.assertEquals(new File(resource.toURI()).getAbsolutePath(), ((MyWadlGenerator2) wadlGenerator).getTestFile()
                .getAbsolutePath());
    }

    @Test
    public void testLoadStream() throws Exception {
        final ServiceLocator serviceLocator = ServerLocatorFactory.createLocator();
        final Properties props = new Properties();
        final String path = getClass().getPackage().getName().replaceAll("\\.", "/") + "/testfile.xml";
        props.put("testStream", path);
        final WadlGeneratorDescription description = new WadlGeneratorDescription(MyWadlGenerator2.class, props);

        final WadlGenerator wadlGenerator = WadlGeneratorLoader.loadWadlGeneratorDescriptions(serviceLocator, description);
        Assert.assertEquals(MyWadlGenerator2.class, wadlGenerator.getClass());

        final URL resource = getClass().getResource("testfile.xml");
        Assert.assertEquals(new File(resource.toURI()).length(), ((MyWadlGenerator2) wadlGenerator).getTestStreamContent()
                .length());

    }

    public static class MyWadlGenerator2 implements WadlGenerator {

        private File _testFile;
        private InputStream _testStream;
        private File _testStreamContent;
        private WadlGenerator _delegate;

        /**
         * @param testFile the testFile to set
         */
        public void setTestFile(File testFile) {
            _testFile = testFile;
        }

        public void setTestStream(InputStream testStream) {
            _testStream = testStream;
        }

        public File getTestFile() {
            return _testFile;
        }

        public File getTestStreamContent() {
            /*
            try {
                System.out.println( "listing file " + _testFileContent.getName() );
                BufferedReader in = new BufferedReader( new FileReader( _testFileContent ) );
                String line = null;
                while ( (line = in.readLine()) != null ) {
                    System.out.println( line );
                }
            } catch ( Exception e ) {
                e.printStackTrace();
            }
            */
            return _testStreamContent;
        }

        public void init() throws IOException {
            if (_testStream != null) {
                _testStreamContent = File.createTempFile("testfile-" + getClass().getSimpleName(), null);
                OutputStream to = null;
                try {
                    to = new FileOutputStream(_testStreamContent);
                    byte[] buffer = new byte[4096];
                    int bytes_read;
                    while ((bytes_read = _testStream.read(buffer)) != -1) {
                        to.write(buffer, 0, bytes_read);
                    }
                } finally {
                    // Always close the streams, even if exceptions were thrown
                    if (to != null) {
                        try {
                            to.close();
                        } catch (IOException e) {
                        }
                    }
                }
            }
        }

        public void setWadlGeneratorDelegate(WadlGenerator delegate) {
            _delegate = delegate;
        }

        /**
         * @return the delegate
         */
        public WadlGenerator getDelegate() {
            return _delegate;
        }

        public Application createApplication() {
            return null;
        }

        public Method createMethod(org.glassfish.jersey.server.model.Resource r, ResourceMethod m) {
            return null;
        }

        public Request createRequest(org.glassfish.jersey.server.model.Resource r,
                                     ResourceMethod m) {
            return null;
        }

        public Param createParam(org.glassfish.jersey.server.model.Resource r,
                                 ResourceMethod m, Parameter p) {
            return null;
        }

        public Representation createRequestRepresentation(
                org.glassfish.jersey.server.model.Resource r, ResourceMethod m,
                MediaType mediaType) {
            return null;
        }

        public Resource createResource(org.glassfish.jersey.server.model.Resource r, String path) {
            return null;
        }

        public Resources createResources() {
            return null;
        }

        public List<Response> createResponses(org.glassfish.jersey.server.model.Resource r,
                                              ResourceMethod m) {
            return null;
        }

        public String getRequiredJaxbContextPath() {
            return null;
        }

        @Override
        public ExternalGrammarDefinition createExternalGrammar() {
            return _delegate.createExternalGrammar();
        }

        @Override
        public void attachTypes(ApplicationDescription egd) {
            _delegate.attachTypes(egd);
        }
    }
}
