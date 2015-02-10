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

package org.glassfish.jersey.client.oauth2;

/**
 * Class that provides methods to build {@link OAuth2CodeGrantFlow} pre-configured for usage
 * with Google provider.
 *
 * @author Miroslav Fuksa
 * @since 2.3
 */
public class OAuth2FlowGoogleBuilder extends AbstractAuthorizationCodeGrantBuilder<OAuth2FlowGoogleBuilder> {

    /**
     * Create a new google flow builder.
     */
    OAuth2FlowGoogleBuilder() {
        super(new AuthCodeGrantImpl.Builder());
    }

    /**
     * Set {@code access type} parameter used in Authorization Request.
     * @param accessType access type value.
     * @return a google authorization flow builder.
     */
    public OAuth2FlowGoogleBuilder accessType(AccessType accessType) {
        return property(OAuth2CodeGrantFlow.Phase.AUTHORIZATION, AccessType.getKey(), accessType.getValue());
    }

    /**
     * Set {@code prompt} parameter used in Authorization Request.
     * @param prompt Prompt value.
     * @return a google authorization flow builder.
     */
    public OAuth2FlowGoogleBuilder prompt(Prompt prompt) {
        return property(OAuth2CodeGrantFlow.Phase.AUTHORIZATION, Prompt.getKey(), prompt.getValue());
    }

    /**
     * Set {@code display} parameter used in Authorization Request.
     * @param display display value.
     * @return a google authorization flow builder.
     */
    public OAuth2FlowGoogleBuilder display(Display display) {
        return property(OAuth2CodeGrantFlow.Phase.AUTHORIZATION, Display.getKey(), display.getValue());
    }

    /**
     * Set {@code login hint} parameter used in Authorization Request.
     * @param loginHint login hint value.
     * @return a google authorization flow builder.
     */
    public OAuth2FlowGoogleBuilder loginHint(String loginHint) {
        return property(OAuth2CodeGrantFlow.Phase.AUTHORIZATION, Display.getKey(), loginHint);
    }

    /**
     * Enum that defines values for "access_type" parameter used in
     * Google OAuth flow. Defines whether the offline access is allowed (without
     * user active session).
     */
    public static enum AccessType {
        ONLINE("online"),
        OFFLINE("offline");

        private final String propertyValue;

        private AccessType(String propertyValue) {
            this.propertyValue = propertyValue;
        }

        public String getValue() {
            return propertyValue;
        }

        public static String getKey() {
            return "access_type";
        }
    }

    /**
     * Enum that defines values for "prompt" parameter used in
     * Google OAuth flow.
     */
    public static enum Prompt {

        NONE("none"),
        /**
         * User will be asked for approval each time the authorization is performed.
         */
        CONSENT("consent"),
        SELECT_ACCOUNT("select_account");

        private final String value;

        private Prompt(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static String getKey() {
            return "prompt";
        }

    }

    /**
     * Enum that defines values for "display" parameter used in
     * Google OAuth flow.
     */
    public static enum Display {
        PAGE("page"),
        POPUP("popup"),
        TOUCH("touch"),
        WAP("wap");

        private final String value;

        private Display(String value) {
            this.value = value;
        }

        public String getValue() {
            return value;
        }

        public static String getKey() {
            return "display";
        }
    }

    /**
     * Property key that defines values for "login_hint" parameter used in
     * Google OAuth flow.
     */
    public static final String LOGIN_HINT = "login_hint";

}
