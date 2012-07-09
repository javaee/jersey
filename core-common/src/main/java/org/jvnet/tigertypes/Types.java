/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 1997-2010 Oracle and/or its affiliates. All rights reserved.
 *
 * The contents of this file are subject to the terms of either the GNU
 * General Public License Version 2 only ("GPL") or the Common Development
 * and Distribution License("CDDL") (collectively, the "License").  You
 * may not use this file except in compliance with the License.  You can
 * obtain a copy of the License at
 * https://glassfish.dev.java.net/public/CDDL+GPL_1_1.html
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

package org.jvnet.tigertypes;

import java.lang.reflect.Array;
import java.lang.reflect.GenericArrayType;
import java.lang.reflect.GenericDeclaration;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.lang.reflect.TypeVariable;
import java.lang.reflect.WildcardType;

/**
 * Type arithmetic functions.
 *
 * @author Kohsuke Kawaguchi
 */
public class Types {
    private static final TypeVisitor<Type,Class> baseClassFinder = new TypeVisitor<Type,Class>() {
        public Type onClass(Class c, Class sup) {
            // t is a raw type
            if(sup==c)
                return sup;

            Type r;

            Type sc = c.getGenericSuperclass();
            if(sc!=null) {
                r = visit(sc,sup);
                if(r!=null)     return r;
            }

            for( Type i : c.getGenericInterfaces() ) {
                r = visit(i,sup);
                if(r!=null)  return r;
            }

            return null;
        }

        public Type onParameterizdType(ParameterizedType p, Class sup) {
            Class raw = (Class) p.getRawType();
            if(raw==sup) {
                // p is of the form sup<...>
                return p;
            } else {
                // recursively visit super class/interfaces
                Type r = raw.getGenericSuperclass();
                if(r!=null)
                    r = visit(bind(r,raw,p),sup);
                if(r!=null)
                    return r;
                for( Type i : raw.getGenericInterfaces() ) {
                    r = visit(bind(i,raw,p),sup);
                    if(r!=null)  return r;
                }
                return null;
            }
        }

        public Type onGenericArray(GenericArrayType g, Class sup) {
            // not clear what I should do here
            return null;
        }

        public Type onVariable(TypeVariable v, Class sup) {
            return visit(v.getBounds()[0],sup);
        }

        public Type onWildcard(WildcardType w, Class sup) {
            // not clear what I should do here
            return null;
        }

        /**
         * Replaces the type variables in {@code t} by its actual arguments.
         *
         * @param decl
         *      provides a list of type variables. See {@link GenericDeclaration#getTypeParameters()}
         * @param args
         *      actual arguments. See {@link ParameterizedType#getActualTypeArguments()}
         */
        private Type bind( Type t, GenericDeclaration decl, ParameterizedType args ) {
            return binder.visit(t,new BinderArg(decl,args.getActualTypeArguments()));
        }
    };

    private static class BinderArg {
        final TypeVariable[] params;
        final Type[] args;

        BinderArg(TypeVariable[] params, Type[] args) {
            this.params = params;
            this.args = args;
            assert params.length==args.length;
        }

        public BinderArg( GenericDeclaration decl, Type[] args ) {
            this(decl.getTypeParameters(),args);
        }

