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

/**
 * Implementation of {@link org.glassfish.jersey.tests.performance.tools.TestValueGenerator} producing constant results.
 *
 * Due to its constant nature, this strategy is not suitable for use with {@link java.util.Set}.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public class ConstantTestValueGenerator extends TestValueGenerator {
    private static final int intConstant = 123456789;
    private static final int charConstant = 'x';
    private static final String stringConstant = "Hello, world!";
    private static final long longConstant = 987654321L;
    private static final float floatConstant = 3.1415f;
    private static final double doubleConstant = 3.1415926535;
    private static final byte byteConstant = (byte) 127;
    private static final short shortConstant = (short) 1024;
    private static final boolean booleanConstant = true;

    /**
     * {@inheritDoc}
     */
    @Override
    public int getInt() {
        return intConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public char getChar() {
        return charConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public String getString() {
        return stringConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public long getLong() {
        return longConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public float getFloat() {
        return floatConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public double getDouble() {
        return doubleConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public byte getByte() {
        return byteConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public short getShort() {
        return shortConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean getBoolean() {
        return booleanConstant;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public <T> T getEnum(Class<T> enumType) {
        T[] values = enumType.getEnumConstants();
        if (values != null && values.length > 0) {
            return values[0];
        }
        return null;
    }
}
