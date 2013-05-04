/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2011 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.tests.spring.springmanaged;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import javax.ws.rs.client.Invocation.Builder;

import org.glassfish.jersey.test.JerseyTest;
import org.glassfish.jersey.test.simple.SimpleTestContainerFactory;
import org.glassfish.jersey.tests.spring.Item;
import org.glassfish.jersey.tests.spring.TestData;


/**
 * Test spring managed singleton resources having at least one constructor.<br>
 * Created on: Apr 10, 2008<br>
 * 
 * @author <a href="mailto:martin.grotzke@freiheit.com">Martin Grotzke</a>
 * @author @author Marko Asplund (marko.asplund at gmail.com)
 * @version $Id$
 */
public class SpringManagedSingletonResourceTestBase extends JerseyTest {
    
    private final String _resourcePath;

    public SpringManagedSingletonResourceTestBase( String resourcePath ) {
        super(new SimpleTestContainerFactory());
        _resourcePath = resourcePath;
    }
    
    public void testGetManagedName() {
        final String actual = target().path(_resourcePath).request().get().readEntity(String.class);
        assertNotNull( actual );
        assertEquals( actual, TestData.MANAGED );
    }
    
    public void testGetAndUpdateManagedItem() {
        
        Builder itemResource = target().path( _resourcePath + "/item").request();
        final Item actualItem =  itemResource.get().readEntity(Item.class);
        assertNotNull( actualItem );
        assertEquals( actualItem.getValue(), TestData.MANAGED );

        /* update the value of the singleton item and afterwards check if it's the same
         */
        final String newValue = "newValue";
        final Builder itemValueResource = target().path( _resourcePath + "/item/value/" + newValue ).request();
        itemValueResource.put(null);
        
        final Item actualUpdatedItem = itemResource.get( Item.class );
        assertNotNull( actualUpdatedItem );
        assertEquals( actualUpdatedItem.getValue(), newValue );
        
    }
    
    public void testGetAndUpdateCount() {
        
        final Builder countResource = target().path( _resourcePath + "/countusage" ).request();
        
        final int actualCount = Integer.parseInt( countResource.get( String.class ) );
        countResource.post(null);
        final int actualCountUpdated = Integer.parseInt( countResource.get( String.class ) );
        assertEquals( actualCountUpdated, actualCount + 1 );
        
    }
    
}
