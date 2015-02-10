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

package org.glassfish.jersey.uri.internal;

import java.util.NoSuchElementException;

/**
 * Iterator which iterates through the input string and returns characters from that string.
 *
 * @author Miroslav Fuksa
 */
final class CharacterIterator {
    private int pos;
    private String s;

    /**
     * Creates a new iterator initialized with the given input string.
     *
     * @param s String trough which the iterator iterates.
     */
    public CharacterIterator(final String s) {
        this.s = s;
        this.pos = -1;
    }

    /**
     * Determines whether there is next character in the iteration chain.
     *
     * @return True if there is a character which can be retrieved by {@link #next()}, false otherwise.
     */
    public boolean hasNext() {
        return pos < s.length() - 1;
    }

    /**
     * Returns next character in the iteration chain and increase the current position.
     *
     * @return Next character.
     * @throws RuntimeException The method might throw exception when there is no more character to be retrieved.
     */
    public char next() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }
        return s.charAt(++pos);
    }

    /**
     * Returns the next character without increasing the position. The method does the same as {@link #next()} but
     * the position is not changed by calling this method.
     *
     * @return Next character.
     */
    public char peek() {
        if (!hasNext()) {
            throw new NoSuchElementException();
        }

        return s.charAt(pos + 1);
    }


    /**
     * Returns the current internal position of the iterator.
     *
     * @return current position of the iterator
     */
    public int pos() {
        return pos;
    }

    /**
     * Returns the input String on which this {@link CharacterIterator iterator} operates.
     *
     * @return String which initialized this iterator.
     */
    public String getInput() {
        return s;
    }

    /**
     * Changes the current position to the position.
     *
     * @param newPosition New position for the iterator.
     */
    public void setPosition(int newPosition) {
        if (newPosition > this.s.length() - 1) {
            throw new IndexOutOfBoundsException("Given position " + newPosition + " is outside the input string range.");
        }
        this.pos = newPosition;
    }

    /**
     * Returns character at the current position.
     *
     * @return Character from current position.
     */
    public char current() {
        if (pos == -1) {
            throw new IllegalStateException("Iterator not used yet.");
        }
        return s.charAt(pos);
    }
}
