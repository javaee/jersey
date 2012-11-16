/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.spi;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.logging.Logger;

import org.glassfish.jersey.internal.LocalizationMessages;

/**
 * Errors utility used to file processing errors (e.g. validation, provider, resource building errors).
 * <p/>
 * Error filing methods ({@code #warning}, {@code #error}, {@code #fatal}) can be invoked only in the "error scope" which is
 * created by {@link #process(org.glassfish.jersey.spi.Errors.Closure)} or
 * {@link #processWithException(org.glassfish.jersey.spi.Errors.Closure)} methods. Filed error messages are present also in this
 * scope.
 * <p/>
 *
 * TODO do not use static thread local?
 *
 * @author Michal Gajdos (michal.gajdos at oracle.com)
 */
public class Errors {

    private static final Logger LOGGER = Logger.getLogger(Errors.class.getName());

    private static final ThreadLocal<Errors> errors = new ThreadLocal<Errors>();

    /**
     * Add an error message to the list of errors.
     *
     * @param message message of the error.
     * @param isFatal indicates whether this error should be treated as fatal error.
     */
    public static void error(final String message, final boolean isFatal) {
        error(null, message, isFatal);
    }

    /**
     * Add an error message to the list of errors.
     *
     * @param source source of the error.
     * @param message message of the error.
     * @param isFatal indicates whether this error should be treated as fatal error.
     */
    public static void error(final Object source, final String message, final boolean isFatal) {
        getInstance().issues.add(new ErrorMessage(source, message, isFatal));
    }

    /**
     * Add a fatal error message to the list of errors.
     *
     * @param source source of the error.
     * @param message message of the error.
     */
    public static void fatal(final Object source, final String message) {
        error(source, message, true);
    }

    /**
     * Add a warning message to the list of errors.
     *
     * @param source source of the error.
     * @param message message of the error.
     */
    public static void warning(final Object source, final String message) {
        error(source, message, false);
    }

    private static List<ErrorMessage> processErrors(final boolean throwException) {
        final List<ErrorMessage> messages = new ArrayList<ErrorMessage>(errors.get().issues);
        boolean isFatal = false;

        if (!messages.isEmpty()) {
            StringBuilder errors = new StringBuilder("\n");
            StringBuilder warnings = new StringBuilder();

            for (final ErrorMessage issue : messages) {
                if (issue.isFatal()) {
                    isFatal = true;
                    errors.append(LocalizationMessages.ERROR_MSG(issue.getMessage())).append('\n');
                } else {
                    warnings.append(LocalizationMessages.WARNING_MSG(issue.getMessage())).append('\n');
                }
            }

            if (isFatal) {
                LOGGER.severe(LocalizationMessages.ERRORS_AND_WARNINGS_DETECTED(errors.append(warnings).toString()));
                if (throwException) {
                    throw new ErrorMessagesException(messages);
                }
            } else {
                LOGGER.warning(LocalizationMessages.WARNINGS_DETECTED(warnings.toString()));
            }
        }

        return messages;
    }

    /**
     * Indicates whether a fatal error message is present in the list of all errors.
     */
    public static boolean fatalIssuesFound() {
        for (final ErrorMessage message : getInstance().issues) {
            if (message.isFatal()) {
                return true;
            }
        }
        return false;
    }

    /**
     * Closure interface.
     */
    public static interface Closure<T> {

        /**
         * Invoke the closure.
         */
        public T invoke();
    }

    /**
     * Invoke given closure and gather errors. After the closure method returns all gathered errors are
     * logged and even if there is a fatal error present in the list of errors no exception is thrown.
     *
     * @param closure closure to be invoked.
     */
    public static <T> T process(final Closure<T> closure) {
        return process(closure, false);
    }

    /**
     * Invoke given closure and gather errors. After the closure method returns all gathered errors are
     * logged and if there is a fatal error present in the list of errors an {@link ErrorMessagesException exception} is thrown.
     *
     * @param closure closure to be invoked.
     */
    public static <T> T processWithException(final Closure<T> closure) {
        return process(closure, true);
    }

    private static <T> T process(final Closure<T> closure, final boolean throwException) {
        Errors instance = errors.get();
        if (instance == null) {
            instance = new Errors();
            errors.set(instance);
        }
        instance.preProcess();

        RuntimeException caught = null;
        try {
            return closure.invoke();
        } catch (RuntimeException re) {
            // If a runtime exception is caught then report errors and rethrow.
            caught = re;
        } finally {
            instance.postProcess(throwException && caught == null);
        }

        throw caught;
    }

