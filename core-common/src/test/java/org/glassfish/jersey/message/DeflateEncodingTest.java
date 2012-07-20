/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
import java.util.HashMap;
import java.util.Map;
import java.util.zip.Deflater;
import java.util.zip.DeflaterOutputStream;
import java.util.zip.Inflater;
import java.util.zip.InflaterInputStream;

import javax.inject.Provider;

import org.glassfish.jersey.Config;

import org.junit.Test;

import com.google.common.collect.Maps;

/**
 * @author Martin Matula (martin.matula at oracle.com)
 */
public class DeflateEncodingTest extends AbstractEncodingTest {
    private static class DummyConfig implements Config, Provider<Config> {
        private final HashMap<String, Object> properties;

        public DummyConfig(boolean noZLib) {
            properties = Maps.newHashMap();
            properties.put(MessageProperties.DEFLATE_WITHOUT_ZLIB, noZLib);
        }

        @Override
        public Object getProperty(String name) {
            return properties.get(name);
        }

        @Override
        public boolean isProperty(String name) {
            return (Boolean) properties.get(name);
        }

        @Override
        public Map<String, Object> getProperties() {
            return properties;
        }

        @Override
        public Config get() {
            return this;
        }
    }

    @Test
    public void testEncodeZLib() throws IOException {
        test(new TestSpec() {
            @Override
            public OutputStream getEncoded(OutputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfig(false)).encode("deflate", stream);
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
                return new DeflateEncoder(new DummyConfig(true)).encode("deflate", stream);
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
                return new DeflateEncoder(new DummyConfig(false)).decode("deflate", stream);
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
                return new DeflateEncoder(new DummyConfig(false)).decode("deflate", stream);
            }
        });
    }

    @Test
    public void testEncodeDecodeZLib() throws IOException {
        test(new TestSpec() {
            @Override
            public OutputStream getEncoded(OutputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfig(false)).encode("deflate", stream);
            }

            @Override
            public InputStream getDecoded(InputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfig(false)).decode("deflate", stream);
            }
        });
    }

    @Test
    public void testEncodeDecodeNoZLib() throws IOException {
        test(new TestSpec() {
            @Override
            public OutputStream getEncoded(OutputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfig(true)).encode("deflate", stream);
            }

            @Override
            public InputStream getDecoded(InputStream stream) throws IOException {
                return new DeflateEncoder(new DummyConfig(false)).decode("deflate", stream);
            }
        });
    }
}
