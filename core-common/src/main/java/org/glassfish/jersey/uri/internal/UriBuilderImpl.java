/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.uri.internal;

import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.Method;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.uri.UriComponent;
import org.glassfish.jersey.uri.UriTemplate;

/**
 * An implementation of {@link UriBuilder}.
 *
 * @author Paul Sandoz
 * @author Martin Matula (martin.matula at oracle.com)
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class UriBuilderImpl extends UriBuilder {

    // All fields should be in the percent-encoded form
    private String scheme;
    private String ssp;
    private String authority;
    private String userInfo;
    private String host;
    private String port;
    private final StringBuilder path;
    private MultivaluedMap<String, String> matrixParams;
    private final StringBuilder query;
    private MultivaluedMap<String, String> queryParams;
    private String fragment;

    public UriBuilderImpl() {
        path = new StringBuilder();
        query = new StringBuilder();
    }

    private UriBuilderImpl(UriBuilderImpl that) {
        this.scheme = that.scheme;
        this.ssp = that.ssp;
        this.authority = that.authority;
        this.userInfo = that.userInfo;
        this.host = that.host;
        this.port = that.port;
        this.path = new StringBuilder(that.path);
        this.matrixParams = that.matrixParams == null ? null : new MultivaluedStringMap(that.matrixParams);
        this.query = new StringBuilder(that.query);
        this.queryParams = that.queryParams == null ? null : new MultivaluedStringMap(that.queryParams);
        this.fragment = that.fragment;
    }

    @Override
    public UriBuilder clone() {
        return new UriBuilderImpl(this);
    }

    @Override
    public UriBuilder uri(URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException("URI parameter is null");
        }

        if (uri.getRawFragment() != null) {
            fragment = uri.getRawFragment();
        }

        if (uri.isOpaque()) {
            scheme = uri.getScheme();
            ssp = uri.getRawSchemeSpecificPart();
            return this;
        }

        if (uri.getScheme() == null) {
            if (ssp != null) {
                if (uri.getRawSchemeSpecificPart() != null) {
                    ssp = uri.getRawSchemeSpecificPart();
                    return this;
                }
            }
        } else {
            scheme = uri.getScheme();
        }

        ssp = null;
        if (uri.getRawAuthority() != null) {
            if (uri.getRawUserInfo() == null && uri.getHost() == null && uri.getPort() == -1) {
                authority = uri.getRawAuthority();
                userInfo = null;
                host = null;
                port = null;
            } else {
                authority = null;
                if (uri.getRawUserInfo() != null) {
                    userInfo = uri.getRawUserInfo();
                }
                if (uri.getHost() != null) {
                    host = uri.getHost();
                }
                if (uri.getPort() != -1) {
                    port = String.valueOf(uri.getPort());
                }
            }
        }

        if (uri.getRawPath() != null && uri.getRawPath().length() > 0) {
            path.setLength(0);
            path.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null && uri.getRawQuery().length() > 0) {
            query.setLength(0);
            query.append(uri.getRawQuery());

        }

        return this;
    }

    // TODO: add override once the method is added to UriBuilder
    public UriBuilder uri(String uriTemplate) {
        UriParser parser = new UriParser(uriTemplate);
        parser.parse();

        if (parser.getFragment() != null) {
            this.fragment = parser.getFragment();
        }

        if (parser.isOpaque()) {
            this.ssp = parser.getSsp();
            this.scheme = parser.getScheme();
            return this;
        }

        if (parser.getScheme() == null && this.ssp != null && parser.getSsp() != null) {
            // relative uri with ssp
            this.ssp = parser.getSsp();
        } else {
            this.scheme = parser.getScheme();
        }

        if (parser.getAuthority() != null) {
            if (parser.getAuthority() == null && parser.getHost() == null && parser.getPort() == null) {
                this.authority = parser.getAuthority();
                this.userInfo = null;
                this.host = null;
                this.port = null;
            } else {
                this.authority = null;
                if (parser.getUserInfo() != null) {
                    this.userInfo = parser.getUserInfo();
                }
                if (parser.getHost() != null) {
                    this.host = parser.getHost();
                }
                if (parser.getPort() != null) {
                    this.port = parser.getPort();
                }
            }

        }

        if (parser.getPath() != null) {
            this.path.setLength(0);
            this.path.append(parser.getPath());
        }
        if (parser.getQuery() != null) {
            this.query.setLength(0);
            this.query.append(parser.getQuery());
        }

        return this;
    }

    @Override
    public UriBuilder scheme(String scheme) {
        if (scheme != null) {
            this.scheme = scheme;
            UriComponent.validate(scheme, UriComponent.Type.SCHEME, true);
        } else {
            this.scheme = null;
        }
        return this;
    }

    @Override
    public UriBuilder schemeSpecificPart(String ssp) {
        if (ssp == null) {
            throw new IllegalArgumentException("Scheme specific part parameter is null");
        }

        // TODO encode or validate scheme specific part
        // This will not work for template variables present in the ssp
        StringBuilder sb = new StringBuilder();
        if (scheme != null) {
            sb.append(scheme).append(':');
        }
        if (ssp != null) {
            sb.append(ssp);
        }
        if (fragment != null && fragment.length() > 0) {
            sb.append('#').append(fragment);
        }
        URI uri = createURI(sb.toString());

        if (uri.getRawSchemeSpecificPart() != null && uri.getRawPath() == null) {
            this.ssp = uri.getRawSchemeSpecificPart();
        } else {
            this.ssp = null;

            if (uri.getRawAuthority() != null) {
                if (uri.getRawUserInfo() == null && uri.getHost() == null && uri.getPort() == -1) {
                    authority = uri.getRawAuthority();
                    userInfo = null;
                    host = null;
                    port = null;
                } else {
                    authority = null;
                    userInfo = uri.getRawUserInfo();
                    host = uri.getHost();
                    port = uri.getPort() == -1 ? null : String.valueOf(uri.getPort());
                }
            }

            path.setLength(0);
            path.append(replaceNull(uri.getRawPath()));

            query.setLength(0);
            query.append(replaceNull(uri.getRawQuery()));
        }
        return this;
    }

    @Override
    public UriBuilder userInfo(String ui) {
        checkSsp();
        this.userInfo = (ui != null)
                ? encode(ui, UriComponent.Type.USER_INFO) : null;
        return this;
    }

    @Override
    public UriBuilder host(String host) {
        checkSsp();
        if (host != null) {
            if (host.length() == 0) // null is used to reset host setting
            {
                throw new IllegalArgumentException("Invalid host name");
            }
            this.host = encode(host, UriComponent.Type.HOST);
        } else {
            this.host = null;
        }
        return this;
    }

    @Override
    public UriBuilder port(int port) {
        checkSsp();
        if (port < -1) // -1 is used to reset port setting and since URI allows
        // as port any positive integer, so do we.
        {
            throw new IllegalArgumentException("Invalid port value");
        }
        this.port = port == -1 ? null : String.valueOf(port);
        return this;
    }

    @Override
    public UriBuilder replacePath(String path) {
        checkSsp();
        this.path.setLength(0);
        if (path != null) {
            appendPath(path);
        }
        return this;
    }

    @Override
    public UriBuilder path(String path) {
        checkSsp();
        appendPath(path);
        return this;
    }

    @Override
    public UriBuilder path(Class resource) {
        checkSsp();
        if (resource == null) {
            throw new IllegalArgumentException("Resource parameter is null");
        }

        Class<?> c = resource;
        Path p = c.getAnnotation(Path.class);
        if (p == null) {
            throw new IllegalArgumentException("The class, " + resource + " is not annotated with @Path");
        }
        appendPath(p);
        return this;
    }

    @Override
    public UriBuilder path(Class resource, String methodName) {
        checkSsp();
        if (resource == null) {
            throw new IllegalArgumentException("Resource parameter is null");
        }
        if (methodName == null) {
            throw new IllegalArgumentException("MethodName parameter is null");
        }

        Method[] methods = resource.getMethods();
        Method found = null;
        for (Method m : methods) {
            if (methodName.equals(m.getName())) {
                if (found == null) {
                    found = m;
                } else {
                    throw new IllegalArgumentException();
                }
            }
        }

        if (found == null) {
            throw new IllegalArgumentException("The method named, " + methodName
                    + ", is not specified by " + resource);
        }

        appendPath(getPath(found));

        return this;
    }

    @Override
    public UriBuilder path(Method method) {
        checkSsp();
        if (method == null) {
            throw new IllegalArgumentException("Method is null");
        }
        appendPath(getPath(method));
        return this;
    }

    private Path getPath(AnnotatedElement ae) {
        Path p = ae.getAnnotation(Path.class);
        if (p == null) {
            throw new IllegalArgumentException("The annotated element, "
                    + ae + " is not annotated with @Path");
        }
        return p;
    }

    @Override
    public UriBuilder segment(String... segments) throws IllegalArgumentException {
        checkSsp();
        if (segments == null) {
            throw new IllegalArgumentException("Segments parameter is null");
        }

        for (String segment : segments) {
            appendPath(segment, true);
        }
        return this;
    }

    @Override
    public UriBuilder replaceMatrix(String matrix) {
        checkSsp();
        int i = path.lastIndexOf("/");
        if (i != -1) {
            i = 0;
        }
        i = path.indexOf(";", i);
        if (i != -1) {
            path.setLength(i + 1);
        } else {
            path.append(';');
        }

        if (matrix != null) {
            path.append(encode(matrix, UriComponent.Type.PATH));
        }
        return this;
    }

    @Override
    public UriBuilder matrixParam(String name, Object... values) {
        checkSsp();
        if (name == null) {
            throw new IllegalArgumentException("Name parameter is null");
        }
        if (values == null) {
            throw new IllegalArgumentException("Value parameter is null");
        }
        if (values.length == 0) {
            return this;
        }

        name = encode(name, UriComponent.Type.MATRIX_PARAM);
        if (matrixParams == null) {
            for (Object value : values) {
                path.append(';').append(name);

                if (value == null) {
                    throw new IllegalArgumentException("One or more of matrix value parameters are null");
                }

                final String stringValue = value.toString();
                if (stringValue.length() > 0) {
                    path.append('=').append(encode(stringValue, UriComponent.Type.MATRIX_PARAM));
                }
            }
        } else {
            for (Object value : values) {
                if (value == null) {
                    throw new IllegalArgumentException("One or more of matrix value parameters are null");
                }

                matrixParams.add(name, encode(value.toString(), UriComponent.Type.MATRIX_PARAM));
            }
        }
        return this;
    }

    @Override
    public UriBuilder replaceMatrixParam(String name, Object... values) {
        checkSsp();

        if (name == null) {
            throw new IllegalArgumentException("Name parameter is null");
        }

        if (matrixParams == null) {
            int i = path.lastIndexOf("/");
            if (i != -1) {
                i = 0;
            }
            matrixParams = UriComponent.decodeMatrix((i != -1) ? path.substring(i) : "", false);
            i = path.indexOf(";", i);
            if (i != -1) {
                path.setLength(i);
            }
        }

        name = encode(name, UriComponent.Type.MATRIX_PARAM);
        matrixParams.remove(name);
        if (values != null) {
            for (Object value : values) {
                if (value == null) {
                    throw new IllegalArgumentException("One or more of matrix value parameters are null");
                }

                matrixParams.add(name, encode(value.toString(), UriComponent.Type.MATRIX_PARAM));
            }
        }
        return this;
    }

    @Override
    public UriBuilder replaceQuery(String query) {
        checkSsp();
        this.query.setLength(0);
        if (query != null) {
            this.query.append(encode(query, UriComponent.Type.QUERY));
        }
        return this;
    }

    @Override
    public UriBuilder queryParam(String name, Object... values) {
        checkSsp();
        if (name == null) {
            throw new IllegalArgumentException("Name parameter is null");
        }
        if (values == null) {
            throw new IllegalArgumentException("Value parameter is null");
        }
        if (values.length == 0) {
            return this;
        }

        name = encode(name, UriComponent.Type.QUERY_PARAM);
        if (queryParams == null) {
            for (Object value : values) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(name);

                if (value == null) {
                    throw new IllegalArgumentException("One or more of query value parameters are null");
                }

                final String stringValue = value.toString();
                if (stringValue.length() > 0) {
                    query.append('=').append(encode(stringValue, UriComponent.Type.QUERY_PARAM));
                }
            }
        } else {
            for (Object value : values) {
                if (value == null) {
                    throw new IllegalArgumentException("One or more of query value parameters are null");
                }

                queryParams.add(name, encode(value.toString(), UriComponent.Type.QUERY_PARAM));
            }
        }
        return this;
    }

    @Override
    public UriBuilder replaceQueryParam(String name, Object... values) {
        checkSsp();

        if (queryParams == null) {
            queryParams = UriComponent.decodeQuery(query.toString(), false);
            query.setLength(0);
        }

        name = encode(name, UriComponent.Type.QUERY_PARAM);
        queryParams.remove(name);

        if (values == null) {
            return this;
        }

        for (Object value : values) {
            if (value == null) {
                throw new IllegalArgumentException("One or more of query value parameters are null");
            }

            queryParams.add(name, encode(value.toString(), UriComponent.Type.QUERY_PARAM));
        }
        return this;
    }

    @Override
    public UriBuilder fragment(String fragment) {
        this.fragment = (fragment != null)
                ? encode(fragment, UriComponent.Type.FRAGMENT)
                : null;
        return this;
    }

    private void checkSsp() {
        if (ssp != null) {
            throw new IllegalArgumentException("Schema specific part is opaque");
        }
    }

    private void appendPath(Path t) {
        if (t == null) {
            throw new IllegalArgumentException("Path is null");
        }

        appendPath(t.value());
    }

    private void appendPath(String path) {
        appendPath(path, false);
    }

    private void appendPath(String segments, boolean isSegment) {
        if (segments == null) {
            throw new IllegalArgumentException("Path segment is null");
        }
        if (segments.length() == 0) {
            return;
        }

        // Encode matrix parameters on current path segment
        encodeMatrix();

        segments = encode(segments,
                (isSegment) ? UriComponent.Type.PATH_SEGMENT : UriComponent.Type.PATH);

        final boolean pathEndsInSlash = path.length() > 0 && path.charAt(path.length() - 1) == '/';
        final boolean segmentStartsWithSlash = segments.charAt(0) == '/';

        if (path.length() > 0 && !pathEndsInSlash && !segmentStartsWithSlash) {
            path.append('/');
        } else if (pathEndsInSlash && segmentStartsWithSlash) {
            segments = segments.substring(1);
            if (segments.length() == 0) {
                return;
            }
        }

        path.append(segments);
    }

    private void encodeMatrix() {
        if (matrixParams == null || matrixParams.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<String>> e : matrixParams.entrySet()) {
            String name = e.getKey();

            for (String value : e.getValue()) {
                path.append(';').append(name);
                if (value.length() > 0) {
                    path.append('=').append(value);
                }
            }
        }
        matrixParams = null;
    }

    private void encodeQuery() {
        if (queryParams == null || queryParams.isEmpty()) {
            return;
        }

        for (Map.Entry<String, List<String>> e : queryParams.entrySet()) {
            String name = e.getKey();

            for (String value : e.getValue()) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(name);

                if (value.length() > 0) {
                    query.append('=').append(value);
                }
            }
        }
        queryParams = null;
    }

    private String encode(String s, UriComponent.Type type) {
        return UriComponent.contextualEncode(s, type, true);
    }

    @Override
    public URI buildFromMap(Map<String, ? extends Object> values) {
        return _buildFromMap(true, values);
    }

    @Override
    public URI buildFromEncodedMap(Map<String, ? extends Object> values) throws IllegalArgumentException, UriBuilderException {
        return _buildFromMap(false, values);
    }

    private URI _buildFromMap(boolean encode, Map<String, ? extends Object> values) {
        if (ssp != null) {
            throw new IllegalArgumentException("Schema specific part is opaque");
        }

        encodeMatrix();
        encodeQuery();

        String uri = UriTemplate.createURI(
                scheme, authority,
                userInfo, host, port,
                path.toString(), query.toString(), fragment, values, encode);
        return createURI(uri);
    }

    @Override
    public URI build(Object... values) {
        return _build(true, values);
    }

    @Override
    public URI buildFromEncoded(Object... values) {
        return _build(false, values);
    }

    private URI _build(boolean encode, Object... values) {
        if (values == null || values.length == 0) {
            return createURI(create());
        }

        if (ssp != null) {
            throw new IllegalArgumentException("Schema specific part is opaque");
        }

        encodeMatrix();
        encodeQuery();

        String uri = UriTemplate.createURI(
                scheme, authority,
                userInfo, host, port,
                path.toString(), query.toString(), fragment, values, encode);
        return createURI(uri);
    }

    private String create() {
        encodeMatrix();
        encodeQuery();

        StringBuilder sb = new StringBuilder();

        if (scheme != null) {
            sb.append(scheme).append(':');
        }

        if (ssp != null) {
            sb.append(ssp);
        } else {
            if (userInfo != null || host != null || port != null) {
                sb.append("//");

                if (userInfo != null && userInfo.length() > 0) {
                    sb.append(userInfo).append('@');
                }

                if (host != null) {
                    // TODO check IPv6 address
                    sb.append(host);
                }

                if (port != null) {
                    sb.append(':').append(port);
                }
            } else if (authority != null) {
                sb.append("//").append(authority);
            }

            if (path.length() > 0) {
                if (sb.length() > 0 && path.charAt(0) != '/') {
                    sb.append("/");
                }
                sb.append(path);
            }

            if (query.length() > 0) {
                sb.append('?').append(query);
            }
        }

        if (fragment != null && fragment.length() > 0) {
            sb.append('#').append(fragment);
        }

        return UriComponent.encodeTemplateNames(sb.toString());
    }

    private URI createURI(String uri) {
        try {
            return new URI(uri);
        } catch (URISyntaxException ex) {
            throw new UriBuilderException(ex);
        }
    }

    private String replaceNull(String s) {
        return (s != null) ? s : "";
    }

}