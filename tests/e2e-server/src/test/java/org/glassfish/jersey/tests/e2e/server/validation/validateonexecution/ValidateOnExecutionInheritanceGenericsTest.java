/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2013-2017 Oracle and/or its affiliates. All rights reserved.
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

package org.glassfish.jersey.tests.e2e.server.validation.validateonexecution;

import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.core.Application;

import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.executable.ExecutableType;
import javax.validation.executable.ValidateOnExecution;

import org.glassfish.jersey.server.ResourceConfig;
import org.glassfish.jersey.server.ServerProperties;
import org.glassfish.jersey.test.util.runner.ConcurrentRunner;

import org.junit.Test;
import org.junit.runner.RunWith;

/**
 * @author Michal Gajdos
 */
@RunWith(ConcurrentRunner.class)
public class ValidateOnExecutionInheritanceGenericsTest extends ValidateOnExecutionAbstractTest {

    /**
     * On METHOD.
     */

    /**
     * {@link ValidateOnExecution} annotations from this interface should be considered during validating phase.
     */
    @SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
    public static interface ValidateExecutableOnMethodsValidation<T extends Number> {

        @Min(0)
        @ValidateOnExecution
        public T validateExecutableDefault(@Max(10) final T value);

        @Min(0)
        @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
        public T validateExecutableMatch(@Max(10) final T value);

        @Min(0)
        @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
        public T validateExecutableMiss(@Max(10) final T value);

        @Min(0)
        @ValidateOnExecution(type = ExecutableType.NONE)
        public T validateExecutableNone(@Max(10) final T value);
    }

    /**
     * Wrong generic types. {@link ValidateOnExecution} annotations should not be considered at all.
     *
     * @param <T>
     */
    @SuppressWarnings({"UnusedDeclaration", "JavaDoc"})
    public static interface ValidateExecutableOnMethodsCharSequenceValidation<T extends CharSequence> {

        @Min(10)
        @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
        public T validateExecutableDefault(@Max(0) final T value);

        @Min(10)
        @ValidateOnExecution(type = ExecutableType.NONE)
        public T validateExecutableMatch(@Max(0) final T value);

        @Min(10)
        @ValidateOnExecution
        public T validateExecutableMiss(@Max(0) final T value);

        @Min(10)
        @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
        public T validateExecutableNone(@Max(0) final T value);
    }

    @ValidateOnExecution(type = ExecutableType.ALL)
    public static interface ValidateExecutableOnMethodsJaxRs extends ValidateExecutableOnMethodsValidation<Integer> {

        @POST
        @Path("validateExecutableDefault")
        @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
        Integer validateExecutableDefault(final Integer value);

        @POST
        @Path("validateExecutableMatch")
        @ValidateOnExecution(type = ExecutableType.GETTER_METHODS)
        Integer validateExecutableMatch(final Integer value);

        @POST
        @Path("validateExecutableMiss")
        @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
        Integer validateExecutableMiss(final Integer value);

        @POST
        @Path("validateExecutableNone")
        @ValidateOnExecution(type = ExecutableType.ALL)
        Integer validateExecutableNone(final Integer value);
    }

    public abstract static class ValidateExecutableOnMethodsAbstractResource
            implements ValidateExecutableOnMethodsJaxRs, ValidateExecutableOnMethodsCharSequenceValidation<String> {

        @ValidateOnExecution(type = ExecutableType.NONE)
        public abstract Integer validateExecutableDefault(final Integer value);

        @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
        public abstract Integer validateExecutableMatch(final Integer value);

        @ValidateOnExecution(type = ExecutableType.ALL)
        public abstract Integer validateExecutableMiss(final Integer value);

        @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
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

        public String validateExecutableDefault(final String value) {
            return value;
        }

        public String validateExecutableMatch(final String value) {
            return value;
        }

        public String validateExecutableMiss(final String value) {
            return value;
        }

        public String validateExecutableNone(final String value) {
            return value;
        }
    }

