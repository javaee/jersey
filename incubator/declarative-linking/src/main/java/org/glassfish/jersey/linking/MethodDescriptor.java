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
package org.glassfish.jersey.linking;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.security.AccessController;
import java.security.PrivilegedAction;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Utility class for working with class methods
 * 
 * @author Ryan Peterson
 */
class MethodDescriptor {

    protected Method getter;
    protected Method setter;

    MethodDescriptor(Class<?> entityClass, Method m) {
        this.getter = m;
        String setMethodName = null;
        if(m.getName().startsWith("get")) {
        	setMethodName = m.getName().replaceFirst("get", "set");
        } else if(m.getName().startsWith("is")) {
        	setMethodName = m.getName().replaceFirst("is", "set");
        } else {
            Logger.getLogger(MethodDescriptor.class.getName()).log(Level.FINE, "Unable to find corresponding setter for "+m.getName()+" in "+entityClass.getName());
        }
        if(setMethodName != null) {
	        try {
				this.setter = m.getDeclaringClass().getMethod(setMethodName, m.getReturnType());
			} catch (NoSuchMethodException | SecurityException e) {
	            Logger.getLogger(MethodDescriptor.class.getName()).log(Level.FINE, null, e);
			}
        }
        
    }

    public Object getMethodValue(Object instance) {
        setAccessibleMethod (getter);
        Object value = null;
        try {
            value = getter.invoke(instance);
        } catch (InvocationTargetException ex) {
            Logger.getLogger(MethodDescriptor.class.getName()).log(Level.FINE, null, ex);
        } catch (IllegalAccessException ex) {
            Logger.getLogger(MethodDescriptor.class.getName()).log(Level.FINE, null, ex);
        }
        return value;
    }

    public String getMethodName() {
        return getter.getName();
    }

    protected static void setAccessibleMethod(final Method m) {
        if (Modifier.isPublic(m.getModifiers()))
            return;

        AccessController.doPrivileged(new PrivilegedAction<Object>() {
            public Object run() {
                if (!m.isAccessible()) {
                    m.setAccessible(true);
                }
                return m;
            }
        });
    }

    @Override
    public boolean equals(Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final MethodDescriptor other = (MethodDescriptor) obj;
        if (this.getter != other.getter && (this.getter == null || !this.getter.equals(other.getter))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 83 * hash + (this.getter != null ? this.getter.hashCode() : 0);
        return hash;
    }
}
