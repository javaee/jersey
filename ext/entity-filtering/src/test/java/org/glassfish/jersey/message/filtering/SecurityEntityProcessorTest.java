/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.message.filtering;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jersey.repackaged.com.google.common.collect.Sets;
import org.glassfish.jersey.message.filtering.spi.EntityGraph;
import org.glassfish.jersey.message.filtering.spi.EntityProcessor;
import org.glassfish.jersey.message.filtering.spi.EntityProcessorContext;
import org.glassfish.jersey.message.filtering.spi.FilteringHelper;
import org.glassfish.jersey.message.filtering.spi.ScopeProvider;

import org.junit.Before;
import org.junit.Test;
import static org.hamcrest.CoreMatchers.equalTo;
import static org.hamcrest.MatcherAssert.assertThat;

/**
 * {@link org.glassfish.jersey.message.filtering.SecurityEntityProcessor} unit tests.
 *
 * @author Michal Gajdos
 */
@SuppressWarnings("JavaDoc")
public class SecurityEntityProcessorTest {

    private SecurityEntityProcessor processor;

    @Before
    public void setUp() throws Exception {
        processor = new SecurityEntityProcessor();
    }

    @Test
    public void testProcessPermitAllClass() throws Exception {
        final EntityGraph actual = new EntityGraphImpl(PermitAllEntity.class);

        final EntityGraph expected = new EntityGraphImpl(PermitAllEntity.class);
        expected.addFilteringScopes(FilteringHelper.getDefaultFilteringScope());

        for (final boolean forWriter : new boolean[] {true, false}) {
            final EntityProcessor.Result result = testProcessClass(PermitAllEntity.class, actual, forWriter);

            assertThat(result, equalTo(EntityProcessor.Result.APPLY));
            assertThat(actual, equalTo(expected));
        }
    }

    @Test
    public void testProcessDenyAllClass() throws Exception {
        final EntityGraph actual = new EntityGraphImpl(DenyAllEntity.class);
        final EntityGraph expected = new EntityGraphImpl(DenyAllEntity.class);

        for (final boolean forWriter : new boolean[] {true, false}) {
            final EntityProcessor.Result result = testProcessClass(DenyAllEntity.class, actual, forWriter);

            assertThat(result, equalTo(EntityProcessor.Result.ROLLBACK));
            assertThat(actual, equalTo(expected));
        }
    }

    @Test
    public void testProcessRolesAllowedClass() throws Exception {
        final EntityGraph actual = new EntityGraphImpl(RolesAllowedEntity.class);

        final EntityGraph expected = new EntityGraphImpl(RolesAllowedEntity.class);
        expected.addFilteringScopes(Sets.newHashSet(SecurityHelper.getRolesAllowedScope("manager"),
                SecurityHelper.getRolesAllowedScope("client")));

        for (final boolean forWriter : new boolean[] {true, false}) {
            final EntityProcessor.Result result = testProcessClass(RolesAllowedEntity.class, actual, forWriter);

            assertThat(result, equalTo(EntityProcessor.Result.APPLY));
            assertThat(actual, equalTo(expected));
        }
    }

    private EntityProcessor.Result testProcessClass(final Class<?> clazz, final EntityGraph graph, final boolean forWriter)
            throws Exception {

        final EntityProcessorContext context = new EntityProcessorContextImpl(
                forWriter ? EntityProcessorContext.Type.CLASS_WRITER : EntityProcessorContext.Type.CLASS_READER,
                clazz, graph);

        return processor.process(context);
    }

    @Test
    public void testProcessPermitAllProperties() throws Exception {
        final EntityGraph actual = new EntityGraphImpl(PermitAllEntity.class);

        final EntityGraph expected = new EntityGraphImpl(PermitAllEntity.class);
        expected.addField("field", ScopeProvider.DEFAULT_SCOPE);

        for (final boolean forWriter : new boolean[] {true, false}) {
            final EntityProcessor.Result result = testProcessProperty(PermitAllEntity.class, actual, forWriter);

            assertThat(result, equalTo(EntityProcessor.Result.APPLY));
            assertThat(actual, equalTo(expected));
        }
    }

    @Test
    public void testProcessDenyAllProperties() throws Exception {
        final EntityGraph actual = new EntityGraphImpl(DenyAllEntity.class);
        final EntityGraph expected = new EntityGraphImpl(DenyAllEntity.class);

        for (final boolean forWriter : new boolean[] {true, false}) {
            final EntityProcessor.Result result = testProcessProperty(DenyAllEntity.class, actual, forWriter);

            assertThat(result, equalTo(EntityProcessor.Result.ROLLBACK));
            assertThat(actual, equalTo(expected));
        }
    }

