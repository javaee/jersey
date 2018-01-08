/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2017 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.inject.cdi.se.injector;

import java.io.PrintStream;
import java.io.PrintWriter;
import java.util.LinkedList;
import java.util.List;

/**
 * This exception can contain multiple other exceptions.
 * However, it will also have the causal chain of the
 * first exception added to the list of exceptions.
 *
 * @author John Wells (john.wells at oracle.com)
 */
public class MultiException extends RuntimeException {
    /**
     * For serialization
     */
    private static final long serialVersionUID = 2112432697858621044L;
    private final Object lock = new byte[0]; // byte[0] is an arbitrary type that is Serializable
    private final List<Throwable> throwables = new LinkedList<>();
    private boolean reportToErrorService = true;

    /**
     * Creates an empty MultiException.
     */
    public MultiException() {
    }

    /**
     * This list must have at least one element in it.
     * The first element of the list will become the
     * cause of this exception, and its message will become
     * the message of this exception.
     *
     * @param ths A non-null, non-empty list of exceptions.
     */
    public MultiException(List<Throwable> ths) {
        super(ths.get(0).getMessage(), ths.get(0));

        for (Throwable th : ths) {
            if (th instanceof MultiException) {
                MultiException me = (MultiException) th;

                throwables.addAll(me.throwables);
            } else {
                throwables.add(th);
            }
        }
    }

    /**
     * This allows for construction of a MultiException
     * with one element in its list.
     *
     * @param th May not be null.
     */
    public MultiException(Throwable th, boolean reportToErrorService) {
        super(th.getMessage(), th);

        if (th instanceof MultiException) {
            MultiException me = (MultiException) th;

            throwables.addAll(me.throwables);
        } else {
            throwables.add(th);
        }

        this.reportToErrorService = reportToErrorService;
    }

    /**
     * This allows for construction of a MultiException
     * with one element in its list.
     *
     * @param th May not be null.
     */
    public MultiException(Throwable th) {
        this(th, true);
    }

    /**
     * Gets all the errors associated with this MultiException.
     *
     * @return All the errors associated with this MultiException. Will
     * not return null, but may return an empty object.
     */
    public List<Throwable> getErrors() {
        synchronized (lock) {
            return new LinkedList<>(throwables);
        }
    }

    /**
     * Adds an error to an existing exception.
     *
     * @param error The exception to add.
     */
    public void addError(Throwable error) {
        synchronized (lock) {
            throwables.add(error);
        }
    }

    /**
     * Gets the message associated with this exception.
     */
    public String getMessage() {
        List<Throwable> listCopy = getErrors();
        StringBuffer sb = new StringBuffer("A MultiException has " + listCopy.size() + " exceptions.  They are:\n");

        int lcv = 1;
        for (Throwable th : listCopy) {
            sb.append(lcv++ + ". " + th.getClass().getName() + ((th.getMessage() != null) ? ": " + th.getMessage() : "") + "\n");
        }

        return sb.toString();
    }

    /**
     * Prints the stack trace of this exception to the given PrintStream.
     */
    public void printStackTrace(PrintStream s) {
        List<Throwable> listCopy = getErrors();

        if (listCopy.size() <= 0) {
            super.printStackTrace(s);
            return;
        }

        int lcv = 1;
        for (Throwable th : listCopy) {
            s.println("MultiException stack " + lcv++ + " of " + listCopy.size());
            th.printStackTrace(s);
        }
    }

    /**
     * Prints the stack trace of this exception to the given PrintWriter.
     */
    public void printStackTrace(PrintWriter s) {
        List<Throwable> listCopy = getErrors();

        if (listCopy.size() <= 0) {
            super.printStackTrace(s);
            return;
        }

        int lcv = 1;
        for (Throwable th : listCopy) {
            s.println("MultiException stack " + lcv++ + " of " + listCopy.size());
            th.printStackTrace(s);
        }
    }

    /**
     * Returns true if this exception should be reported
     * to the error service when thrown during a creation
     * or deletion of a service.
     *
     * @return true if this exception should be reported to
     * the error service when creating or deleting a service.
     */
    public boolean getReportToErrorService() {
        return reportToErrorService;
    }

    /**
     * Sets if this exception should be reported
     * to the error service when thrown during a creation
     * or deletion of a service.
     *
     * @param report true if this exception should be reported to
     *               the error service when creating or deleting a service.
     */
    public void setReportToErrorService(boolean report) {
        reportToErrorService = report;
    }

    @Override
    public String toString() {
        return getMessage();
    }

}
