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

import com.google.common.collect.Maps;
import com.google.common.net.InetAddresses;

/**
 * A Jersey implementation of {@link UriBuilder}.
 *
 * @author Paul Sandoz
 * @author Martin Matula (martin.matula at oracle.com)
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 */
public class JerseyUriBuilder extends UriBuilder {

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

    /**
     * Create new implementation of {@code UriBuilder}.
     */
    public JerseyUriBuilder() {
        path = new StringBuilder();
        query = new StringBuilder();
    }

    private JerseyUriBuilder(JerseyUriBuilder that) {
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

    @SuppressWarnings("CloneDoesntCallSuperClone")
    @Override
    public JerseyUriBuilder clone() {
        return new JerseyUriBuilder(this);
    }

    @Override
    public JerseyUriBuilder uri(URI uri) {
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

    @Override
    public JerseyUriBuilder uri(String uriTemplate) {
        if (uriTemplate == null) {
            throw new IllegalArgumentException("URI parameter is null");
        }

        UriParser parser = new UriParser(uriTemplate);
        parser.parse();

        scheme(parser.getScheme());

        schemeSpecificPart(parser);

        fragment(parser.getFragment());

        return this;
    }

    /**
     * Set scheme specific part from the URI parser.
     *
     * @param parser initialized URI parser.
     * @return {@code true} if the parser data has been fully consumed, {@code false} otherwise.
     */
    private void schemeSpecificPart(UriParser parser) {
        if (parser.isOpaque()) {
            if (parser.getSsp() != null) {
                this.authority = this.host = this.port = null;
                this.path.setLength(0);
                this.query.setLength(0);

                // TODO encode or validate scheme specific part
                this.ssp = parser.getSsp();
            }
            return;
        }

        if (parser.getAuthority() != null) {
            if (parser.getUserInfo() == null && parser.getHost() == null && parser.getPort() == null) {
                this.authority = encode(parser.getAuthority(), UriComponent.Type.AUTHORITY);
                this.userInfo = null;
                this.host = null;
                this.port = null;
            } else {
                this.authority = null;
                if (parser.getUserInfo() != null) {
                    userInfo(parser.getUserInfo());
                }
                if (parser.getHost() != null) {
                    host(parser.getHost());
                }
                if (parser.getPort() != null) {
                    this.port = parser.getPort();
                }
            }
        }

        if (parser.getPath() != null) {
            this.path.setLength(0);
            path(parser.getPath());
        }
        if (parser.getQuery() != null) {
            this.query.setLength(0);
            this.query.append(parser.getQuery());
        }
    }

    @Override
    public JerseyUriBuilder scheme(String scheme) {
        if (scheme != null) {
            this.scheme = scheme;
            UriComponent.validate(scheme, UriComponent.Type.SCHEME, true);
        } else {
            this.scheme = null;
        }
        return this;
    }

    @Override
    public JerseyUriBuilder schemeSpecificPart(String ssp) {
        if (ssp == null) {
            throw new IllegalArgumentException("Supplied scheme-specific part parameter is null");
        }

        UriParser parser = new UriParser((scheme != null) ? scheme + ":" + ssp : ssp);
        parser.parse();

        if (parser.getScheme() != null && !parser.getScheme().equals(scheme)) {
            throw new IllegalStateException(String.format(
                    "Supplied scheme-specific URI part '%s' contains unexpected URI Scheme component: '%s'",
                    ssp, parser.getScheme()));
        }
        if (parser.getFragment() != null) {
            throw new IllegalStateException(String.format(
                    "Supplied scheme-specific URI part '%s' contains URI Fragment component: '%s'",
                    ssp, parser.getFragment()));
        }

        schemeSpecificPart(parser);

        return this;
    }

    @Override
    public JerseyUriBuilder userInfo(String ui) {
        checkSsp();
        this.userInfo = (ui != null)
                ? encode(ui, UriComponent.Type.USER_INFO) : null;
        return this;
    }

    @Override
    public JerseyUriBuilder host(String host) {
        checkSsp();
        if (host != null) {
            if (host.length() == 0) {
                throw new IllegalArgumentException("Invalid host name");
            }
            if (InetAddresses.isMappedIPv4Address(host) || InetAddresses.isUriInetAddress(host)) {
                this.host = host;
            } else {
                this.host = encode(host, UriComponent.Type.HOST);
            }
        } else {
            // null is used to reset host setting
            this.host = null;
        }
        return this;
    }

    @Override
    public JerseyUriBuilder port(int port) {
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
    public JerseyUriBuilder replacePath(String path) {
        checkSsp();
        this.path.setLength(0);
        if (path != null) {
            appendPath(path);
        }
        return this;
    }

    @Override
    public JerseyUriBuilder path(String path) {
        checkSsp();
        appendPath(path);
        return this;
    }

    @Override
    public JerseyUriBuilder path(Class<?> resource) {
        checkSsp();
        if (resource == null) {
            throw new IllegalArgumentException("Resource parameter is null");
        }

        Path p = resource.getAnnotation(Path.class);
        if (p == null) {
            throw new IllegalArgumentException("The class, " + resource + " is not annotated with @Path");
        }
        appendPath(p);
        return this;
    }

    @Override
    public JerseyUriBuilder path(Class resource, String methodName) {
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
    public JerseyUriBuilder path(Method method) {
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
    public JerseyUriBuilder segment(String... segments) throws IllegalArgumentException {
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
    public JerseyUriBuilder replaceMatrix(String matrix) {
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
    public JerseyUriBuilder matrixParam(String name, Object... values) {
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
    public JerseyUriBuilder replaceMatrixParam(String name, Object... values) {
        checkSsp();

        if (name == null) {
            throw new IllegalArgumentException("Name parameter is null");
        }

        if (matrixParams == null) {
            int i = path.lastIndexOf("/");
            if (i != -1) {
                i = 0;
            }
            matrixParams = UriComponent.decodeMatrix((i != -1) ? path.substring(i) : path.toString(), false);
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
    public JerseyUriBuilder replaceQuery(String query) {
        checkSsp();
        this.query.setLength(0);
        if (query != null) {
            this.query.append(encode(query, UriComponent.Type.QUERY));
        }
        return this;
    }

    @Override
    public JerseyUriBuilder queryParam(String name, Object... values) {
        checkSsp();
        if (name == null) {
            throw new IllegalArgumentException("Name is null");
        }
        if (values == null) {
            throw new IllegalArgumentException("Value is null");
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

                query.append('=').append(encode(value.toString(), UriComponent.Type.QUERY_PARAM));
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
    public JerseyUriBuilder replaceQueryParam(String name, Object... values) {
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
    public JerseyUriBuilder resolveTemplate(String name, Object value) throws IllegalArgumentException {
        resolveTemplate(name, value, true, true);

        return this;
    }

    @Override
    public JerseyUriBuilder resolveTemplate(String name, Object value, boolean encodeSlashInPath) {
        resolveTemplate(name, value, true, encodeSlashInPath);
        return this;
    }

    @Override
    public JerseyUriBuilder resolveTemplateFromEncoded(String name, Object value) {
        resolveTemplate(name, value, false, false);
        return this;
    }

    private JerseyUriBuilder resolveTemplate(String name, Object value, boolean encode, boolean encodeSlashInPath) {
        if (name == null) {
            throw new IllegalArgumentException("Name is null.");
        }
        if (value == null) {
            throw new IllegalArgumentException("Value is null.");
        }

        Map<String, Object> templateValues = Maps.newHashMap();
        templateValues.put(name, value);
        resolveTemplates(templateValues, encode, encodeSlashInPath);
        return this;
    }


    @Override
    public JerseyUriBuilder resolveTemplates(Map<String, Object> templateValues) throws IllegalArgumentException {
        resolveTemplates(templateValues, true, true);
        return this;
    }

    public JerseyUriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath) throws
            IllegalArgumentException {
        resolveTemplates(templateValues, true, encodeSlashInPath);
        return this;
    }

    @Override
    public JerseyUriBuilder resolveTemplatesFromEncoded(Map<String, Object> templateValues) {
        resolveTemplates(templateValues, false, false);
        return this;
    }


    private JerseyUriBuilder resolveTemplates(Map<String, Object> templateValues, boolean encode, boolean encodeSlashInPath) {
        if (templateValues == null) {
            throw new IllegalArgumentException("templateValues parameter is null.");
        }

        scheme = UriTemplate.resolveTemplateValues(UriComponent.Type.SCHEME, scheme, false, templateValues);
        userInfo = UriTemplate.resolveTemplateValues(UriComponent.Type.USER_INFO, userInfo, encode, templateValues);
        host = UriTemplate.resolveTemplateValues(UriComponent.Type.HOST, host, encode, templateValues);
        port = UriTemplate.resolveTemplateValues(UriComponent.Type.PORT, port, false, templateValues);
        authority = UriTemplate.resolveTemplateValues(UriComponent.Type.AUTHORITY, authority, encode, templateValues);

        // path template values are treated as path segments unless encodeSlashInPath is false.
        UriComponent.Type pathComponent = (encodeSlashInPath) ? UriComponent.Type.PATH_SEGMENT : UriComponent.Type.PATH;
        final String newPath = UriTemplate.resolveTemplateValues(pathComponent, path.toString(), encode, templateValues);
        path.setLength(0);
        path.append(newPath);

        final String newQuery = UriTemplate.resolveTemplateValues(UriComponent.Type.QUERY_PARAM, query.toString(), encode,
                templateValues);
        query.setLength(0);
        query.append(newQuery);

        fragment = UriTemplate.resolveTemplateValues(UriComponent.Type.FRAGMENT, fragment, encode, templateValues);

        return this;
    }


    @Override
    public JerseyUriBuilder fragment(String fragment) {
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
                query.append(name).append('=').append(value);
            }
        }
        queryParams = null;
    }

    private String encode(String s, UriComponent.Type type) {
        return UriComponent.contextualEncode(s, type, true);
    }

    @Override
    public URI buildFromMap(Map<String, ?> values) {
        return _buildFromMap(true, true, values);
    }

    //@Override
    public URI buildFromMap(Map<String, ?> values, boolean encodeSlashInPath) {
        return _buildFromMap(true, encodeSlashInPath, values);
    }

    @Override
    public URI buildFromEncodedMap(Map<String, ?> values) throws IllegalArgumentException, UriBuilderException {
        return _buildFromMap(false, false, values);
    }

    private URI _buildFromMap(boolean encode, boolean encodeSlashInPath, Map<String, ?> values) {
        if (ssp != null) {
            throw new IllegalArgumentException("Schema specific part is opaque");
        }

        encodeMatrix();
        encodeQuery();

        String uri = UriTemplate.createURI(
                scheme, authority,
                userInfo, host, port,
                path.toString(), query.toString(), fragment, values, encode, encodeSlashInPath);
        return createURI(uri);
    }

    @Override
    public URI build(Object... values) {
        return _build(true, true, values);
    }

    // @Override
    public URI build(Object[] values, boolean encodeSlashInPath) {
        return _build(true, encodeSlashInPath, values);
    }

    @Override
    public URI buildFromEncoded(Object... values) {
        return _build(false, false, values);
    }

    // @Override
    public String toTemplate() {
        encodeMatrix();
        encodeQuery();

        StringBuilder sb = new StringBuilder();

        if (scheme != null) {
            sb.append(scheme).append(':');
        }

        if (ssp != null) {
            sb.append(ssp);
        } else {
            boolean hasAuthority = false;
            if (userInfo != null || host != null || port != null) {
                hasAuthority = true;
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
                hasAuthority = true;
                sb.append("//").append(authority);
            }

            if (path.length() > 0) {
                if (sb.length() > 0 && path.charAt(0) != '/') {
                    sb.append("/");
                }
                sb.append(path);
            } else if (hasAuthority && (query.length() > 0 || (fragment != null && fragment.length() > 0))) {
                // if has authority and query or fragment and no path value, we need to append root '/' to the path
                // see URI RFC 3986 section 3.3
                sb.append("/");
            }

            if (query.length() > 0) {
                sb.append('?').append(query);
            }
        }

        if (fragment != null && fragment.length() > 0) {
            sb.append('#').append(fragment);
        }

        return sb.toString();
    }

    private URI _build(boolean encode, boolean encodeSlashInPath, Object... values) {
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
                path.toString(), query.toString(), fragment, values, encode, encodeSlashInPath);
        return createURI(uri);
    }

    private String create() {
        return UriComponent.encodeTemplateNames(toTemplate());
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