    /**
     * On TYPE.
     */

    @SuppressWarnings("JavaDoc")
    public static interface ValidateExecutableOnType<T extends Number> {

        @Min(0)
        public T validateExecutable(@Max(10) final T value);
    }

    @SuppressWarnings("JavaDoc")
    public static interface ValidateExecutableCharSequenceOnType<X extends CharSequence> {

        @Min(10)
        public X validateExecutable(@Max(0) final X value);
    }

    @ValidateOnExecution
    public static interface ValidateExecutableOnTypeDefault extends ValidateExecutableOnType<Integer> {
    }

    @ValidateOnExecution
    public static interface ValidateExecutableCharSequenceOnTypeDefault extends ValidateExecutableCharSequenceOnType<String> {

        @ValidateOnExecution
        public String validateExecutable(final String value);
    }

    /**
     * This {@link ValidateOnExecution} annotation should be considered during validating phase.
     */
    @ValidateOnExecution(type = ExecutableType.GETTER_METHODS)
    public abstract static class ValidateExecutableOnTypeDefaultAbstractResource implements ValidateExecutableOnTypeDefault {

        @POST
        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @Path("on-type-default")
    @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
    public static class ValidateExecutableOnTypeDefaultResource extends ValidateExecutableOnTypeDefaultAbstractResource
            implements ValidateExecutableCharSequenceOnTypeDefault {

        @POST
        @Path("another")
        public String validateExecutable(final String value) {
            return value;
        }
    }

    /**
     * This {@link ValidateOnExecution} annotation should be considered during validating phase.
     */
    @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
    public static interface ValidateExecutableOnTypeMatch extends ValidateExecutableOnType<Integer> {
    }

    @ValidateOnExecution
    public static interface ValidateExecutableCharSequenceOnTypeMatch extends ValidateExecutableCharSequenceOnType<String> {

        @ValidateOnExecution
        public String validateExecutable(final String value);
    }

    @ValidateOnExecution(type = ExecutableType.GETTER_METHODS)
    public abstract static class ValidateExecutableOnTypeMatchAbstractResource implements ValidateExecutableOnTypeMatch {

        @POST
        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @Path("on-type-match")
    @ValidateOnExecution(type = ExecutableType.NONE)
    public static class ValidateExecutableOnTypeMatchResource extends ValidateExecutableOnTypeMatchAbstractResource
            implements ValidateExecutableCharSequenceOnTypeMatch {

        @POST
        @Path("another")
        public String validateExecutable(final String value) {
            return value;
        }
    }

    /**
     * This {@link ValidateOnExecution} annotation should be considered during validating phase.
     */
    @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
    public static interface ValidateExecutableOnTypeMiss extends ValidateExecutableOnType<Integer> {
    }

    @ValidateOnExecution
    public static interface ValidateExecutableCharSequenceOnTypeMiss extends ValidateExecutableCharSequenceOnType<String> {

        @ValidateOnExecution
        public String validateExecutable(final String value);
    }

    @ValidateOnExecution(type = ExecutableType.NON_GETTER_METHODS)
    public abstract static class ValidateExecutableOnTypeMissAbstractResource implements ValidateExecutableOnTypeMiss {

        @POST
        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @Path("on-type-miss")
    @ValidateOnExecution
    public static class ValidateExecutableOnTypeMissResource extends ValidateExecutableOnTypeMissAbstractResource
            implements ValidateExecutableCharSequenceOnTypeMiss {

        @POST
        @Path("another")
        public String validateExecutable(final String value) {
            return value;
        }
    }

    /**
     * This {@link ValidateOnExecution} annotation should be considered during validating phase.
     */
    @ValidateOnExecution(type = ExecutableType.NONE)
    public static interface ValidateExecutableOnTypeNone extends ValidateExecutableOnType<Integer> {
    }

