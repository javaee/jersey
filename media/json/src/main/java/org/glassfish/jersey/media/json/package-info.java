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
/**
 * Jersey JSON provider classes.
 * <p>
 * Classes in this package provide support for enabling and configuring JSON.
 * Besides enabling JSON, the API also allows customization of the JSON format
 * produced and consumed with JAXB beans.
 * Such customization requires that an implementation of
 * {@link javax.ws.rs.ext.ContextResolver} returns a configured
 * {@link org.glassfish.jersey.media.json.JsonJaxbContext} instance.
 *
 * <p>For example, if the following two JAXB beans are defined:
 * <blockquote><pre>
 * <span style="font-weight:bold">&#064;XmlRootElement</span>
 * public class <span style="font-weight:bold">BeanOne</span> {
 *   public String name;
 *   public int number;
 * }
 *
 * <span style="font-weight:bold">&#064;XmlRootElement</span>
 * public class <span style="font-weight:bold">BeanTwo</span> {
 *   public List&lt;String&gt; titles;
 * }
 * </pre></blockquote>
 *
 * And the following resource class uses the above JAXB beans:
 *
 * <blockquote><pre>
 * &#064;Path("beans")
 * public class MyResource {
 *
 *   &#064;GET &#064;Path("one") &#064;Produces(MediaType.APPLICATION_JSON)
 *   public <span style="font-weight:bold">BeanOne</span> getOne() {
 *       BeanOne one = new BeanOne();
 *       one.name = "Howard";
 *       one.number = 3;
 *       return one;
 *   }
 *
 *   &#064;GET &#064;Path("two") &#064;Produces(MediaType.APPLICATION_JSON)
 *   public <span style="font-weight:bold">BeanTwo</span> getTwo() {
 *       BeanTwo two = new BeanTwo();
 *       two.titles = new ArrayList<String>(1){{add("Title1");}};
 *       return two;
 *   }
 * </pre></blockquote>
 *
 * <p>
 * Then, for the URI path <code>beans/one</code>, the following JSON will be
 * produced:
 * <blockquote><pre>
 * {"name":"Howard","number":<span style="color:red">"</span>3<span style="color:red">"</span>}
 * </pre></blockquote>
 * However, it might be required that the JSON object named <code>number</code> have
 * a non-String value <code>3</code>.
 * <p>
 * And, for the URI path <code>beans/two</code>, the following JSON will be
 * produced:
 * <blockquote><pre>
 * {"titles":"Title1"}
 * </pre></blockquote>
 * However, it might be required that the JSON object named <code>titles</code>
 * have a JSON array value <code><span style="color:red">[</span>"Title1"<span style="color:red">]</span></code>,
 * since the <code>titles</code> field on the JAXB bean <code>BeanTwo</code>
 * represents an array, thus enabling consuming of such JSON values the same way on
 * the client side no matter how many elements the array contains.
 *
 * <p>The {@link com.sun.jersey.api.json.JSONJAXBContext} may be configured to enable
 * such required production of JSON as described above in the following manner:
 * <blockquote><pre>
 * <span style="font-weight:bold">&#064;Provider</span>
 * public final class JaxbContextResolver <span style="font-weight:bold">implements ContextResolver&lt;JAXBContext&gt;</span> {
 *
 *   <span style="font-weight:bold">private final JAXBContext context;</span>
 *
 *   private final Set&lt;Class&gt; types;
 *
 *   private final Class[] cTypes = {BeanOne.class, BeanTwo.class};
 *
 *   public JaxbContextResolver() throws JAXBException {
 *       this.context = new <span style="font-weight:bold">JsonJaxbContext</span>(<span style="font-weight:bold">JsonConfiguration.natural().build()</span>, cTypes);
 *       this.types = new HashSet(Arrays.asList(cTypes));
 *   }
 *
 *   <span style="font-weight:bold">public JAXBContext getContext(Class&lt;?&gt; objectType)</span> {
 *       return (types.contains(objectType)) ? <span style="font-weight:bold">context</span> : null;
 *   }
 * }
 * </pre></blockquote>
 *
 * Then, the produced JSON would become: <code>{"name":"Howard","number":3}</code>
 * and <code>{"titles":["Title1"]}</code> respectively for the URI paths
 * <code>beans/one</code> and <code>beans/two</code>. Please note, that you do not need
 * to configure this in much detail. Using {@link org.glassfish.jersey.media.json.JsonConfiguration.Notation#NATURAL natural} notation
 * means that Jersey JSON processor will automatically take care about numbers, booleans and arrays. This notation
 * will probably become the default one for Jersey in one of the future releases.
 *
 * <p>For a complete set of supported properties, see
 * {@link org.glassfish.jersey.media.json.JsonConfiguration.Builder JsonConfiguration.Builder}.
 */
package org.glassfish.jersey.media.json;