    @Test
    public void testProcessRolesAllowedProperties() throws Exception {
        final EntityGraph actual = new EntityGraphImpl(RolesAllowedEntity.class);
        final EntityGraph expected = new EntityGraphImpl(RolesAllowedEntity.class);

        for (final boolean forWriter : new boolean[] {true, false}) {
            final EntityProcessor.Result result = testProcessProperty(RolesAllowedEntity.class, actual, forWriter);

            if (forWriter) {
                expected.addField("field", SecurityHelper.getRolesAllowedScope("manager"));
            } else {
                expected.addField("field", SecurityHelper.getRolesAllowedScope("client"));
            }

            assertThat(result, equalTo(EntityProcessor.Result.APPLY));
            assertThat(actual, equalTo(expected));
        }
    }

    private EntityProcessor.Result testProcessProperty(final Class<?> clazz, final EntityGraph graph, final boolean forWriter)
            throws Exception {

        final Field field = clazz.getDeclaredField("field");
        final Method method = forWriter ? clazz.getMethod("getField") : clazz.getMethod("setField", String.class);

        final EntityProcessorContext context = new EntityProcessorContextImpl(
                forWriter ? EntityProcessorContext.Type.PROPERTY_WRITER : EntityProcessorContext.Type.PROPERTY_WRITER,
                field, method, graph);

        return processor.process(context);
    }

    @Test
    public void testProcessPermitAllAccessors() throws Exception {
        final EntityGraph actual = new EntityGraphImpl(PermitAllEntity.class);
        actual.addFilteringScopes(FilteringHelper.getDefaultFilteringScope());

        final EntityGraph expected = new EntityGraphImpl(PermitAllEntity.class);
        expected.addFilteringScopes(FilteringHelper.getDefaultFilteringScope());
        expected.addSubgraph("subgraph", SubEntity.class, ScopeProvider.DEFAULT_SCOPE);

        for (final boolean forWriter : new boolean[] {true, false}) {
            final EntityProcessor.Result result = testProcessAccessor(PermitAllEntity.class, actual, forWriter);

            assertThat(result, equalTo(EntityProcessor.Result.APPLY));
            assertThat(actual, equalTo(expected));
        }
    }

    @Test
    public void testProcessDenyAllAccessors() throws Exception {
        final EntityGraph actual = new EntityGraphImpl(DenyAllEntity.class);
        final EntityGraph expected = new EntityGraphImpl(DenyAllEntity.class);

        for (final boolean forWriter : new boolean[] {true, false}) {
            final EntityProcessor.Result result = testProcessAccessor(DenyAllEntity.class, actual, forWriter);

            assertThat(result, equalTo(EntityProcessor.Result.ROLLBACK));
            assertThat(actual, equalTo(expected));
        }
    }

    @Test
    public void testProcessRolesAllowedAccessor() throws Exception {
        final EntityGraph actual = new EntityGraphImpl(RolesAllowedEntity.class);
        final EntityGraph expected = new EntityGraphImpl(RolesAllowedEntity.class);

        for (final boolean forWriter : new boolean[] {true, false}) {
            final EntityProcessor.Result result = testProcessAccessor(RolesAllowedEntity.class, actual, forWriter);

            if (forWriter) {
                expected.addSubgraph("subgraph", SubEntity.class, SecurityHelper.getRolesAllowedScope("manager"));
            } else {
                expected.addSubgraph("subgraph", SubEntity.class, SecurityHelper.getRolesAllowedScope("client"));
            }

            assertThat(result, equalTo(EntityProcessor.Result.APPLY));
            assertThat(actual, equalTo(expected));
        }
    }

    private EntityProcessor.Result testProcessAccessor(final Class<?> clazz, final EntityGraph graph, final boolean forWriter)
            throws Exception {

        final Method method = forWriter ? clazz.getMethod("getSubgraph") : clazz.getMethod("setSubgraph", SubEntity.class);

        final EntityProcessorContext context = new EntityProcessorContextImpl(
                forWriter ? EntityProcessorContext.Type.PROPERTY_WRITER : EntityProcessorContext.Type.PROPERTY_WRITER,
                method, graph);

        return processor.process(context);
    }
}