        Type replace( TypeVariable v ) {
            for(int i=0; i<params.length; i++)
                if(params[i].equals(v))
                    return args[i];
            return v;   // this is a free variable
        }
    }
    private static final TypeVisitor<Type,BinderArg> binder = new TypeVisitor<Type,BinderArg>() {
        public Type onClass(Class c, BinderArg args) {
            return c;
        }

        public Type onParameterizdType(ParameterizedType p, BinderArg args) {
            Type[] params = p.getActualTypeArguments();

            boolean different = false;
            for( int i=0; i<params.length; i++ ) {
                Type t = params[i];
                params[i] = visit(t,args);
                different |= t!=params[i];
            }

            Type newOwner = p.getOwnerType();
            if(newOwner!=null)
                newOwner = visit(newOwner,args);
            different |= p.getOwnerType()!=newOwner;

            if(!different)  return p;

            return new ParameterizedTypeImpl( (Class<?>)p.getRawType(), params, newOwner );
        }

        public Type onGenericArray(GenericArrayType g, BinderArg types) {
            Type c = visit(g.getGenericComponentType(),types);
            if(c==g.getGenericComponentType())  return g;

            return new GenericArrayTypeImpl(c);
        }

        public Type onVariable(TypeVariable v, BinderArg types) {
            return types.replace(v);
        }

        public Type onWildcard(WildcardType w, BinderArg types) {
            // TODO: this is probably still incorrect
            // bind( "? extends T" ) with T= "? extends Foo" should be "? extends Foo",
            // not "? extends (? extends Foo)"
            Type[] lb = w.getLowerBounds();
            Type[] ub = w.getUpperBounds();
            boolean diff = false;

            for( int i=0; i<lb.length; i++ ) {
                Type t = lb[i];
                lb[i] = visit(t,types);
                diff |= (t!=lb[i]);
            }

            for( int i=0; i<ub.length; i++ ) {
                Type t = ub[i];
                ub[i] = visit(t,types);
                diff |= (t!=ub[i]);
            }

            if(!diff)       return w;

            return new WildcardTypeImpl(lb,ub);
        }
    };

    /**
     * Gets the parameterization of the given base type.
     *
     * <p>
     * For example, given the following
     * <pre><xmp>
     * interface Foo<T> extends List<List<T>> {}
     * interface Bar extends Foo<String> {}
     * </xmp></pre>
     * This method works like this:
     * <pre><xmp>
     * getBaseClass( Bar, List ) = List<List<String>
     * getBaseClass( Bar, Foo  ) = Foo<String>
     * getBaseClass( Foo<? extends Number>, Collection ) = Collection<List<? extends Number>>
     * getBaseClass( ArrayList<? extends BigInteger>, List ) = List<? extends BigInteger>
     * </xmp></pre>
     *
     * @param type
     *      The type that derives from {@code baseType}
     * @param baseType
     *      The class whose parameterization we are interested in.
     * @return
     *      The use of {@code baseType} in {@code type}.
     *      or null if the type is not assignable to the base type.
     */
    public static Type getBaseClass(Type type, Class baseType) {
        return baseClassFinder.visit(type,baseType);
    }

    /**
     * Gets the display name of the type object
     *
     * @return
     *      a human-readable name that the type represents.
     */
    public static String getTypeName(Type type) {
        if (type instanceof Class) {
            Class c = (Class) type;
            if(c.isArray())
                return getTypeName(c.getComponentType())+"[]";
            return c.getName();
        }
        return type.toString();
    }

    /**
     * Checks if {@code sub} is a sub-type of {@code sup}.
     */
    public static boolean isSubClassOf(Type sub, Type sup) {
        return erasure(sup).isAssignableFrom(erasure(sub));
    }


    /**
     * Implements the logic for {@link #erasure(Type)}.
     */
    private static final TypeVisitor<Class,Void> eraser = new TypeVisitor<Class,Void>() {
        public Class onClass(Class c,Void _) {
            return c;
        }

        public Class onParameterizdType(ParameterizedType p,Void _) {
            // TODO: why getRawType returns Type? not Class?
            return visit(p.getRawType(),null);
        }

        public Class onGenericArray(GenericArrayType g,Void _) {
            return Array.newInstance(
                    visit(g.getGenericComponentType(),null),
                    0 ).getClass();
        }

        public Class onVariable(TypeVariable v,Void _) {
            return visit(v.getBounds()[0],null);
        }

        public Class onWildcard(WildcardType w,Void _) {
            return visit(w.getUpperBounds()[0],null);
        }
    };

    /**
     * Returns the {@link Class} representation of the given type.
     *
     * This corresponds to the notion of the erasure in JSR-14.
     *
     * <p>
     * It made me realize how difficult it is to define the common navigation
     * layer for two different underlying reflection library. The other way
     * is to throw away the entire parameterization and go to the wrapper approach.
     */
    public static <T> Class<T> erasure(Type t) {
        return eraser.visit(t,null);
    }

