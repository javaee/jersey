/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2007-2012 Oracle and/or its affiliates. All rights reserved.
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
%%
%class JsonLexer
%unicode
%line
%char
%column
%type JsonToken
%{
   StringBuffer string = new StringBuffer();
   public int getCharOffset() { return yychar; }
   public int getLineNumber() { return yyline; }
   public int getColumn() { return yycolumn; }
%}
%state STRING
WHITE_SPACE_CHAR=[\n\r\ \t\b\012]
NUMBER_TEXT=-?(0|[1-9][0-9]*)(\.[0-9]+)?([eE][+-]?[0-9]+)?
%%
<YYINITIAL> {
  "," { return (new JsonToken(JsonToken.COMMA, yytext(), yyline, yychar, yychar+1, yycolumn)); }
  ":" { return (new JsonToken(JsonToken.COLON, yytext(), yyline, yychar, yychar+1, yycolumn)); }
  "[" { return (new JsonToken(JsonToken.START_ARRAY, yytext(), yyline, yychar, yychar+1, yycolumn)); }
  "]" { return (new JsonToken(JsonToken.END_ARRAY, yytext(), yyline, yychar, yychar+1, yycolumn)); }
  "{" { return (new JsonToken(JsonToken.START_OBJECT, yytext(), yyline, yychar, yychar+1, yycolumn)); }
  "}" { return (new JsonToken(JsonToken.END_OBJECT, yytext(), yyline, yychar, yychar+1, yycolumn)); }
  "true" { return (new JsonToken(JsonToken.TRUE, yytext(), yyline, yychar, yychar+yylength(), yycolumn));}
  "false" { return (new JsonToken(JsonToken.FALSE, yytext(), yyline, yychar, yychar+yylength(), yycolumn));}
  "null" { return (new JsonToken(JsonToken.NULL, yytext(), yyline, yychar, yychar+yylength(), yycolumn));}
  \"  { string.setLength(0); yybegin(STRING); }
  {NUMBER_TEXT} { return (new JsonToken(JsonToken.NUMBER, yytext(), yyline, yychar, yychar+yylength(), yycolumn));}
  {WHITE_SPACE_CHAR} {}
  . { throw new JsonFormatException(yytext(), yyline, yycolumn, "Unexpected character: " + yytext() + " (line: " + (yyline + 1) + ", column: " + (yycolumn + 1) + ")"); }
}

<STRING> {
 \"  { yybegin(YYINITIAL);
       return (new JsonToken(JsonToken.STRING, string.toString(), yyline, yychar, yychar+string.length(), yycolumn));}
 [^\n\r\"\\]+   { string.append(yytext()); }
  \\\"          { string.append('\"'); }
  \\\\          { string.append('\\'); }
  \\\/           { string.append('/'); }
  \\b          { string.append('\b'); }
  \\f          { string.append('\f'); }
  \\n           { string.append('\n'); }
  \\r           { string.append('\r'); }
  \\t          { string.append('\t'); }
  \\u[0-9A-Fa-f]{4}    { string.append(Character.toChars(Integer.parseInt(yytext().substring(2),16))); }
  <<EOF>>       { throw new JsonFormatException(null, yyline, yycolumn, "Unexpected end of file."); }
}


