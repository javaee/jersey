/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2012-2016 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.internal;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Deque;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.logging.Logger;

import org.glassfish.jersey.Severity;
import org.glassfish.jersey.internal.util.Producer;

/**
 * Errors utility used to file processing messages (e.g. validation, provider, resource building errors, hint).
 * <p/>
 * Error filing methods ({@code #warning}, {@code #error}, {@code #fatal}) can be invoked only in the "error scope" which is
 * created by {@link #process(Producer)} or
 * {@link #processWithException(Producer)} methods. Filed error messages are present also in this
 * scope.
 * <p/>
 * TODO do not use static thread local?
 *
 * @author Michal Gajdos
 */
public class Errors {

    private static final Logger LOGGER = Logger.getLogger(Errors.class.getName());

    private static final ThreadLocal<Errors> errors = new ThreadLocal<Errors>();

    /**
     * Add an error to the list of messages.
     *
     * @param message  message of the error.
     * @param severity indicates severity of added error.
     */
    public static void error(final String message, Severity severity) {
        error(null, message, severity);
    }

    /**
     * Add an error to the list of messages.
     *
     * @param source   source of the error.
     * @param message  message of the error.
     * @param severity indicates severity of added error.
     */
    public static void error(final Object source, final String message, final Severity severity) {
        getInstance().issues.add(new ErrorMessage(source, message, severity));
    }

    /**
     * Add a fatal error to the list of messages.
     *
     * @param source  source of the error.
     * @param message message of the error.
     */
    public static void fatal(final Object source, final String message) {
        error(source, message, Severity.FATAL);
    }

    /**
     * Add a warning to the list of messages.
     *
     * @param source  source of the error.
     * @param message message of the error.
     */
    public static void warning(final Object source, final String message) {
        error(source, message, Severity.WARNING);
    }

    /**
     * Add a hint to the list of messages.
     *
     * @param source  source of the error.
     * @param message message of the error.
     */
    public static void hint(final Object source, final String message) {
        getInstance().issues.add(new ErrorMessage(source, message, Severity.HINT));
    }

    /**
     * Log errors and throw an exception if there are any fatal issues detected and
     * the {@code throwException} flag has been set to {@code true}.
     *
     * @param throwException if set to {@code true}, any fatal issues will cause a {@link ErrorMessagesException}
     *                       to be thrown.
     */
    private static void processErrors(final boolean throwException) {
        final List<ErrorMessage> errors = new ArrayList<ErrorMessage>(Errors.errors.get().issues);
        boolean isFatal = logErrors(errors);
        if (throwException && isFatal) {
            throw new ErrorMessagesException(errors);
        }
    }

    /**
     * Log errors and return a status flag indicating whether a fatal issue has been found
     * in the error collection.
     * <p>
     * The {@code afterMark} flag indicates whether only those issues should be logged that were
     * added after a {@link #mark() mark has been set}.
     * </p>
     *
     * @param afterMark if {@code true}, only issues added after a mark has been set are returned,
     *                  if {@code false} all issues are returned.
     * @return {@code true} if there are any fatal issues present in the collection, {@code false}
     *         otherwise.
     */
    public static boolean logErrors(final boolean afterMark) {
        return logErrors(getInstance()._getErrorMessages(afterMark));
    }

    /**
     * Log supplied errors and return a status flag indicating whether a fatal issue has been found
     * in the error collection.
     *
     * @param errors a collection of errors to be logged.
     * @return {@code true} if there are any fatal issues present in the collection, {@code false}
     *         otherwise.
     */
    private static boolean logErrors(final Collection<ErrorMessage> errors) {
        boolean isFatal = false;

        if (!errors.isEmpty()) {
            StringBuilder fatals = new StringBuilder("\n");
            StringBuilder warnings = new StringBuilder();
            StringBuilder hints = new StringBuilder();

            for (final ErrorMessage error : errors) {
                switch (error.getSeverity()) {
                    case FATAL:
                        isFatal = true;
                        fatals.append(LocalizationMessages.ERROR_MSG(error.getMessage())).append('\n');
                        break;
                    case WARNING:
                        warnings.append(LocalizationMessages.WARNING_MSG(error.getMessage())).append('\n');
                        break;
                    case HINT:
                        hints.append(LocalizationMessages.HINT_MSG(error.getMessage())).append('\n');
                        break;
                }
            }

            if (isFatal) {
                LOGGER.severe(LocalizationMessages.ERRORS_AND_WARNINGS_DETECTED(fatals.append(warnings)
                        .append(hints).toString()));
            } else {
                if (warnings.length() > 0) {
                    LOGGER.warning(LocalizationMessages.WARNINGS_DETECTED(warnings.toString()));
                }

                if (hints.length() > 0) {
                    LOGGER.config(LocalizationMessages.HINTS_DETECTED(hints.toString()));
                }
            }
        }

        return isFatal;
    }


    /**
     * Check whether a fatal error is present in the list of all messages.
     *
     * @return {@code true} if there are any fatal issues in this error context, {@code false} otherwise.
     */
    public static boolean fatalIssuesFound() {
        for (final ErrorMessage message : getInstance().issues) {
            if (message.getSeverity() == Severity.FATAL) {
                return true;
            }
        }
        return false;
    }

    /**
     * Invoke given producer task and gather errors.
     * <p/>
     * After the task is complete all gathered errors are logged. No exception is thrown
     * even if there is a fatal error present in the list of errors.
     *
     * @param producer producer task to be invoked.
     * @return the result produced by the task.
     */
    public static <T> T process(final Producer<T> producer) {
        return process(producer, false);
    }

