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

package org.glassfish.jersey.message.filtering.spi;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

/**
 * {@link EntityProcessor Entity processor} context providing details about entity processing.
 * <p/>
 * The context contains the {@link Type} which distinguishes between types of context. There are various properties in the
 * context (accessible by getters) and some of them might be relevant only to specific context types.
 *
 * @author Michal Gajdos
 */
public interface EntityProcessorContext {

    /**
     * The type of the context which describes in which entity processing phase the
     * {@link EntityProcessor#process(EntityProcessorContext)} is triggered.
     */
    public enum Type {

        /**
         * Context created to process entity class when reading entity from an input stream into an Java object. Properties
         * available for this type are: {@link #getEntityClass()}, {@link #getEntityGraph()}.
         */
        CLASS_READER,

        /**
         * Context created to process entity class when writing entity to an output stream from an Java object. Properties
         * available for this type are: {@link #getEntityClass()}, {@link #getEntityGraph()}.
         */
        CLASS_WRITER,

        /**
         * Context created to process entity properties when reading entity from an input stream into an Java object. Properties
         * available for this type are: {@link #getField()}, {@link #getMethod()}, {@link #getEntityGraph()}.
         */
        PROPERTY_READER,

        /**
         * Context created to process entity properties when writing entity to an output stream from an Java object. Properties
         * available for this type are: {@link #getField()}, {@link #getMethod()}, {@link #getEntityGraph()}.
         */
        PROPERTY_WRITER,

        /**
         * Context created to process property accessors when reading entity from an input stream into an Java object. Properties
         * available for this type are: {@link #getMethod()}, {@link #getEntityGraph()}.
         */
        METHOD_READER,

        /**
         * Context created to process property accessors when writing entity to an output stream from an Java object. Properties
         * available for this type are: {@link #getMethod()}, {@link #getEntityGraph()}.
         */
        METHOD_WRITER
    }

    /**
     * Get the {@link Type type} of this context.
     *
     * @return entity processing context type.
     */
    public Type getType();

    /**
     * Get entity class to be processed. The entity class is available only for {@link Type#CLASS_WRITER} and
     * {@link Type#CLASS_READER} context types.
     *
     * @return entity class or {@code null} if the class is not available.
     */
    public Class<?> getEntityClass();

    /**
     * Get field to be processed. The field is available only for {@link Type#PROPERTY_WRITER} and
     * {@link Type#PROPERTY_READER} context types.
     *
     * @return field or {@code null} if the field is not available.
     */
    public Field getField();

    /**
     * Get method to be processed. The method is available for {@link Type#PROPERTY_WRITER}, {@link Type#PROPERTY_READER},
     * {@link Type#METHOD_WRITER}, {@link Type#METHOD_READER} context types.
     *
     * @return method or {@code null} if the method is not available.
     */
    public Method getMethod();

    /**
     * Get entity graph to be modified by the processing. The entity graph is available for all context types.
     *
     * @return entity graph.
     */
    public EntityGraph getEntityGraph();
}
