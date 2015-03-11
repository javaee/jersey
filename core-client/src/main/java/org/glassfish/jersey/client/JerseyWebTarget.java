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
package org.glassfish.jersey.client;

import java.net.URI;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import javax.ws.rs.core.Link;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

import jersey.repackaged.com.google.common.base.Preconditions;

/**
 * Jersey implementation of {@link javax.ws.rs.client.WebTarget JAX-RS client target}
 * contract.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public class JerseyWebTarget implements javax.ws.rs.client.WebTarget, Initializable<JerseyWebTarget> {

    private final ClientConfig config;
    private final UriBuilder targetUri;

    /**
     * Create new web target instance.
     *
     * @param uri    target URI.
     * @param parent parent client.
     */
    /*package*/ JerseyWebTarget(String uri, JerseyClient parent) {
        this(UriBuilder.fromUri(uri), parent.getConfiguration());
    }

    /**
     * Create new web target instance.
     *
     * @param uri    target URI.
     * @param parent parent client.
     */
    /*package*/ JerseyWebTarget(URI uri, JerseyClient parent) {
        this(UriBuilder.fromUri(uri), parent.getConfiguration());
    }

    /**
     * Create new web target instance.
     *
     * @param uriBuilder builder for the target URI.
     * @param parent     parent client.
     */
    /*package*/ JerseyWebTarget(UriBuilder uriBuilder, JerseyClient parent) {
        this(uriBuilder.clone(), parent.getConfiguration());
    }

    /**
     * Create new web target instance.
     *
     * @param link   link to the target URI.
     * @param parent parent client.
     */
    /*package*/ JerseyWebTarget(Link link, JerseyClient parent) {
        // TODO handle relative links
        this(UriBuilder.fromUri(link.getUri()), parent.getConfiguration());
    }

    /**
     * Create new web target instance.
     *
     * @param uriBuilder builder for the target URI.
     * @param that       original target to copy the internal data from.
     */
    protected JerseyWebTarget(UriBuilder uriBuilder, JerseyWebTarget that) {
        this(uriBuilder, that.config);
    }

    /**
     * Create new web target instance.
     *
     * @param uriBuilder   builder for the target URI.
     * @param clientConfig target configuration.
     */
    protected JerseyWebTarget(UriBuilder uriBuilder, ClientConfig clientConfig) {
        clientConfig.checkClient();

        this.targetUri = uriBuilder;
        this.config = clientConfig.snapshot();
    }

    @Override
    public URI getUri() {
        checkNotClosed();
        try {
            return targetUri.build();
        } catch (IllegalArgumentException ex) {
            throw new IllegalStateException(ex.getMessage(), ex);
        }
    }

    private void checkNotClosed() {
        config.getClient().checkNotClosed();
    }

    @Override
    public UriBuilder getUriBuilder() {
        checkNotClosed();
        return targetUri.clone();
    }

    @Override
    public JerseyWebTarget path(String path) throws NullPointerException {
        checkNotClosed();
        Preconditions.checkNotNull(path, "path is 'null'.");

        return new JerseyWebTarget(getUriBuilder().path(path), this);
    }

    @Override
    public JerseyWebTarget matrixParam(String name, Object... values) throws NullPointerException {
        checkNotClosed();
        Preconditions.checkNotNull(name, "Matrix parameter name must not be 'null'.");

        if (values == null || values.length == 0 || (values.length == 1 && values[0] == null)) {
            return new JerseyWebTarget(getUriBuilder().replaceMatrixParam(name, (Object[]) null), this);
        }

        checkForNullValues(name, values);
        return new JerseyWebTarget(getUriBuilder().matrixParam(name, values), this);
    }

    @Override
    public JerseyWebTarget queryParam(String name, Object... values) throws NullPointerException {
        checkNotClosed();
        return new JerseyWebTarget(JerseyWebTarget.setQueryParam(getUriBuilder(), name, values), this);
    }

    private static UriBuilder setQueryParam(UriBuilder uriBuilder, String name, Object[] values) {
        if (values == null || values.length == 0 || (values.length == 1 && values[0] == null)) {
            return uriBuilder.replaceQueryParam(name, (Object[]) null);
        }

        checkForNullValues(name, values);
        return uriBuilder.queryParam(name, values);
    }

    private static void checkForNullValues(String name, Object[] values) {
        Preconditions.checkNotNull(name, "name is 'null'.");

        List<Integer> indexes = new LinkedList<Integer>();
        for (int i = 0; i < values.length; i++) {
            if (values[i] == null) {
                indexes.add(i);
            }
        }
        final int failedIndexCount = indexes.size();
        if (failedIndexCount > 0) {
            final String valueTxt;
            final String indexTxt;
            if (failedIndexCount == 1) {
                valueTxt = "value";
                indexTxt = "index";
            } else {
                valueTxt = "values";
                indexTxt = "indexes";
            }

            throw new NullPointerException(
                    String.format("'null' %s detected for parameter '%s' on %s : %s",
                            valueTxt, name, indexTxt, indexes.toString()));
        }
    }

    @Override
    public JerseyInvocation.Builder request() {
        checkNotClosed();
        return new JerseyInvocation.Builder(getUri(), config.snapshot());
    }

    @Override
    public JerseyInvocation.Builder request(String... acceptedResponseTypes) {
        checkNotClosed();
        JerseyInvocation.Builder b = new JerseyInvocation.Builder(getUri(), config.snapshot());
        b.request().accept(acceptedResponseTypes);
        return b;
    }

    @Override
    public JerseyInvocation.Builder request(MediaType... acceptedResponseTypes) {
        checkNotClosed();
        JerseyInvocation.Builder b = new JerseyInvocation.Builder(getUri(), config.snapshot());
        b.request().accept(acceptedResponseTypes);
        return b;
    }

    @Override
    public JerseyWebTarget resolveTemplate(String name, Object value) throws NullPointerException {
        return resolveTemplate(name, value, true);
    }

    @Override
    public JerseyWebTarget resolveTemplate(String name, Object value, boolean encodeSlashInPath) throws NullPointerException {
        checkNotClosed();
        Preconditions.checkNotNull(name, "name is 'null'.");
        Preconditions.checkNotNull(value, "value is 'null'.");

        return new JerseyWebTarget(getUriBuilder().resolveTemplate(name, value, encodeSlashInPath), this);
    }

    @Override
    public JerseyWebTarget resolveTemplateFromEncoded(String name, Object value)
            throws NullPointerException {
        checkNotClosed();
        Preconditions.checkNotNull(name, "name is 'null'.");
        Preconditions.checkNotNull(value, "value is 'null'.");

        return new JerseyWebTarget(getUriBuilder().resolveTemplateFromEncoded(name, value), this);
    }

    @Override
    public JerseyWebTarget resolveTemplates(Map<String, Object> templateValues) throws NullPointerException {
        return resolveTemplates(templateValues, true);
    }

    @Override
    public JerseyWebTarget resolveTemplates(Map<String, Object> templateValues, boolean encodeSlashInPath)
            throws NullPointerException {
        checkNotClosed();
        checkTemplateValues(templateValues);

        if (templateValues.isEmpty()) {
            return this;
        } else {
            return new JerseyWebTarget(getUriBuilder().resolveTemplates(templateValues, encodeSlashInPath), this);
        }
    }

    @Override
    public JerseyWebTarget resolveTemplatesFromEncoded(Map<String, Object> templateValues)
            throws NullPointerException {
        checkNotClosed();
        checkTemplateValues(templateValues);

        if (templateValues.isEmpty()) {
            return this;
        } else {
            return new JerseyWebTarget(getUriBuilder().resolveTemplatesFromEncoded(templateValues), this);
        }
    }

    /**
     * Check template values for {@code null} values. Throws {@code NullPointerException} if the name-value map or any of the
     * names or encoded values in the map is {@code null}.
     *
     * @param templateValues map to check.
     * @throws NullPointerException if the name-value map or any of the names or encoded values in the map
     * is {@code null}.
     */
    private void checkTemplateValues(final Map<String, Object> templateValues) throws NullPointerException {
        Preconditions.checkNotNull(templateValues, "templateValues is 'null'.");

        for (final Map.Entry entry : templateValues.entrySet()) {
            Preconditions.checkNotNull(entry.getKey(), "name is 'null'.");
            Preconditions.checkNotNull(entry.getValue(), "value is 'null'.");
        }
    }

    @Override
    public JerseyWebTarget register(Class<?> providerClass) {
        checkNotClosed();
        config.register(providerClass);
        return this;
    }

    @Override
    public JerseyWebTarget register(Object provider) {
        checkNotClosed();
        config.register(provider);
        return this;
    }

    @Override
    public JerseyWebTarget register(Class<?> providerClass, int bindingPriority) {
        checkNotClosed();
        config.register(providerClass, bindingPriority);
        return this;
    }

    @Override
    public JerseyWebTarget register(Class<?> providerClass, Class<?>... contracts) {
        checkNotClosed();
        config.register(providerClass, contracts);
        return this;
    }

    @Override
    public JerseyWebTarget register(Class<?> providerClass, Map<Class<?>, Integer> contracts) {
        checkNotClosed();
        config.register(providerClass, contracts);
        return this;
    }

    @Override
    public JerseyWebTarget register(Object provider, int bindingPriority) {
        checkNotClosed();
        config.register(provider, bindingPriority);
        return this;
    }

    @Override
    public JerseyWebTarget register(Object provider, Class<?>... contracts) {
        checkNotClosed();
        config.register(provider, contracts);
        return this;
    }

    @Override
    public JerseyWebTarget register(Object provider, Map<Class<?>, Integer> contracts) {
        checkNotClosed();
        config.register(provider, contracts);
        return this;
    }

    @Override
    public JerseyWebTarget property(String name, Object value) {
        checkNotClosed();
        config.property(name, value);
        return this;
    }

    @Override
    public ClientConfig getConfiguration() {
        checkNotClosed();
        return config.getConfiguration();
    }

    @Override
    public JerseyWebTarget preInitialize() {
        config.preInitialize();
        return this;
    }

    @Override
    public String toString() {
        return "JerseyWebTarget { " + targetUri.toTemplate() + " }";
    }
}
