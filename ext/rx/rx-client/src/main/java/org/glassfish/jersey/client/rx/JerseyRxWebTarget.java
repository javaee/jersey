/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.client.rx;

import java.net.URI;
import java.util.Map;
import java.util.concurrent.ExecutorService;

import javax.ws.rs.client.WebTarget;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.UriBuilder;

/**
 * Jersey implementation of {@link org.glassfish.jersey.client.rx.RxWebTarget reactive client target}, extension of JAX-RS
 * {@link javax.ws.rs.client.WebTarget client target} contract.
 *
 * @param <RX> the concrete reactive invocation type.
 * @author Michal Gajdos
 */
final class JerseyRxWebTarget<RX extends RxInvoker> implements RxWebTarget<RX> {

    private final Class<RX> invokerType;
    private final ExecutorService executor;

    private WebTarget target;

    JerseyRxWebTarget(final WebTarget target, final Class<RX> invokerType, final ExecutorService executor) {
        this.target = target;
        this.invokerType = invokerType;
        this.executor = executor;
    }

    @Override
    public URI getUri() {
        return target.getUri();
    }

    @Override
    public UriBuilder getUriBuilder() {
        return target.getUriBuilder();
    }

    @Override
    public JerseyRxWebTarget<RX> path(final String path) {
        return new JerseyRxWebTarget<>(target.path(path), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> resolveTemplate(final String name, final Object value) {
        return new JerseyRxWebTarget<>(target.resolveTemplate(name, value), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> resolveTemplate(final String name, final Object value, final boolean encodeSlashInPath) {
        return new JerseyRxWebTarget<>(target.resolveTemplate(name, value, encodeSlashInPath), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> resolveTemplateFromEncoded(final String name, final Object value) {
        return new JerseyRxWebTarget<>(target.resolveTemplateFromEncoded(name, value), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> resolveTemplates(final Map<String, Object> templateValues) {
        return new JerseyRxWebTarget<>(target.resolveTemplates(templateValues), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> resolveTemplates(final Map<String, Object> templateValues, final boolean encodeSlashInPath) {
        return new JerseyRxWebTarget<>(target.resolveTemplates(templateValues, encodeSlashInPath), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> resolveTemplatesFromEncoded(final Map<String, Object> templateValues) {
        return new JerseyRxWebTarget<>(target.resolveTemplatesFromEncoded(templateValues), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> matrixParam(final String name, final Object... values) {
        return new JerseyRxWebTarget<>(target.matrixParam(name, values), invokerType, executor);
    }

    @Override
    public JerseyRxWebTarget<RX> queryParam(final String name, final Object... values) {
        return new JerseyRxWebTarget<>(target.queryParam(name, values), invokerType, executor);
    }

    @Override
    public JerseyRxInvocationBuilder<RX> request() {
        return new JerseyRxInvocationBuilder<>(target.request(), invokerType, executor);
    }

    @Override
    public JerseyRxInvocationBuilder<RX> request(final String... acceptedResponseTypes) {
        return new JerseyRxInvocationBuilder<>(target.request(acceptedResponseTypes), invokerType, executor);
    }

    @Override
    public JerseyRxInvocationBuilder<RX> request(final MediaType... acceptedResponseTypes) {
        return new JerseyRxInvocationBuilder<>(target.request(acceptedResponseTypes), invokerType, executor);
    }

    @Override
    public Configuration getConfiguration() {
        return target.getConfiguration();
    }

    @Override
    public JerseyRxWebTarget<RX> property(final String name, final Object value) {
        target = target.property(name, value);
        return this;
    }

    @Override
    public JerseyRxWebTarget<RX> register(final Class<?> componentClass) {
        target = target.register(componentClass);
        return this;
    }

    @Override
    public JerseyRxWebTarget<RX> register(final Class<?> componentClass, final int priority) {
        target = target.register(componentClass, priority);
        return this;
    }

    @Override
    public JerseyRxWebTarget<RX> register(final Class<?> componentClass, final Class<?>... contracts) {
        target = target.register(componentClass, contracts);
        return this;
    }

    @Override
    public JerseyRxWebTarget<RX> register(final Class<?> componentClass, final Map<Class<?>, Integer> contracts) {
        target = target.register(componentClass, contracts);
        return this;
    }

    @Override
    public JerseyRxWebTarget<RX> register(final Object component) {
        target = target.register(component);
        return this;
    }

    @Override
    public JerseyRxWebTarget<RX> register(final Object component, final int priority) {
        target = target.register(component, priority);
        return this;
    }

    @Override
    public JerseyRxWebTarget<RX> register(final Object component, final Class<?>... contracts) {
        target = target.register(component, contracts);
        return this;
    }

    @Override
    public JerseyRxWebTarget<RX> register(final Object component, final Map<Class<?>, Integer> contracts) {
        target = target.register(component, contracts);
        return this;
    }
}
