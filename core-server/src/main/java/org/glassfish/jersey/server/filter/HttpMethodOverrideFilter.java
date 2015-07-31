/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2015 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.server.filter;

import java.util.List;
import java.util.Map;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.BadRequestException;
import javax.ws.rs.Priorities;
import javax.ws.rs.container.ContainerRequestContext;
import javax.ws.rs.container.ContainerRequestFilter;
import javax.ws.rs.container.PreMatching;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.Form;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import javax.ws.rs.core.UriBuilder;

import javax.annotation.Priority;

import org.glassfish.jersey.internal.util.Tokenizer;
import org.glassfish.jersey.server.ContainerRequest;
import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.server.internal.LocalizationMessages;

import static org.glassfish.jersey.internal.util.Tokenizer.COMMON_DELIMITERS;

/**
 * A pre-matching filter to support HTTP method replacing of a POST request to a request
 * utilizing another HTTP method for the case where proxies or HTTP
 * servers would otherwise block that HTTP method.
 * <p>
 * This filter may be used to replace a POST request with a PUT, DELETE or GET
 * request.
 * <p>
 * Replacement will occur if the request method is POST and there exists either
 * a request header "X-HTTP-Method-Override", or
 * a query parameter "_method" with a non-empty value. That value
 * will be the HTTP method that replaces the POST method. In addition to that,
 * when replacing the POST method with GET, the filter will convert the form parameters
 * to query parameters. If the filter is configured to look for both the X-HTTP-Method-Override
 * header as well as the _method query parameter (the default setting), both are present in the
 * request and they differ, the filter returns {@link javax.ws.rs.core.Response.Status#BAD_REQUEST} response.
 * </p>
 * <p>
 * The filter behavior can be configured using {@link org.glassfish.jersey.server.ServerProperties#HTTP_METHOD_OVERRIDE}
 * property.
 * </p>
 *
 * @author Paul Sandoz
 * @author Martin Matula
 * @author Fredy Nagy
 * @author Florian Hars (florian at hars.de)
 */
@PreMatching
@Priority(Priorities.HEADER_DECORATOR + 50) // must go after UriConnegFilter (if present)
public final class HttpMethodOverrideFilter implements ContainerRequestFilter {

    /**
     * Configuration flags.
     * <p/>
     * Package-private for testing purposes.
     */
    final int config;

    /**
     * Enumeration representing possible sources of information about the method overriding
     * the filter should look for.
     */
    public enum Source {

        /**
         * If present in the filter configuration,
         * causes the filter to look for a method override in the X-HTTP-Method-Override header.
         */
        HEADER(1),
        /**
         * If present in the filter configuration,
         * causes the filter to look for a method override in the _method query parameter.
         */
        QUERY(2);
        private final int flag;

        Source(final int flag) {
            this.flag = flag;
        }

        /**
         * Returns the numeric value of the bit corresponding to this flag.
         *
         * @return numeric value of this flag.
         */
        public int getFlag() {
            return flag;
        }

        /**
         * Returns {@code true} if the bit corresponding to this flag is set in a given integer value.
         *
         * @param config integer value to check for the bit corresponding to this flag.
         * @return {@code true} if the passed value has the bit corresponding to this flag set.
         */
        public boolean isPresentIn(final int config) {
            return (config & flag) == flag;
        }
    }

    /**
     * Registers this filter into the passed {@link ResourceConfig} instance and
     * configures it.
     *
     * @param rc      Resource config.
     * @param sources Sources of method override information. If empty, both
     *                {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source#HEADER} and
     *                {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source#QUERY} will
     *                be added to the config by default.
     */
    public static void enableFor(final ResourceConfig rc, final Source... sources) {
        rc.registerClasses(HttpMethodOverrideFilter.class);
        rc.property(ServerProperties.HTTP_METHOD_OVERRIDE, sources);
    }

    /**
     * Create a filter that reads the configuration ({@link ServerProperties#HTTP_METHOD_OVERRIDE})
     * from the provided {@link org.glassfish.jersey.server.ResourceConfig} instance.
     * <p/>
     * This constructor will be called by the Jersey runtime when the filter class is returned from
     * {@link javax.ws.rs.core.Application#getClasses()}. The {@link org.glassfish.jersey.server.ResourceConfig}
     * instance will get auto-injected.
     *
     * @param rc ResourceConfig instance that holds the configuration for the filter.
     */
    public HttpMethodOverrideFilter(@Context final Configuration rc) {
        this(parseConfig(rc.getProperty(ServerProperties.HTTP_METHOD_OVERRIDE)));
    }

