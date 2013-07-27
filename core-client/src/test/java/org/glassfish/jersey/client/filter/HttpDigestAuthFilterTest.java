/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.client.filter;

import java.io.IOException;

import java.util.Arrays;
import org.glassfish.jersey.client.filter.HttpDigestAuthFilter.DigestScheme;

import org.junit.Test;
import org.junit.Assert;

/**
 * @author raphael.jolivet@gmail.com
 * @author Stefan Katerkamp (stefan@katerkamp.de
 */
public class HttpDigestAuthFilterTest {

	@Test
	public void testParseHeaders1() throws IOException // No "digest" scheme
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		DigestScheme ds = f.parseAuthHeaders(Arrays.asList(new String[]{
			"basic toto=tutu",
			"basic toto=\"tutu\""
		}));

		Assert.assertNull(ds);
	}

	@Test
	public void testParseHeaders2() throws IOException // Two concurrent schemes
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		DigestScheme ds = f.parseAuthHeaders(Arrays.asList(new String[]{
			"Digest realm=\"tata\"",
			"basic  toto=\"tutu\""
		}));
		Assert.assertNotNull(ds);

		Assert.assertEquals("tata", ds.getRealm());
	}

	@Test
	public void testParseHeaders3() throws IOException // Complex case, with comma inside value
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		DigestScheme ds = f.parseAuthHeaders(Arrays.asList(new String[]{
			"digest realm=\"tata\",nonce=\"foo, bar\""
		}));

		Assert.assertNotNull(ds);
		Assert.assertEquals("tata", ds.getRealm());
		Assert.assertEquals("foo, bar", ds.getNonce());
	}

	@Test
	public void testParseHeaders4() throws IOException // Spaces
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		DigestScheme ds = f.parseAuthHeaders(Arrays.asList(new String[]{
			"    digest realm =   \"tata\"  ,  opaque=\"bar\" ,nonce=\"foo, bar\""
		}));

		Assert.assertNotNull(ds);
		Assert.assertEquals("tata", ds.getRealm());
		Assert.assertEquals("foo, bar", ds.getNonce());
		Assert.assertEquals("bar", ds.getOpaque());
	}

	@Test
	public void testParseHeaders5() throws IOException // Mix of quotes and  non-quotes
	{
		HttpDigestAuthFilter f = new HttpDigestAuthFilter("foo", "bar");
		DigestScheme ds = f.parseAuthHeaders(Arrays.asList(new String[]{
			"    digest realm =   \"tata\"  ,  opaque =bar ,nonce=\"foo, bar\""
		}));

		Assert.assertNotNull(ds);
		Assert.assertEquals("tata", ds.getRealm());
		Assert.assertEquals("foo, bar", ds.getNonce());
		Assert.assertEquals("bar", ds.getOpaque());
	}
}
