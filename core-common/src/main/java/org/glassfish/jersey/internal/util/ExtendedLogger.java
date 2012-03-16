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
package org.glassfish.jersey.internal.util;

import java.util.Arrays;
import java.util.ResourceBundle;
import java.util.logging.Filter;
import java.util.logging.Handler;
import java.util.logging.Level;
import java.util.logging.LogRecord;
import java.util.logging.Logger;

/**
 * Logger extension with additional logging utility & convenience methods.
 *
 * @author Marek Potociar (marek.potociar at oracle.com)
 */
public final class ExtendedLogger extends Logger {
    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;
    private final Level debugLevel;

    public ExtendedLogger(final Logger logger, final Level debugLevel) {
        super(logger.getName(), logger.getResourceBundleName());
        this.logger = logger;
        this.debugLevel = debugLevel;
    }

    /**
     * Check if the debug level is loggable.
     *
     * @return {@code true} if the debug level is loggable, {@code false}
     *     otherwise.
     */
    public boolean isDebugLoggable() {
        return logger.isLoggable(debugLevel);
    }

    /**
     * Get the configured debug level.
     *
     * @return configured debug level.
     */
    public Level getDebugLevel() {
        return debugLevel;
    }

    /**
     * Log a debug message using the configured debug level.
     *
     * This method appends thread name information to the end of the logged message.
     *
     * @param messageTemplate
     * @param args
     */
    public void debugLog(final String messageTemplate, final Object... args) {

        if (logger.isLoggable(debugLevel)) {
            final Object[] messageArguments;
            if (args == null || args.length == 0) {
                messageArguments = new Object[1];
            } else {
                messageArguments = Arrays.copyOf(args, args.length + 1);
            }
            messageArguments[messageArguments.length - 1] = Thread.currentThread().getName();

            final StringBuilder messageBuilder = new StringBuilder(messageTemplate.length() + 15);
            messageBuilder.append(messageTemplate)
                    .append(" on thread {").append(messageArguments.length - 1).append('}');

            logger.log(debugLevel, messageBuilder.toString(), messageArguments);
        }
    }

    @Override
    public String toString() {
        return logger.toString();
    }

    @Override
    public int hashCode() {
        return logger.hashCode();
    }

    @Override
    public boolean equals(Object obj) {
        return logger.equals(obj);
    }

    @Override
    public void warning(String msg) {
        logger.warning(msg);
    }

    @Override
    public void throwing(String sourceClass, String sourceMethod, Throwable thrown) {
        logger.throwing(sourceClass, sourceMethod, thrown);
    }

    @Override
    public void severe(String msg) {
        logger.severe(msg);
    }

    @Override
    public void setUseParentHandlers(boolean useParentHandlers) {
        logger.setUseParentHandlers(useParentHandlers);
    }

    @Override
    public void setParent(Logger parent) {
        logger.setParent(parent);
    }

    @Override
    public void setLevel(Level newLevel) throws SecurityException {
        logger.setLevel(newLevel);
    }

    @Override
    public void setFilter(Filter newFilter) throws SecurityException {
        logger.setFilter(newFilter);
    }

    @Override
    public void removeHandler(Handler handler) throws SecurityException {
        logger.removeHandler(handler);
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Throwable thrown) {
        logger.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown);
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object[] params) {
        logger.logrb(level, sourceClass, sourceMethod, bundleName, msg, params);
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg, Object param1) {
        logger.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1);
    }

    @Override
    public void logrb(Level level, String sourceClass, String sourceMethod, String bundleName, String msg) {
        logger.logrb(level, sourceClass, sourceMethod, bundleName, msg);
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Throwable thrown) {
        logger.logp(level, sourceClass, sourceMethod, msg, thrown);
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object[] params) {
        logger.logp(level, sourceClass, sourceMethod, msg, params);
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg, Object param1) {
        logger.logp(level, sourceClass, sourceMethod, msg, param1);
    }

    @Override
    public void logp(Level level, String sourceClass, String sourceMethod, String msg) {
        logger.logp(level, sourceClass, sourceMethod, msg);
    }

    @Override
    public void log(Level level, String msg, Throwable thrown) {
        logger.log(level, msg, thrown);
    }

    @Override
    public void log(Level level, String msg, Object[] params) {
        logger.log(level, msg, params);
    }

    @Override
    public void log(Level level, String msg, Object param1) {
        logger.log(level, msg, param1);
    }

    @Override
    public void log(Level level, String msg) {
        logger.log(level, msg);
    }

    @Override
    public void log(LogRecord record) {
        logger.log(record);
    }

    @Override
    public boolean isLoggable(Level level) {
        return logger.isLoggable(level);
    }

    @Override
    public void info(String msg) {
        logger.info(msg);
    }

    @Override
    public boolean getUseParentHandlers() {
        return logger.getUseParentHandlers();
    }

    @Override
    public String getResourceBundleName() {
        return logger.getResourceBundleName();
    }

    @Override
    public ResourceBundle getResourceBundle() {
        return logger.getResourceBundle();
    }

    @Override
    public Logger getParent() {
        return logger.getParent();
    }

    @Override
    public String getName() {
        return logger.getName();
    }

    @Override
    public Level getLevel() {
        return logger.getLevel();
    }

    @Override
    public Handler[] getHandlers() {
        return logger.getHandlers();
    }

    @Override
    public Filter getFilter() {
        return logger.getFilter();
    }

    @Override
    public void finest(String msg) {
        logger.finest(msg);
    }

    @Override
    public void finer(String msg) {
        logger.finer(msg);
    }

    @Override
    public void fine(String msg) {
        logger.fine(msg);
    }

    @Override
    public void exiting(String sourceClass, String sourceMethod, Object result) {
        logger.exiting(sourceClass, sourceMethod, result);
    }

    @Override
    public void exiting(String sourceClass, String sourceMethod) {
        logger.exiting(sourceClass, sourceMethod);
    }

    @Override
    public void entering(String sourceClass, String sourceMethod, Object[] params) {
        logger.entering(sourceClass, sourceMethod, params);
    }

    @Override
    public void entering(String sourceClass, String sourceMethod, Object param1) {
        logger.entering(sourceClass, sourceMethod, param1);
    }

    @Override
    public void entering(String sourceClass, String sourceMethod) {
        logger.entering(sourceClass, sourceMethod);
    }

    @Override
    public void config(String msg) {
        logger.config(msg);
    }

    @Override
    public void addHandler(Handler handler) throws SecurityException {
        logger.addHandler(handler);
    }
}