    /**
     * Returns the {@link Type} object that represents {@code clazz&lt;T1,T2,T3>}.
     */
    public static ParameterizedType createParameterizedType( Class rawType, Type... arguments ) {
        return new ParameterizedTypeImpl(rawType,arguments,null);
    }

    /**
     * Checks if the type is an array type.
     */
    public static boolean isArray(Type t) {
        if (t instanceof Class) {
            Class c = (Class) t;
            return c.isArray();
        }
        if(t instanceof GenericArrayType)
            return true;
        return false;
    }

    /**
     * Checks if the type is an array type but not byte[].
     */
    public static boolean isArrayButNotByteArray(Type t) {
        if (t instanceof Class) {
            Class c = (Class) t;
            return c.isArray() && c!=byte[].class;
        }
        if(t instanceof GenericArrayType) {
            t = ((GenericArrayType)t).getGenericComponentType();
            return t!=Byte.TYPE;
        }
        return false;
    }

    /**
     * Gets the component type of the array.
     *
     * @param t
     *      must be an array.
     */
    public static Type getComponentType(Type t) {
        if (t instanceof Class) {
            Class c = (Class) t;
            return c.getComponentType();
        }
        if(t instanceof GenericArrayType)
            return ((GenericArrayType)t).getGenericComponentType();

        throw new IllegalArgumentException();
    }

    /**
     * Gets the i-th type argument from a parameterized type.
     *
     * <p>
     * Unlike {@link #getTypeArgument(Type, int, Type)}, this method
     * throws {@link IllegalArgumentException} if the given type is
     * not parameterized.
     */
    public static Type getTypeArgument(Type type, int i) {
        Type r = getTypeArgument(type, i, null);
        if(r==null)
            throw new IllegalArgumentException();
        return r;
    }

    /**
     * Gets the i-th type argument from a parameterized type.
     *
     * <p>
     * For example, {@code getTypeArgument([Map<Integer,String>],0)=Integer}
     * If the given type is not a parameterized type, returns the specified
     * default value.
     *
     * <p>
     * This is convenient for handling raw types and parameterized types uniformly.
     *
     * @throws IndexOutOfBoundsException
     *      If i is out of range.
     */
    public static Type getTypeArgument(Type type, int i, Type defaultValue) {
        if (type instanceof ParameterizedType) {
            ParameterizedType p = (ParameterizedType) type;
            return fix(p.getActualTypeArguments()[i]);
        } else
            return defaultValue;
    }

    /**
     * Checks if the given type is a primitive type.
     */
    public static boolean isPrimitive(Type type) {
        if (type instanceof Class) {
            Class c = (Class) type;
            return c.isPrimitive();
        }
        return false;
    }

    public static boolean isOverriding(Method method, Class base) {
        // this isn't actually correct,
        // as the JLS considers
        // class Derived extends Base<Integer> {
        //   Integer getX() { ... }
        // }
        // class Base<T> {
        //   T getX() { ... }
        // }
        // to be overrided. Handling this correctly needs a careful implementation

        String name = method.getName();
        Class[] params = method.getParameterTypes();

        while(base!=null) {
            try {
                if(base.getDeclaredMethod(name,params)!=null)
                    return true;
            } catch (NoSuchMethodException e) {
                // recursively go into the base class
            }

            base = base.getSuperclass();
        }

        return false;
    }

    /**
     * JDK 5.0 has a bug of createing {@link GenericArrayType} where it shouldn't.
     * fix that manually to work around the problem.
     *
     * See bug 6202725.
     */
    private static Type fix(Type t) {
        if(!(t instanceof GenericArrayType))
            return t;

        GenericArrayType gat = (GenericArrayType) t;
        if(gat.getGenericComponentType() instanceof Class) {
            Class c = (Class) gat.getGenericComponentType();
            return Array.newInstance(c,0).getClass();
        }

        return t;
    }

}