    /**
     * Initializes this filter setting the sources of information the filter should look for.
     *
     * @param sources Sources of method override information. If empty, both
     *                {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source#HEADER} and
     *                {@link org.glassfish.jersey.server.filter.HttpMethodOverrideFilter.Source#QUERY} will
     *                be added to the config by default.
     */
    public HttpMethodOverrideFilter(final Source... sources) {
        int c = 0;
        for (final Source cf : sources) {
            if (cf != null) {
                c |= cf.getFlag();
            }
        }
        if (c == 0) {
            c = 3;
        }
        this.config = c;
    }

    /**
     * Converts configuration property value to an array of {@link Source} literals.
     *
     * @param config {@link ServerProperties#HTTP_METHOD_OVERRIDE configuration property} value
     * @return array of {@code Source} objects.
     */
    private static Source[] parseConfig(final Object config) {
        if (config == null) {
            return new Source[0];
        }

        if (config instanceof Source[]) {
            return (Source[]) config;
        } else if (config instanceof Source) {
            return new Source[] {(Source) config};
        } else {
            final String[] stringValues;
            if (config instanceof String) {
                stringValues = Tokenizer.tokenize((String) config, COMMON_DELIMITERS);
            } else if (config instanceof String[]) {
                stringValues = Tokenizer.tokenize((String[]) config, COMMON_DELIMITERS);
            } else {
                return new Source[0];
            }

            final Source[] result = new Source[stringValues.length];
            for (int i = 0; i < stringValues.length; i++) {
                try {
                    result[i] = Source.valueOf(stringValues[i]);
                } catch (final IllegalArgumentException e) {
                    Logger.getLogger(HttpMethodOverrideFilter.class.getName()).log(Level.WARNING,
                            LocalizationMessages.INVALID_CONFIG_PROPERTY_VALUE(ServerProperties.HTTP_METHOD_OVERRIDE,
                                    stringValues[i]));
                }
            }
            return result;
        }
    }

    /**
     * Returns parameter value in a normalized form (uppercase, trimmed and {@code null} if empty string)
     * considering the config flags.
     *
     * @param source    Config flag to look for (if set in the config, this method returns the param value,
     *                  if not set, this method returns {@code null}).
     * @param paramsMap Map to retrieve the parameter from.
     * @param paramName Name of the parameter to retrieve.
     * @return Normalized parameter value. Never returns an empty string - converts it to {@code null}.
     */
    private String getParamValue(final Source source, final MultivaluedMap<String, String> paramsMap, final String paramName) {
        String value = source.isPresentIn(config) ? paramsMap.getFirst(paramName) : null;
        if (value == null) {
            return null;
        }
        value = value.trim();
        return value.length() == 0 ? null : value.toUpperCase();
    }

    @Override
    public void filter(final ContainerRequestContext request) {
        if (!request.getMethod().equalsIgnoreCase("POST")) {
            return;
        }

        final String header = getParamValue(Source.HEADER, request.getHeaders(), "X-HTTP-Method-Override");
        final String query = getParamValue(Source.QUERY, request.getUriInfo().getQueryParameters(), "_method");

        final String override;
        if (header == null) {
            override = query;
        } else {
            override = header;
            if (query != null && !query.equals(header)) {
                // inconsistent query and header param values
                throw new BadRequestException();
            }
        }

        if (override != null) {
            request.setMethod(override);
            if (override.equals("GET")) {
                if (request.getMediaType() != null
                        && MediaType.APPLICATION_FORM_URLENCODED_TYPE.getType().equals(request.getMediaType().getType())) {
                    final UriBuilder ub = request.getUriInfo().getRequestUriBuilder();
                    final Form f = ((ContainerRequest) request).readEntity(Form.class);
                    for (final Map.Entry<String, List<String>> param : f.asMap().entrySet()) {
                        ub.queryParam(param.getKey(), param.getValue().toArray());
                    }
                    request.setRequestUri(request.getUriInfo().getBaseUri(), ub.build());
                }
            }
        }
    }
}
