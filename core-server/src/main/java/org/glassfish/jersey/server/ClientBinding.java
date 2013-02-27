/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

import javax.ws.rs.core.Configuration;

/**
 * Meta-annotation that provides a facility for creating bindings between an {@link Uri &#64;Uri}-injectable
 * {@link javax.ws.rs.client.WebTarget WebTarget} instances and clients (and their configurations) that are used to create
 * the injected web target instances.
 * <p>
 * Jersey refers to client instance configured using custom bound configurations as <em>managed clients</em>. As a first step,
 * when using a managed client in a server-side JAX-RS/Jersey application, a custom client binding annotation has to be
 * defined:
 * <pre>
 * &#64;ClientBinding
 * public &#64;interface <b>MyClient</b> { }
 * </pre>
 * This defines new {@code @MyClient} binding annotation which will be configured using a default {@link #configClass()
 * configuration class}, will {@link #inheritServerProviders() inherit all server-side providers} as well as will use a
 * default {@link #baseUri() base URI} to resolve relative {@link Uri &#64;Uri} web target URI values.
 * </p>
 * <p>
 * Once a custom client binding annotation is defined, it can be used when {@link Uri injecting} new
 * {@code WebTarget} instances created by a managed client. To complete a binding between a manged client and an injected
 * web target instance, put the custom client binding annotation into the definition of an injected web target field or
 * parameter. For example:
 * <pre>
 * &#64;Path("foo")
 * public class ManagedClientResource {
 *   &#64;Uri("bar") <b>&#64;MyClient</b>
 *   private WebTarget targetBar;
 *
 *   &#64;GET
 *   &#64;Path("bar")
 *   public String getBar() {
 *     return targetBar.request(MediaType.TEXT_PLAIN).get(String.class);
 *   }
 *
 *   &#64;GET
 *   &#64;Path("baz")
 *   public Response getBaz(&#64;Uri("baz") <b>&#64;MyClient</b> WebTarget targetBaz) {
 *     return targetB.request(MediaType.TEXT_PLAIN).get();
 *   }
 * }
 * </pre>
 * </p>
 * <p>
 * Often managed clients may require a more complex configuration, including specifying custom provider classes and instances
 * and setting custom properties. In such case it may be more convenient to provide a custom {@link Configuration}
 * implementation class and link it with the binding annotation:
 * <pre>
 * public class <b>MyClientConfig</b> implements Configuration {
 *   ... // configure provide
 * }
 *
 * &#64;ClientBinding(configClass = <b>MyClientConfig</b>.class)
 * public &#64;interface <b>MyClient</b> { }
 * </pre>
 * Note that the easiest way how to provide a custom client-side {@code Configuration} implementation in Jersey is to extend
 * the {@link org.glassfish.jersey.client.ClientConfig} class that provides reusable implementation of JAX-RS
 * {@link Configuration} as well as {@link javax.ws.rs.core.Configurable Configurable} APIs.
 * </p>
 * <p>
 * In case a managed client needs special properties, these properties can also be provided via custom {@code Configuration}
 * implementation class. Another way how to pass custom properties to a managed client configuration is to define the managed
 * client properties in the server configuration using a special
 * <tt><em>&lt;client.binding.annotation.FQN&gt;</em>.property.</tt> prefix. This can be either done programmatically,
 * for example:
 * <pre>
 * MyResourceConfig.property(
 *     "my.package.MyClient.property.<b>custom-client-property</b>", "custom-value");
 * </pre>
 * </p>
 * <p>
 * Or declaratively via {@code web.xml}:
 * <pre>
 * &lt;init-param&gt;
 *     &lt;param-name&gt;my.package.MyClient.property.<b>custom-client-property</b>&lt;/param-name&gt;
 *     &lt;param-value&gt;custom-value&lt;/param-value&gt;
 * &lt;/init-param&gt;
 * </pre>
 * Properties defined this way can be accessed from the proper managed client instances using the custom property names:
 * <pre>
 * Object value = customTarget.getConfiguration().getProperty("<b>custom-client-property</b>");
 * </pre>
 * Note that the technique of defining managed client properties via server-side configuration described above can be also used
 * to override the default property values defined programmatically in a custom configuration implementation class.
 * </p>
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.ANNOTATION_TYPE)
@Documented
public @interface ClientBinding {
    /**
     * Define a configuration implementation class to be instantiated and used to configure bound web targets.
     * If not set, a default client-side configuration implementation class will be used.
     * <p>
     * Hard-coded value of this property may be overridden at deploy-time by providing a new value for a
     * <tt><em>&lt;client.binding.annotation.FQN&gt;</em>.configClass</tt> property.
     * </p>
     * <p>
     * For example:
     * <pre>
     * MyResourceConfig.property(
     *     "my.package.MyClient.configClass",
     *     "my.package.MyClientConfig");
     * </pre>
     * </p>
     * <p>
     * Or declaratively via {@code web.xml}:
     * <pre>
     * &lt;init-param&gt;
     *     &lt;param-name&gt;my.package.MyClient.configClass&lt;/param-name&gt;
     *     &lt;param-value&gt;my.package.MyClientConfig&lt;/param-value&gt;
     * &lt;/init-param&gt;
     * </pre>
     * </p>
     */
    public Class<? extends Configuration> configClass() default Configuration.class;

    /**
     * Determine whether providers present in the server-side configuration should be inherited by the bound client
     * configuration ({@code true}) or not ({@code false}). By default the server-side providers are inherited, i.e.
     * the annotation property defaults to {@code true}.
     * <p>
     * Hard-coded value of this property may be overridden at deploy-time by providing a new value for a
     * <tt><em>&lt;client.binding.annotation.FQN&gt;</em>.inheritServerProviders</tt> property.
     * </p>
     * <p>
     * For example:
     * <pre>
     * MyResourceConfig.property(
     *     "my.package.MyClient.inheritServerProviders", false);
     * </pre>
     * </p>
     * <p>
     * Or declaratively via {@code web.xml}:
     * <pre>
     * &lt;init-param&gt;
     *     &lt;param-name&gt;my.package.MyClient.inheritServerProviders&lt;/param-name&gt;
     *     &lt;param-value&gt;false&lt;/param-value&gt;
     * &lt;/init-param&gt;
     * </pre>
     * </p>
     */
    public boolean inheritServerProviders() default true;

    /**
     * Define a custom base URI for managed {@link javax.ws.rs.client.WebTarget WebTarget} instances injected using
     * {@link Uri &#64;Uri} annotation with a relative web target URI value. By default, the base
     * URI is empty indicating that the current application base URI should be used.
     * <p>
     * Using a custom context root is useful in cases where the absolute URI of the target endpoint(s) is expected
     * to change on may vary over time. An typical scenarios include transition of the application from a test to
     * production environment, etc.
     * </p>
     * <p>
     * Hard-coded value of this property may be overridden at deploy-time by providing a new value for a
     * <tt><em>&lt;client.binding.annotation.FQN&gt;</em>.baseUri</tt> property.
     * </p>
     * <p>
     * For example:
     * <pre>
     * MyResourceConfig.property(
     *     "my.package.MyClient.baseUri", "http://jersey.java.net/examples/");
     * </pre>
     * </p>
     * <p>
     * Or declaratively via {@code web.xml}:
     * <pre>
     * &lt;init-param&gt;
     *     &lt;param-name&gt;my.package.MyClient.baseUri&lt;/param-name&gt;
     *     &lt;param-value&gt;http://jersey.java.net/examples/&lt;/param-value&gt;
     * &lt;/init-param&gt;
     * </pre>
     * </p>
     */
    public String baseUri() default "";
}
