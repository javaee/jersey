/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2014-2017 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://oss.oracle.com/licenses/CDDL+GPL-1.1
 * or LICENSE.txt.  See the License for the specific
 * language governing permissions and limitations under the License.
 *
 * When distributing the software, include this License Header Notice in each
 * file and include the License file at LICENSE.txt.
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

package org.glassfish.jersey.examples.tonegen;

/**
 * Enum representing the DTMF tone and its parameters.
 *
 * @author Adam Lindenthal (adam.lindenthal at oracle.com)
 */
public enum DTMFTone {
    TONE_1('1', 697, 1209),
    TONE_2('2', 697, 1336),
    TONE_3('3', 697, 1477),
    TONE_4('4', 770, 1209),
    TONE_5('5', 770, 1336),
    TONE_6('6', 770, 1477),
    TONE_7('7', 852, 1209),
    TONE_8('8', 852, 1336),
    TONE_9('9', 852, 1477),
    TONE_0('0', 941, 1336),
    TONE_HASH('#', 941, 1477),
    TONE_STAR('*', 941, 1209);

    private char value;
    private int loFreq;
    private int hiFreq;

    private DTMFTone(char value, int loFreq, int hiFreq) {
        this.value = value;
        this.loFreq = loFreq;
        this.hiFreq = hiFreq;
    }

    /**
     * @return character representation of the DTMF tone
     */
    public char getValue() {
        return this.value;
    }

    /**
     * @return the lower frequency of DTMF tone in Herz [Hz]
     */
    public int getLoFreq() {
        return this.loFreq;
    }

    /**
     * @return the higher frequency of DTMF tone in Herz [Hz]
     */
    public int getHiFreq() {
        return this.hiFreq;
    }

    /**
     * Returns an instance of the enum for given character representation.
     *
     * @param value character representation of the DTMF tone.
     * @return enum instance
     */
    public static DTMFTone forValue(char value) {
        for (DTMFTone tone : DTMFTone.values()) {
            if (tone.getValue() == value) {
                return tone;
            }
        }
        return null;
    }
}
