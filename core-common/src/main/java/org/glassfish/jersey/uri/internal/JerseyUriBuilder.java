/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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
import java.security.AccessController;
import java.util.List;
import java.util.Map;

import javax.ws.rs.Path;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;
import javax.ws.rs.core.UriBuilderException;

import org.glassfish.jersey.internal.LocalizationMessages;
import org.glassfish.jersey.internal.util.ReflectionHelper;
import org.glassfish.jersey.internal.util.collection.MultivaluedStringMap;
import org.glassfish.jersey.uri.UriComponent;
import org.glassfish.jersey.uri.UriTemplate;

import jersey.repackaged.com.google.common.collect.Maps;
import jersey.repackaged.com.google.common.net.InetAddresses;

/**
 * A Jersey implementation of {@link UriBuilder}.
 *
 * @author Paul Sandoz
 * @author Martin Matula
 * @author Miroslav Fuksa
 * @author Vetle Leinonen-Roeim (vetle at roeim.net)
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

    private JerseyUriBuilder(final JerseyUriBuilder that) {
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
    public JerseyUriBuilder uri(final URI uri) {
        if (uri == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("uri"));
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

        if (uri.getRawPath() != null && !uri.getRawPath().isEmpty()) {
            path.setLength(0);
            path.append(uri.getRawPath());
        }
        if (uri.getRawQuery() != null && !uri.getRawQuery().isEmpty()) {
            query.setLength(0);
            query.append(uri.getRawQuery());

        }

        return this;
    }

    @Override
    public JerseyUriBuilder uri(final String uriTemplate) {
        if (uriTemplate == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("uriTemplate"));
        }

        UriParser parser = new UriParser(uriTemplate);
        parser.parse();

        final String parsedScheme = parser.getScheme();
        if (parsedScheme != null) {
            scheme(parsedScheme);
        } else if (ssp != null) {
            // The previously set scheme was opaque and uriTemplate does not contain a scheme part.
            // However, the scheme might have already changed, as demonstrated in
            // JerseyUriBuilderTest.testChangeUriStringAfterChangingOpaqueSchemeToHttp().
            // So to be safe, we need to erase the existing internal SSP value and
            // re-parse the new uriTemplate using the current scheme and try to set the SSP
            // again using the re-parsed data.
            // See also JERSEY-1457 and related test.
            ssp = null;
            parser = new UriParser(scheme + ":" + uriTemplate);
            parser.parse();
        }

        schemeSpecificPart(parser);

        final String parserFragment = parser.getFragment();
        if (parserFragment != null) {
            fragment(parserFragment);
        }

        return this;
    }

    /**
     * Set scheme specific part from the URI parser.
     *
     * @param parser initialized URI parser.
     */
    private void schemeSpecificPart(final UriParser parser) {
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

        this.ssp = null;
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
    public JerseyUriBuilder scheme(final String scheme) {
        if (scheme != null) {
            this.scheme = scheme;
            UriComponent.validate(scheme, UriComponent.Type.SCHEME, true);
        } else {
            this.scheme = null;
        }
        return this;
    }

    @Override
    public JerseyUriBuilder schemeSpecificPart(final String ssp) {
        if (ssp == null) {
            throw new IllegalArgumentException(LocalizationMessages.URI_BUILDER_SCHEME_PART_NULL());
        }

        final UriParser parser = new UriParser((scheme != null) ? scheme + ":" + ssp : ssp);
        parser.parse();

        if (parser.getScheme() != null && !parser.getScheme().equals(scheme)) {
            throw new IllegalStateException(
                    LocalizationMessages.URI_BUILDER_SCHEME_PART_UNEXPECTED_COMPONENT(ssp, parser.getScheme()));
        }
        if (parser.getFragment() != null) {
            throw new IllegalStateException(
                    LocalizationMessages.URI_BUILDER_URI_PART_FRAGMENT(ssp, parser.getFragment()));
        }

        schemeSpecificPart(parser);

        return this;
    }

    @Override
    public JerseyUriBuilder userInfo(final String ui) {
        checkSsp();
        this.userInfo = (ui != null)
                ? encode(ui, UriComponent.Type.USER_INFO) : null;
        return this;
    }

    @Override
    public JerseyUriBuilder host(final String host) {
        checkSsp();
        if (host != null) {
            if (host.isEmpty()) {
                throw new IllegalArgumentException(LocalizationMessages.INVALID_HOST());
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
    public JerseyUriBuilder port(final int port) {
        checkSsp();
        if (port < -1) {
            // -1 is used to reset port setting and since URI allows
            // as port any positive integer, so do we.

            throw new IllegalArgumentException(LocalizationMessages.INVALID_PORT());
        }
        this.port = port == -1 ? null : String.valueOf(port);
        return this;
    }

    @Override
    public JerseyUriBuilder replacePath(final String path) {
        checkSsp();
        this.path.setLength(0);
        if (path != null) {
            appendPath(path);
        }
        return this;
    }

    @Override
    public JerseyUriBuilder path(final String path) {
        checkSsp();
        appendPath(path);
        return this;
    }

    @SuppressWarnings("unchecked")
    @Override
    public UriBuilder path(final Class resource) throws IllegalArgumentException {
        checkSsp();
        if (resource == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("resource"));
        }

        final Path p = Path.class.cast(resource.getAnnotation(Path.class));
        if (p == null) {
            throw new IllegalArgumentException(LocalizationMessages.URI_BUILDER_CLASS_PATH_ANNOTATION_MISSING(resource));
        }
        appendPath(p);
        return this;
    }

    @Override
    public JerseyUriBuilder path(final Class resource, final String methodName) {
        checkSsp();
        if (resource == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("resource"));
        }
        if (methodName == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("methodName"));
        }

        final Method[] methods = AccessController.doPrivileged(ReflectionHelper.getMethodsPA(resource));
        Method found = null;
        for (final Method m : methods) {
            if (methodName.equals(m.getName())) {
                if (found == null || found.isSynthetic()) {
                    found = m;
                } else if (!m.isSynthetic()) {
                    throw new IllegalArgumentException();
                }
            }
        }

        if (found == null) {
            throw new IllegalArgumentException(LocalizationMessages.URI_BUILDER_METHODNAME_NOT_SPECIFIED(methodName, resource));
        }

        appendPath(getPath(found));

        return this;
    }

    @Override
    public JerseyUriBuilder path(final Method method) {
        checkSsp();
        if (method == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("method"));
        }
        appendPath(getPath(method));
        return this;
    }

    private Path getPath(final AnnotatedElement ae) {
        final Path p = ae.getAnnotation(Path.class);
        if (p == null) {
            throw new IllegalArgumentException(LocalizationMessages.URI_BUILDER_ANNOTATEDELEMENT_PATH_ANNOTATION_MISSING(ae));
        }
        return p;
    }

    @Override
    public JerseyUriBuilder segment(final String... segments) throws IllegalArgumentException {
        checkSsp();
        if (segments == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("segments"));
        }

        for (final String segment : segments) {
            appendPath(segment, true);
        }
        return this;
    }

    @Override
    public JerseyUriBuilder replaceMatrix(final String matrix) {
        checkSsp();
        final boolean trailingSlash = path.charAt(path.length() - 1) == '/';
        final int slashIndex = trailingSlash ? path.lastIndexOf("/", path.length() - 2) : path.lastIndexOf("/");

        final int i = path.indexOf(";", slashIndex);

        if (i != -1) {
            path.setLength(i + 1);
        } else if (matrix != null) {
            path.append(';');
        }

        if (matrix != null) {
            path.append(encode(matrix, UriComponent.Type.PATH));
        } else if (i != -1) {
            path.setLength(i);

            if (trailingSlash) {
                path.append("/");
            }
        }

        return this;
    }

    @Override
    public JerseyUriBuilder matrixParam(String name, final Object... values) {
        checkSsp();
        if (name == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("name"));
        }
        if (values == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("value"));
        }
        if (values.length == 0) {
            return this;
        }

        name = encode(name, UriComponent.Type.MATRIX_PARAM);
        if (matrixParams == null) {
            for (final Object value : values) {
                path.append(';').append(name);

                if (value == null) {
                    throw new IllegalArgumentException(LocalizationMessages.MATRIX_PARAM_NULL());
                }

                final String stringValue = value.toString();
                if (!stringValue.isEmpty()) {
                    path.append('=').append(encode(stringValue, UriComponent.Type.MATRIX_PARAM));
                }
            }
        } else {
            for (final Object value : values) {
                if (value == null) {
                    throw new IllegalArgumentException(LocalizationMessages.MATRIX_PARAM_NULL());
                }

                matrixParams.add(name, encode(value.toString(), UriComponent.Type.MATRIX_PARAM));
            }
        }
        return this;
    }

    @Override
    public JerseyUriBuilder replaceMatrixParam(String name, final Object... values) {
        checkSsp();

        if (name == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("name"));
        }

        if (matrixParams == null) {
            int i = path.lastIndexOf("/");
            if (i == -1) {
                i = 0;
            }
            matrixParams = UriComponent.decodeMatrix(path.substring(i), false);
            i = path.indexOf(";", i);
            if (i != -1) {
                path.setLength(i);
            }
        }

        name = encode(name, UriComponent.Type.MATRIX_PARAM);
        matrixParams.remove(name);
        if (values != null) {
            for (final Object value : values) {
                if (value == null) {
                    throw new IllegalArgumentException(LocalizationMessages.MATRIX_PARAM_NULL());
                }

                matrixParams.add(name, encode(value.toString(), UriComponent.Type.MATRIX_PARAM));
            }
        }
        return this;
    }

    @Override
    public JerseyUriBuilder replaceQuery(final String query) {
        checkSsp();
        this.query.setLength(0);
        if (query != null) {
            this.query.append(encode(query, UriComponent.Type.QUERY));
        }
        return this;
    }

    @Override
    public JerseyUriBuilder queryParam(String name, final Object... values) {
        checkSsp();
        if (name == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("name"));
        }
        if (values == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("values"));
        }
        if (values.length == 0) {
            return this;
        }

        name = encode(name, UriComponent.Type.QUERY_PARAM);
        if (queryParams == null) {
            for (final Object value : values) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(name);

                if (value == null) {
                    throw new IllegalArgumentException(LocalizationMessages.QUERY_PARAM_NULL());
                }

                query.append('=').append(encode(value.toString(), UriComponent.Type.QUERY_PARAM));
            }
        } else {
            for (final Object value : values) {
                if (value == null) {
                    throw new IllegalArgumentException(LocalizationMessages.QUERY_PARAM_NULL());
                }

                queryParams.add(name, encode(value.toString(), UriComponent.Type.QUERY_PARAM));
            }
        }
        return this;
    }

    @Override
    public JerseyUriBuilder replaceQueryParam(String name, final Object... values) {
        checkSsp();

        if (queryParams == null) {
            queryParams = UriComponent.decodeQuery(query.toString(), false, false);
            query.setLength(0);
        }

        name = encode(name, UriComponent.Type.QUERY_PARAM);
        queryParams.remove(name);

        if (values == null) {
            return this;
        }

        for (final Object value : values) {
            if (value == null) {
                throw new IllegalArgumentException(LocalizationMessages.QUERY_PARAM_NULL());
            }

            queryParams.add(name, encode(value.toString(), UriComponent.Type.QUERY_PARAM));
        }
        return this;
    }

    @Override
    public JerseyUriBuilder resolveTemplate(final String name, final Object value) throws IllegalArgumentException {
        resolveTemplate(name, value, true, true);

        return this;
    }

    @Override
    public JerseyUriBuilder resolveTemplate(final String name, final Object value, final boolean encodeSlashInPath) {
        resolveTemplate(name, value, true, encodeSlashInPath);
        return this;
    }

    @Override
    public JerseyUriBuilder resolveTemplateFromEncoded(final String name, final Object value) {
        resolveTemplate(name, value, false, false);
        return this;
    }

    private JerseyUriBuilder resolveTemplate(final String name,
                                             final Object value,
                                             final boolean encode,
                                             final boolean encodeSlashInPath) {
        if (name == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("name"));
        }
        if (value == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("value"));
        }

        final Map<String, Object> templateValues = Maps.newHashMap();
        templateValues.put(name, value);
        resolveTemplates(templateValues, encode, encodeSlashInPath);
        return this;
    }

    @Override
    public JerseyUriBuilder resolveTemplates(final Map<String, Object> templateValues) throws IllegalArgumentException {
        resolveTemplates(templateValues, true, true);
        return this;
    }

    @Override
    public JerseyUriBuilder resolveTemplates(final Map<String, Object> templateValues, final boolean encodeSlashInPath) throws
            IllegalArgumentException {
        resolveTemplates(templateValues, true, encodeSlashInPath);
        return this;
    }

    @Override
    public JerseyUriBuilder resolveTemplatesFromEncoded(final Map<String, Object> templateValues) {
        resolveTemplates(templateValues, false, false);
        return this;
    }

    private JerseyUriBuilder resolveTemplates(final Map<String, Object> templateValues,
                                              final boolean encode,
                                              final boolean encodeSlashInPath) {
        if (templateValues == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("templateValues"));
        } else {
            for (final Map.Entry entry : templateValues.entrySet()) {
                if (entry.getKey() == null || entry.getValue() == null) {
                    throw new IllegalArgumentException(LocalizationMessages.TEMPLATE_PARAM_NULL());
                }
            }
        }

        scheme = UriTemplate.resolveTemplateValues(UriComponent.Type.SCHEME, scheme, false, templateValues);
        userInfo = UriTemplate.resolveTemplateValues(UriComponent.Type.USER_INFO, userInfo, encode, templateValues);
        host = UriTemplate.resolveTemplateValues(UriComponent.Type.HOST, host, encode, templateValues);
        port = UriTemplate.resolveTemplateValues(UriComponent.Type.PORT, port, false, templateValues);
        authority = UriTemplate.resolveTemplateValues(UriComponent.Type.AUTHORITY, authority, encode, templateValues);

        // path template values are treated as path segments unless encodeSlashInPath is false.
        final UriComponent.Type pathComponent = (encodeSlashInPath) ? UriComponent.Type.PATH_SEGMENT : UriComponent.Type.PATH;
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
    public JerseyUriBuilder fragment(final String fragment) {
        this.fragment = (fragment != null)
                ? encode(fragment, UriComponent.Type.FRAGMENT)
                : null;
        return this;
    }

    private void checkSsp() {
        if (ssp != null) {
            throw new IllegalArgumentException(LocalizationMessages.URI_BUILDER_SCHEMA_PART_OPAQUE());
        }
    }

    private void appendPath(final Path path) {
        if (path == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("path"));
        }

        appendPath(path.value());
    }

    private void appendPath(final String path) {
        appendPath(path, false);
    }

    private void appendPath(String segments, final boolean isSegment) {
        if (segments == null) {
            throw new IllegalArgumentException(LocalizationMessages.PARAM_NULL("segments"));
        }
        if (segments.isEmpty()) {
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
            if (segments.isEmpty()) {
                return;
            }
        }

        path.append(segments);
    }

    private void encodeMatrix() {
        if (matrixParams == null || matrixParams.isEmpty()) {
            return;
        }

        for (final Map.Entry<String, List<String>> e : matrixParams.entrySet()) {
            final String name = e.getKey();

            for (final String value : e.getValue()) {
                path.append(';').append(name);
                if (!value.isEmpty()) {
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

        for (final Map.Entry<String, List<String>> e : queryParams.entrySet()) {
            final String name = e.getKey();

            for (final String value : e.getValue()) {
                if (query.length() > 0) {
                    query.append('&');
                }
                query.append(name).append('=').append(value);
            }
        }
        queryParams = null;
    }

    private String encode(final String s, final UriComponent.Type type) {
        return UriComponent.contextualEncode(s, type, true);
    }

    @Override
    public URI buildFromMap(final Map<String, ?> values) {
        return _buildFromMap(true, true, values);
    }

    @Override
    public URI buildFromMap(final Map<String, ?> values, final boolean encodeSlashInPath) {
        return _buildFromMap(true, encodeSlashInPath, values);
    }

    @Override
    public URI buildFromEncodedMap(final Map<String, ?> values) throws IllegalArgumentException, UriBuilderException {
        return _buildFromMap(false, false, values);
    }

    private URI _buildFromMap(final boolean encode, final boolean encodeSlashInPath, final Map<String, ?> values) {
        if (ssp != null) {
            throw new IllegalArgumentException(LocalizationMessages.URI_BUILDER_SCHEMA_PART_OPAQUE());
        }

        encodeMatrix();
        encodeQuery();

        final String uri = UriTemplate.createURI(
                scheme, authority,
                userInfo, host, port,
                path.toString(), query.toString(), fragment, values, encode, encodeSlashInPath);
        return createURI(uri);
    }

    @Override
    public URI build(final Object... values) {
        return _build(true, true, values);
    }

    @Override
    public URI build(final Object[] values, final boolean encodeSlashInPath) {
        return _build(true, encodeSlashInPath, values);
    }

    @Override
    public URI buildFromEncoded(final Object... values) {
        return _build(false, false, values);
    }

    @Override
    public String toTemplate() {
        encodeMatrix();
        encodeQuery();

        final StringBuilder sb = new StringBuilder();

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

                if (userInfo != null && !userInfo.isEmpty()) {
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
                if (hasAuthority && path.charAt(0) != '/') {
                    sb.append("/");
                }
                sb.append(path);
            } else if (hasAuthority && (query.length() > 0 || (fragment != null && !fragment.isEmpty()))) {
                // if has authority and query or fragment and no path value, we need to append root '/' to the path
                // see URI RFC 3986 section 3.3
                sb.append("/");
            }

            if (query.length() > 0) {
                sb.append('?').append(query);
            }
        }

        if (fragment != null && !fragment.isEmpty()) {
            sb.append('#').append(fragment);
        }

        return sb.toString();
    }

    private URI _build(final boolean encode, final boolean encodeSlashInPath, final Object... values) {
        if (ssp != null) {
            if (values == null || values.length == 0) {
                return createURI(create());
            }
            throw new IllegalArgumentException(LocalizationMessages.URI_BUILDER_SCHEMA_PART_OPAQUE());
        }

        encodeMatrix();
        encodeQuery();

        final String uri = UriTemplate.createURI(
                scheme, authority,
                userInfo, host, port,
                path.toString(), query.toString(), fragment, values, encode, encodeSlashInPath);
        return createURI(uri);
    }

    private String create() {
        return UriComponent.encodeTemplateNames(toTemplate());
    }

    private URI createURI(final String uri) {
        try {
            return new URI(uri);
        } catch (final URISyntaxException ex) {
            throw new UriBuilderException(ex);
        }
    }

    @Override
    public String toString() {
        return toTemplate();
    }

    /**
     * Check whether or not the URI represented by this {@code UriBuilder} is absolute.
     * <p/>
     * A URI is absolute if, and only if, it has a scheme component.
     *
     * @return {@code true} if, and only if, the URI represented by this {@code UriBuilder} is absolute.
     * @since 2.7
     */
    public boolean isAbsolute() {
        return scheme != null;
    }
}