    @ValidateOnExecution
    public static interface ValidateExecutableCharSequenceOnTypeNone extends ValidateExecutableCharSequenceOnType<String> {

        @ValidateOnExecution
        public String validateExecutable(final String value);
    }

    @ValidateOnExecution(type = ExecutableType.ALL)
    public abstract static class ValidateExecutableOnTypeNoneAbstractResource implements ValidateExecutableOnTypeNone {

        @POST
        public Integer validateExecutable(final Integer value) {
            return value;
        }
    }

    @Path("on-type-none")
    @ValidateOnExecution(type = {ExecutableType.CONSTRUCTORS, ExecutableType.NON_GETTER_METHODS})
    public static class ValidateExecutableOnTypeNoneResource extends ValidateExecutableOnTypeNoneAbstractResource
            implements ValidateExecutableCharSequenceOnTypeNone {

        @POST
        @Path("another")
        public String validateExecutable(final String value) {
            return value;
        }
    }

    /**
     * MIXED.
     */

    @ValidateOnExecution(type = ExecutableType.NONE)
    public static interface ValidateExecutableMixedDefault<T extends Number> {

        @Min(0)
        @ValidateOnExecution
        public T validateExecutable(@Max(10) final T value);
    }

    @ValidateOnExecution(type = ExecutableType.NONE)
    public static interface ValidateExecutableCharSequenceMixedDefault<T extends CharSequence> {

        @Min(10)
        @ValidateOnExecution(type = ExecutableType.NONE)
        public T validateExecutable(@Max(0) final T value);
    }

    @Path("mixed-default")
    public static class ValidateExecutableMixedDefaultResource
            implements ValidateExecutableMixedDefault<Integer>, ValidateExecutableCharSequenceMixedDefault<String> {

        @POST
        @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
        public Integer validateExecutable(final Integer value) {
            return value;
        }

        @POST
        @Path("another")
        @ValidateOnExecution(type = ExecutableType.CONSTRUCTORS)
        public String validateExecutable(final String value) {
            return value;
        }
    }

    @ValidateOnExecution
    public static interface ValidateExecutableMixedNone<T extends Number> {

        @Min(0)
        @ValidateOnExecution(type = ExecutableType.NONE)
        public T validateExecutable(@Max(10) final T value);
    }

    @ValidateOnExecution
    public static interface ValidateExecutableCharSequenceMixedNone<T extends CharSequence> {

        @Min(10)
        @ValidateOnExecution
        public T validateExecutable(@Max(0) final T value);
    }

    @Path("mixed-none")
    public static class ValidateExecutableMixedNoneResource
            implements ValidateExecutableMixedNone<Integer>, ValidateExecutableCharSequenceMixedNone<String> {

        @POST
        @ValidateOnExecution(type = ExecutableType.ALL)
        public Integer validateExecutable(final Integer value) {
            return value;
        }

        @POST
        @Path("another")
        @ValidateOnExecution(type = ExecutableType.ALL)
        public String validateExecutable(final String value) {
            return value;
        }
    }

    @Override
    protected Application configure() {
        return new ResourceConfig(ValidateExecutableOnMethodsResource.class,
                ValidateExecutableOnTypeDefaultResource.class,
                ValidateExecutableOnTypeMatchResource.class,
                ValidateExecutableOnTypeMissResource.class,
                ValidateExecutableOnTypeNoneResource.class,
                ValidateExecutableMixedDefaultResource.class,
                ValidateExecutableMixedNoneResource.class)
                .property(ServerProperties.BV_DISABLE_VALIDATE_ON_EXECUTABLE_OVERRIDE_CHECK, true);
    }

    @Test
    public void testOnTypeValidateInputPassValidateExecutableDefault() throws Exception {
        _testOnType("default", 15, 400);
    }

    @Test
    public void testOnTypeValidateResultPassNoValidateExecutableDefault() throws Exception {
        _testOnType("default", -15, 200);
    }
}
