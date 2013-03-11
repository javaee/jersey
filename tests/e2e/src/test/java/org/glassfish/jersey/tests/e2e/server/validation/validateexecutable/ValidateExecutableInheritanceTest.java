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

package org.glassfish.jersey.tests.e2e.server.validation.validateexecutable;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateExecutable;

import org.glassfish.jersey.server.ResourceConfig;

import org.junit.Test;

/**
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class ValidateExecutableInheritanceTest extends ValidateExecutableBasicTest {

    /**
     * On METHOD.
     */

    /**
     * {@link ValidateExecutable} annotations from this interface should be considered during validating phase.
     */
    @SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
    public static interface ValidateExecutableOnMethodsValidation {

        @Min(0)
        @ValidateExecutable
        public Integer validateExecutableDefault(@Max(10) final Integer value);

        @Min(0)
        @ValidateExecutable(ExecutableType.NON_GETTER_METHODS)
        public Integer validateExecutableMatch(@Max(10) final Integer value);

        @Min(0)
        @ValidateExecutable(ExecutableType.CONSTRUCTORS)
        public Integer validateExecutableMiss(@Max(10) final Integer value);

        @Min(0)
        @ValidateExecutable(ExecutableType.NONE)
        public Integer validateExecutableNone(@Max(10) final Integer value);
    }

    @ValidateExecutable(ExecutableType.ALL)
    public static interface ValidateExecutableOnMethodsJaxRs extends ValidateExecutableOnMethodsValidation {

        @POST
        @Path("validateExecutableDefault")
        @ValidateExecutable(ExecutableType.CONSTRUCTORS)
        Integer validateExecutableDefault(final Integer value);

        @POST
        @Path("validateExecutableMatch")
        @ValidateExecutable(ExecutableType.GETTER_METHODS)
        Integer validateExecutableMatch(final Integer value);

        @POST
        @Path("validateExecutableMiss")
        @ValidateExecutable(ExecutableType.NON_GETTER_METHODS)
        Integer validateExecutableMiss(final Integer value);

        @POST
        @Path("validateExecutableNone")
        @ValidateExecutable(ExecutableType.ALL)
        Integer validateExecutableNone(final Integer value);
    }

    public static abstract class ValidateExecutableOnMethodsAbstractResource implements ValidateExecutableOnMethodsJaxRs {

        @ValidateExecutable(ExecutableType.NONE)
        public abstract Integer validateExecutableDefault(final Integer value);

        @ValidateExecutable(ExecutableType.CONSTRUCTORS)
        public abstract Integer validateExecutableMatch(final Integer value);

        @ValidateExecutable(ExecutableType.ALL)
        public abstract Integer validateExecutableMiss(final Integer value);

        @ValidateExecutable(ExecutableType.NON_GETTER_METHODS)
        public abstract Integer validateExecutableNone(final Integer value);
    }

    @Path("on-method")
    public static class ValidateExecutableOnMethodsResource extends ValidateExecutableOnMethodsAbstractResource {

        public Integer validateExecutableDefault(final Integer value) {
            return value;
        }

        public Integer validateExecutableMatch(final Integer value) {
            return value;
        }

        public Integer validateExecutableMiss(final Integer value) {
            return value;
        }

        public Integer validateExecutableNone(final Integer value) {
            return value;
        }
    }

    /**
     * On TYPE.
     */

    @SuppressWarnings("JavaDoc")
    public static interface ValidateExecutableOnType {

        @POST
        @Min(0)
        public Integer validateExecutable(@Max(10) final Integer value);
    }

    /**
     * This {@link ValidateExecutable} annotation should be considered during validating phase.
     */
    @ValidateExecutable
    public static interface ValidateExecutableOnTypeDefault extends ValidateExecutableOnType {
    }

    @ValidateExecutable(ExecutableType.GETTER_METHODS)
    public static abstract class ValidateExecutableOnTypeDefaultAbstractResource implements ValidateExecutableOnTypeDefault {

        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @Path("on-type-default")
    @ValidateExecutable(ExecutableType.CONSTRUCTORS)
    public static class ValidateExecutableOnTypeDefaultResource extends ValidateExecutableOnTypeDefaultAbstractResource {
    }

    /**
     * This {@link ValidateExecutable} annotation should be considered during validating phase.
     */
    @ValidateExecutable(ExecutableType.NON_GETTER_METHODS)
    public static interface ValidateExecutableOnTypeMatch extends ValidateExecutableOnType {
    }

    @ValidateExecutable(ExecutableType.GETTER_METHODS)
    public static abstract class ValidateExecutableOnTypeMatchAbstractResource implements ValidateExecutableOnTypeMatch {

        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @Path("on-type-match")
    @ValidateExecutable(ExecutableType.NONE)
    public static class ValidateExecutableOnTypeMatchResource extends ValidateExecutableOnTypeMatchAbstractResource {
    }

    /**
     * This {@link ValidateExecutable} annotation should be considered during validating phase.
     */
    @ValidateExecutable(ExecutableType.CONSTRUCTORS)
    public static interface ValidateExecutableOnTypeMiss extends ValidateExecutableOnType {
    }

    @ValidateExecutable(ExecutableType.NON_GETTER_METHODS)
    public static abstract class ValidateExecutableOnTypeMissAbstractResource implements ValidateExecutableOnTypeMiss {

        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @Path("on-type-miss")
    @ValidateExecutable
    public static class ValidateExecutableOnTypeMissResource extends ValidateExecutableOnTypeMissAbstractResource {
    }

    /**
     * This {@link ValidateExecutable} annotation should be considered during validating phase.
     */
    @ValidateExecutable(ExecutableType.NONE)
    public static interface ValidateExecutableOnTypeNone extends ValidateExecutableOnType {
    }

    @ValidateExecutable(ExecutableType.ALL)
    public static abstract class ValidateExecutableOnTypeNoneAbstractResource implements ValidateExecutableOnTypeNone {

        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @Path("on-type-none")
    @ValidateExecutable({ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS})
    public static class ValidateExecutableOnTypeNoneResource extends ValidateExecutableOnTypeNoneAbstractResource {
    }

    /**
     * MIXED.
     */

    @ValidateExecutable(ExecutableType.NONE)
    public static interface ValidateExecutableMixedDefault {

        @Min(0)
        @ValidateExecutable
        public Integer validateExecutable(@Max(10) final Integer value);
    }

    @Path("mixed-default")
    public static class ValidateExecutableMixedDefaultResource implements ValidateExecutableMixedDefault {

        @POST
        @ValidateExecutable(ExecutableType.CONSTRUCTORS)
        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @ValidateExecutable
    public static interface ValidateExecutableMixedNone {

        @Min(0)
        @ValidateExecutable(ExecutableType.NONE)
        public Integer validateExecutable(@Max(10) final Integer value);
    }

    @Path("mixed-none")
    public static class ValidateExecutableMixedNoneResource implements ValidateExecutableMixedNone {

        @POST
        @ValidateExecutable(ExecutableType.ALL)
        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @ValidateExecutable
    public static interface ValidateExecutableMixedClassDefault {

        @Min(0)
        public Integer validateExecutable(@Max(10) final Integer value);
    }

    @Path("mixed-class-default")
    @ValidateExecutable(ExecutableType.NONE)
    public static class ValidateExecutableMixedClassDefaultResource implements ValidateExecutableMixedClassDefault {

        @POST
        @ValidateExecutable(ExecutableType.CONSTRUCTORS)
        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @ValidateExecutable(ExecutableType.NONE)
    public static interface ValidateExecutableMixedClassNone {

        @Min(0)
        public Integer validateExecutable(@Max(10) final Integer value);
    }

    @Path("mixed-class-none")
    @ValidateExecutable(ExecutableType.NON_GETTER_METHODS)
    public static class ValidateExecutableMixedClassNoneResource implements ValidateExecutableMixedClassNone {

        @POST
        @ValidateExecutable(ExecutableType.ALL)
        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(ValidateExecutableOnMethodsResource.class,
                ValidateExecutableOnTypeNoneResource.class,
                ValidateExecutableOnTypeMissResource.class,
                ValidateExecutableOnTypeMatchResource.class,
                ValidateExecutableOnTypeDefaultResource.class,
                ValidateExecutableMixedDefaultResource.class,
                ValidateExecutableMixedNoneResource.class,
                ValidateExecutableMixedClassNoneResource.class,
                ValidateExecutableMixedClassDefaultResource.class);
    }

    @Test
    public void testMixedClassValidatePassDefault() throws Exception {
        _test("mixed-class-default", 0, 200);
    }

    @Test
    public void testMixedClassValidateInputFailDefault() throws Exception {
        _test("mixed-class-default", 15, 400);
    }

    @Test
    public void testMixedClassValidateResultFailDefault() throws Exception {
        _test("mixed-class-default", -15, 500);
    }

    @Test
    public void testMixedClassValidatePassNone() throws Exception {
        _test("mixed-class-none", 0, 200);
    }

    @Test
    public void testMixedClassValidateInputPassNone() throws Exception {
        _test("mixed-class-none", 15, 200);
    }

    @Test
    public void testMixedClassValidateResultPassNone() throws Exception {
        _test("mixed-class-none", -15, 200);
    }
}
