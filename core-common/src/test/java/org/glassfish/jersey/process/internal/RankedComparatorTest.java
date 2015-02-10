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
package org.glassfish.jersey.process.internal;

import java.util.Collections;
import java.util.List;

import javax.annotation.Priority;

import org.glassfish.jersey.model.internal.RankedComparator;
import org.glassfish.jersey.model.internal.RankedProvider;

import org.junit.Test;
import static org.junit.Assert.assertTrue;

import jersey.repackaged.com.google.common.collect.Lists;

/**
 * Tests {@link org.glassfish.jersey.model.internal.RankedComparator}.
 * @author Miroslav Fuksa
 *
 */
public class RankedComparatorTest {

    @Test
    public void testPriorityComparator() {
        List<RankedProvider<Object>> list = Lists.newLinkedList();
        list.add(new RankedProvider<Object>(new F1000()));
        list.add(new RankedProvider<Object>(new FF200()));
        list.add(new RankedProvider<Object>(new F0()));
        list.add(new RankedProvider<Object>(new F0()));
        list.add(new RankedProvider<Object>(new F300()));
        list.add(new RankedProvider<Object>(new F1000()));
        list.add(new RankedProvider<Object>(new F100()));
        list.add(new RankedProvider<Object>(new F200()));
        list.add(new RankedProvider<Object>(new F200()));
        list.add(new RankedProvider<Object>(new F_INT_MIN()));
        list.add(new RankedProvider<Object>(new F_INT_MAX()));
        Collections.sort(list, new RankedComparator<Object>(RankedComparator.Order.ASCENDING));
        int max = Integer.MIN_VALUE;
        for (RankedProvider<Object> o : list) {
            int val = o.getRank();
            assertTrue(val >= max);
            max = val;
        }

        Collections.sort(list, new RankedComparator<Object>(RankedComparator.Order.DESCENDING));
        max = Integer.MAX_VALUE;
        for (RankedProvider<Object> o : list) {
            int val = o.getRank();
            assertTrue(val <= max);
            max = val;
        }
    }

    @Priority(0)
    private static class F0 {
    }

    @Priority(100)
    private static class F100 {
    }

    @Priority(200)
    private static class F200 {
    }

    @Priority(200)
    private static class FF200 {
    }

    @Priority(300)
    private static class F300 {
    }

    @Priority(1000)
    private static class F1000 {
    }

    @Priority(Integer.MIN_VALUE)
    private static class F_INT_MIN {
    }

    @Priority(Integer.MAX_VALUE)
    private static class F_INT_MAX {
    }
}
