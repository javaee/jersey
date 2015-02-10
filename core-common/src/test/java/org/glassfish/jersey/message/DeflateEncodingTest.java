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

package org.glassfish.jersey.message;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.ws.rs.RuntimeType;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Feature;

import javax.inject.Provider;

import org.junit.Test;

import jersey.repackaged.com.google.common.collect.Maps;

/**
 * @author Martin Matula
 */
public class DeflateEncodingTest extends AbstractEncodingTest {

    private static class DummyConfiguration implements Configuration, Provider<Configuration> {

        private final HashMap<String, Object> properties;

        public DummyConfiguration(boolean noZLib) {
            properties = Maps.newHashMap();
            properties.put(MessageProperties.DEFLATE_WITHOUT_ZLIB, noZLib);
        }

        @Override
        public RuntimeType getRuntimeType() {
            return null;
        }

        @Override
        public Object getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public Collection<String> getPropertyNames() {
            return properties.keySet();
        }

        @Override
        public boolean isEnabled(Feature feature) {
            return false;
        }

        @Override
        public boolean isEnabled(Class<? extends Feature> featureClass) {
            return false;
        }

        @Override
        public boolean isRegistered(Object provider) {
            return false;
        }

        @Override
        public boolean isRegistered(Class<?> providerClass) {
            return false;
        }
        @Override
        public Map<String, Object> getProperties() {
            return properties;
        }

        @Override
        public Configuration get() {
            return this;
        }

        @Override
        public Map<Class<?>, Integer> getContracts(Class<?> componentClass) {
            return Collections.emptyMap();
        }

        @Override
        public Set<Class<?>> getClasses() {
            return Collections.emptySet();
        }

        @Override
        public Set<Object> getInstances() {
            return Collections.emptySet();
        }
    }

    @Test
    public void testEncodeZLib() throws IOException {
        test(new TestSpec() {
            @Override
            public OutputStream getEncoded(OutputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfiguration(false)).encode("deflate", stream);
            }

            @Override
            public InputStream getDecoded(InputStream stream) {
                return new InflaterInputStream(stream);
            }
        });
    }

    @Test
    public void testEncodeNoZLib() throws IOException {
        test(new TestSpec() {
            @Override
            public OutputStream getEncoded(OutputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfiguration(true)).encode("deflate", stream);
            }

            @Override
            public InputStream getDecoded(InputStream stream) {
                return new InflaterInputStream(stream, new Inflater(true));
            }
        });
    }

    @Test
    public void testDecodeZLib() throws IOException {
        test(new TestSpec() {
            @Override
            public OutputStream getEncoded(OutputStream stream) throws IOException {
                return new DeflaterOutputStream(stream);
            }

            @Override
            public InputStream getDecoded(InputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfiguration(false)).decode("deflate", stream);
            }
        });
    }

    @Test
    public void testDecodeNoZLib() throws IOException {
        test(new TestSpec() {
            @Override
            public OutputStream getEncoded(OutputStream stream) throws IOException {
                return new DeflaterOutputStream(stream, new Deflater(Deflater.DEFAULT_COMPRESSION, true));
            }

            @Override
            public InputStream getDecoded(InputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfiguration(false)).decode("deflate", stream);
            }
        });
    }

    @Test
    public void testEncodeDecodeZLib() throws IOException {
        test(new TestSpec() {
            @Override
            public OutputStream getEncoded(OutputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfiguration(false)).encode("deflate", stream);
            }

            @Override
            public InputStream getDecoded(InputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfiguration(false)).decode("deflate", stream);
            }
        });
    }

    @Test
    public void testEncodeDecodeNoZLib() throws IOException {
        test(new TestSpec() {
            @Override
            public OutputStream getEncoded(OutputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfiguration(true)).encode("deflate", stream);
            }

            @Override
            public InputStream getDecoded(InputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfiguration(false)).decode("deflate", stream);
            }
        });
    }
}