    private static Errors getInstance() {
        final Errors instance = errors.get();
        // No error processing in scope
        if (instance == null) {
            throw new IllegalStateException(LocalizationMessages.NO_ERROR_PROCESSING_IN_SCOPE());
        }
        // The following should not be necessary but given the fragile nature of
        // static thread local probably best to add it in case some internals of
        // this class change
        if (instance.stack == 0) {
            errors.remove();
            throw new IllegalStateException(LocalizationMessages.NO_ERROR_PROCESSING_IN_SCOPE());
        }
        return instance;
    }

    /**
     * Return all error messages.
     *
     * @return non-null error list.
     */
    public static List<ErrorMessage> getErrorMessages() {
        return getErrorMessages(false);
    }

    /**
     * Return list of error messages filed after the mark flag was set.
     *
     * @return non-null error list.
     */
    public static List<ErrorMessage> getErrorMessages(final boolean afterMark) {
        return getInstance()._getErrorMessages(afterMark);
    }

    public static void mark() {
        getInstance()._mark();
    }

    public static void unmark() {
        getInstance()._unmark();
    }

    public static void reset() {
        getInstance()._reset();
    }

    private final ArrayList<ErrorMessage> issues = new ArrayList<ErrorMessage>(0);

    private Errors() {
    }

    private int mark = -1;
    private int stack = 0;

    private void _mark() {
        mark = issues.size();
    }

    private void _unmark() {
        mark = -1;
    }

    private void _reset() {
        if (mark >= 0 && mark < issues.size()) {
            issues.subList(mark, issues.size()).clear();
            _unmark();
        }
    }

    private void preProcess() {
        stack++;
    }

    private void postProcess(boolean throwException) {
        stack--;

        if (stack == 0) {
            try {
                if (!issues.isEmpty()) {
                    processErrors(throwException);
                }
            } finally {
                errors.remove();
            }
        }
    }

    private List<ErrorMessage> _getErrorMessages(final boolean afterMark) {
        if (afterMark && mark >= 0 && mark < issues.size()) {
            return Collections.unmodifiableList(new ArrayList<ErrorMessage>(issues.subList(mark, issues.size())));
        } else {
            return Collections.unmodifiableList(new ArrayList<ErrorMessage>(issues));
        }
    }

    /**
     * Error message exception.
     */
    public static class ErrorMessagesException extends RuntimeException {

        private final List<ErrorMessage> messages;

        private ErrorMessagesException(final List<ErrorMessage> messages) {
            this.messages = messages;
        }

        /**
         * Get encountered errors.
         *
         * @return encountered errors.
         */
        public List<ErrorMessage> getMessages() {
            return messages;
        }
    }

    /**
     * Generic error message.
     */
    public static class ErrorMessage {

        private final Object source;
        private final String message;
        private final boolean isFatal;

        private ErrorMessage(final Object source, final String message, final boolean isFatal) {
            this.source = source;
            this.message = message;
            this.isFatal = isFatal;
        }

        /**
         * Check if the issue is fatal.
         *
         * Fatal issues typically prevent the deployment of the application to succeed.
         *
         * @return {@code true} if the issue is fatal, {@code false} otherwise.
         */
        public boolean isFatal() {
            return isFatal;
        }

        /**
         * Human-readable description of the issue.
         *
         * @return message describing the issue.
         */
        public String getMessage() {
            return message;
        }

        /**
         * The issue source.
         *
         * Identifies the object where the issue was found.
         *
         * @return source of the issue.
         */
        public Object getSource() {
            return source;
        }

        @Override
        public boolean equals(final Object o) {
            if (this == o)
                return true;
            if (o == null || getClass() != o.getClass())
                return false;

            final ErrorMessage that = (ErrorMessage) o;

            if (isFatal != that.isFatal)
                return false;
            if (message != null ? !message.equals(that.message) : that.message != null)
                return false;
            if (source != null ? !source.equals(that.source) : that.source != null)
                return false;

            return true;
        }

        @Override
        public int hashCode() {
            int result = source != null ? source.hashCode() : 0;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            result = 31 * result + (isFatal ? 1 : 0);
            return result;
        }
    }

}