    /**
     * Invoke given callable task and gather messages.
     * <p/>
     * After the task is complete all gathered errors are logged. Any exception thrown
     * by the throwable is re-thrown.
     *
     * @param task callable task to be invoked.
     * @return the result produced by the task.
     * @throws Exception exception thrown by the task.
     */
    public static <T> T process(final Callable<T> task) throws Exception {
        return process(task, true);
    }

    /**
     * Invoke given producer task and gather messages.
     * <p/>
     * After the task is complete all gathered errors are logged. If there is a fatal error
     * present in the list of errors an {@link ErrorMessagesException exception} is thrown.
     *
     * @param producer producer task to be invoked.
     * @return the result produced by the task.
     */
    public static <T> T processWithException(final Producer<T> producer) {
        return process(producer, true);
    }

    /**
     * Invoke given task and gather messages.
     * <p/>
     * After the task is complete all gathered errors are logged. No exception is thrown
     * even if there is a fatal error present in the list of errors.
     *
     * @param task task to be invoked.
     */
    public static void process(final Runnable task) {
        process(new Producer<Void>() {

            @Override
            public Void call() {
                task.run();
                return null;
            }
        }, false);
    }

    /**
     * Invoke given task and gather messages.
     * <p/>
     * After the task is complete all gathered errors are logged. If there is a fatal error
     * present in the list of errors an {@link ErrorMessagesException exception} is thrown.
     *
     * @param task task to be invoked.
     */
    public static void processWithException(final Runnable task) {
        process(new Producer<Void>() {
            @Override
            public Void call() {
                task.run();
                return null;
            }
        }, true);
    }

    private static <T> T process(final Producer<T> task, final boolean throwException) {
        try {
            return process((Callable<T>) task, throwException);
        } catch (RuntimeException ex) {
            throw ex;
        } catch (Exception ex) {
            throw new RuntimeException(ex);
        }
    }

    private static <T> T process(final Callable<T> task, final boolean throwException) throws Exception {
        Errors instance = errors.get();
        if (instance == null) {
            instance = new Errors();
            errors.set(instance);
        }
        instance.preProcess();

        Exception caught = null;
        try {
            return task.call();
        } catch (Exception re) {
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
     * Get the list of all error messages.
     *
     * @return non-null error message list.
     */
    public static List<ErrorMessage> getErrorMessages() {
        return getErrorMessages(false);
    }

    /**
     * Get the list of error messages.
     * <p>
     * The {@code afterMark} flag indicates whether only those issues should be returned that were
     * added after a {@link #mark() mark has been set}.
     * </p>
     *
     * @param afterMark if {@code true}, only issues added after a mark has been set are returned,
     *                  if {@code false} all issues are returned.
     * @return non-null error list.
     */
    public static List<ErrorMessage> getErrorMessages(final boolean afterMark) {
        return getInstance()._getErrorMessages(afterMark);
    }

    /**
     * Set a mark at a current position in the errors messages list.
     */
    public static void mark() {
        getInstance()._mark();
    }

    /**
     * Remove a previously set mark, if any.
     */
    public static void unmark() {
        getInstance()._unmark();
    }

    /**
     * Removes all issues that have been added since the last marked position as well as
     * removes the last mark.
     */
    public static void reset() {
        getInstance()._reset();
    }

    private final ArrayList<ErrorMessage> issues = new ArrayList<ErrorMessage>(0);

    private Errors() {
    }

    private Deque<Integer> mark = new ArrayDeque<Integer>(4);
    private int stack = 0;

    private void _mark() {
        mark.addLast(issues.size());
    }

    private void _unmark() {
        mark.pollLast();
    }

    private void _reset() {
        final Integer _pos = mark.pollLast(); // also performs "unmark" functionality
        final int markedPos = (_pos == null) ? -1 : _pos;

        if (markedPos >= 0 && markedPos < issues.size()) {
            issues.subList(markedPos, issues.size()).clear();
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
        if (afterMark) {
            final Integer _pos = mark.peekLast();
            final int markedPos = (_pos == null) ? -1 : _pos;

            if (markedPos >= 0 && markedPos < issues.size()) {
                return Collections.unmodifiableList(new ArrayList<ErrorMessage>(issues.subList(markedPos, issues.size())));
            } // else return all errors
        }

        return Collections.unmodifiableList(new ArrayList<ErrorMessage>(issues));
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
         * Get encountered error messages.
         *
         * @return encountered error messages.
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
        private final Severity severity;

        private ErrorMessage(final Object source, final String message, Severity severity) {
            this.source = source;
            this.message = message;
            this.severity = severity;
        }

        /**
         * Get {@link Severity}.
         *
         * @return severity of current {@code ErrorMessage}.
         */
        public Severity getSeverity() {
            return severity;
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
         * <p/>
         * Identifies the object where the issue was found.
         *
         * @return source of the issue.
         */
        public Object getSource() {
            return source;
        }

        @Override
        public boolean equals(Object o) {
            if (this == o) {
                return true;
            }
            if (o == null || getClass() != o.getClass()) {
                return false;
            }

            ErrorMessage that = (ErrorMessage) o;

            if (message != null ? !message.equals(that.message) : that.message != null) {
                return false;
            }
            if (severity != that.severity) {
                return false;
            }
            if (source != null ? !source.equals(that.source) : that.source != null) {
                return false;
            }

            return true;
        }

        @Override
        public int hashCode() {
            int result = source != null ? source.hashCode() : 0;
            result = 31 * result + (message != null ? message.hashCode() : 0);
            result = 31 * result + (severity != null ? severity.hashCode() : 0);
            return result;
        }
    }
}
