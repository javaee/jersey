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
