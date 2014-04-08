/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.performance.tools;

import java.util.Random;

/**
 * Implementation of {@link org.glassfish.jersey.tests.performance.tools.TestValueGenerator} producing random results.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class RandomTestValueGenerator extends TestValueGenerator {
    private static final int MAX_STRING_LENGTH = 50;

    private static final Random random = new Random();
    private static final String characters = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz1234567890 _";

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getEnum(Class<T> enumType) {
        T[] enumValues = enumType.getEnumConstants();
        return enumValues[random.nextInt(enumValues.length)];
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt() {
        return random.nextInt();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char getChar() {
        return (char) random.nextInt(Character.MAX_VALUE + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString() {
        return randomString(random.nextInt(MAX_STRING_LENGTH));
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong() {
        return random.nextLong();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat() {
        return random.nextFloat();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble() {
        return random.nextDouble();
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getByte() {
        return (byte) random.nextInt(Byte.MAX_VALUE + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getShort() {
        return (short) random.nextInt(Short.MAX_VALUE + 1);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean() {
        return random.nextBoolean();
    }

    private String randomString(int length) {
        StringBuilder sb = new StringBuilder();
        for (int i = 0; i < length; i++) {
            sb.append(characters.charAt(random.nextInt(characters.length())));
        }
        return sb.toString();
    }

}
