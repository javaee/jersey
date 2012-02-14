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
package org.glassfish.jersey.media.json.internal.reader;

/**
 * JSON token.
 *
 * @author Jakub Podlesak
 */
class JsonToken {

    public static final int START_OBJECT = 1;
    public static final int END_OBJECT = 2;
    public static final int START_ARRAY = 3;
    public static final int END_ARRAY = 4;
    public static final int COLON = 5;
    public static final int COMMA = 6;
    public static final int STRING = 7;
    public static final int NUMBER = 8;
    public static final int TRUE = 9;
    public static final int FALSE = 10;
    public static final int NULL = 11;
    public int tokenType;
    public String tokenText;
    public int line;
    public int charBegin;
    public int charEnd;
    public int column;

    JsonToken(int tokenType, String text, int line, int charBegin, int charEnd, int column) {
        this.tokenType = tokenType;
        this.tokenText = text;
        this.line = line;
        this.charBegin = charBegin;
        this.charEnd = charEnd;
        this.column = column;
    }

    @Override
    public String toString() {
        return "(token|" + tokenType + "|" + tokenText + ")";
    }
}
