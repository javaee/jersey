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
@SuppressWarnings("deprecation")
public final class ExtendedLogger {

    @SuppressWarnings("NonConstantLogger")
    private final Logger logger;
    private final Level debugLevel;

    /**
     * Create new logger extension.
     *
     * @param logger     wrapped logger.
     * @param debugLevel debug message logging level.
     */
    public ExtendedLogger(final Logger logger, final Level debugLevel) {
        this.logger = logger;
        this.debugLevel = debugLevel;
    }

    /**
     * Check if the debug level is loggable.
     *
     * @return {@code true} if the debug level is loggable, {@code false}
     *         otherwise.
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
     * @param message debug message.
     */
    public void debugLog(final String message) {
        debugLog(message, (Object[]) null);
    }

    /**
     * Log a debug message using the configured debug level.
     *
     * This method appends thread name information to the end of the logged message.
     *
     * @param messageTemplate debug message template.
     * @param args            message template parameters.
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

            logger.log(debugLevel, "[DEBUG] " + messageTemplate + " on thread {" + (messageArguments.length - 1) + '}',
                    messageArguments);
        }
    }

    @Override
    public String toString() {
        return "ExtendedLogger{" + "logger=" + logger + ", debugLevel=" + debugLevel + '}';
    }

    @Override
    public boolean equals(final Object obj) {
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        final ExtendedLogger other = (ExtendedLogger) obj;
        if (this.logger != other.logger && (this.logger == null || !this.logger.equals(other.logger))) {
            return false;
        }
        if (this.debugLevel != other.debugLevel && (this.debugLevel == null || !this.debugLevel.equals(other.debugLevel))) {
            return false;
        }
        return true;
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = 17 * hash + (this.logger != null ? this.logger.hashCode() : 0);
        hash = 17 * hash + (this.debugLevel != null ? this.debugLevel.hashCode() : 0);
        return hash;
    }

    public void warning(final String msg) {
        logger.warning(msg);
    }

    public void throwing(final String sourceClass, final String sourceMethod, final Throwable thrown) {
        logger.throwing(sourceClass, sourceMethod, thrown);
    }

    public void severe(final String msg) {
        logger.severe(msg);
    }

    public void setUseParentHandlers(final boolean useParentHandlers) {
        logger.setUseParentHandlers(useParentHandlers);
    }

    public void setParent(final Logger parent) {
        logger.setParent(parent);
    }

    public void setLevel(final Level newLevel) throws SecurityException {
        logger.setLevel(newLevel);
    }

    public void setFilter(final Filter newFilter) throws SecurityException {
        logger.setFilter(newFilter);
    }

    public void removeHandler(final Handler handler) throws SecurityException {
        logger.removeHandler(handler);
    }

    public void logrb(final Level level,
                      final String sourceClass,
                      final String sourceMethod,
                      final String bundleName,
                      final String msg,
                      final Throwable thrown) {
        logger.logrb(level, sourceClass, sourceMethod, bundleName, msg, thrown);
    }

    public void logrb(final Level level,
                      final String sourceClass,
                      final String sourceMethod,
                      final String bundleName,
                      final String msg,
                      final Object[] params) {
        logger.logrb(level, sourceClass, sourceMethod, bundleName, msg, params);
    }

    public void logrb(final Level level,
                      final String sourceClass,
                      final String sourceMethod,
                      final String bundleName,
                      final String msg,
                      final Object param1) {
        logger.logrb(level, sourceClass, sourceMethod, bundleName, msg, param1);
    }

    public void logrb(final Level level,
                      final String sourceClass,
                      final String sourceMethod,
                      final String bundleName,
                      final String msg) {
        logger.logrb(level, sourceClass, sourceMethod, bundleName, msg);
    }

    public void logp(final Level level,
                     final String sourceClass,
                     final String sourceMethod,
                     final String msg,
                     final Throwable thrown) {
        logger.logp(level, sourceClass, sourceMethod, msg, thrown);
    }

    public void logp(final Level level,
                     final String sourceClass,
                     final String sourceMethod,
                     final String msg,
                     final Object[] params) {
        logger.logp(level, sourceClass, sourceMethod, msg, params);
    }

    public void logp(final Level level,
                     final String sourceClass,
                     final String sourceMethod,
                     final String msg,
                     final Object param1) {
        logger.logp(level, sourceClass, sourceMethod, msg, param1);
    }

    public void logp(final Level level, final String sourceClass, final String sourceMethod, final String msg) {
        logger.logp(level, sourceClass, sourceMethod, msg);
    }

    public void log(final Level level, final String msg, final Throwable thrown) {
        logger.log(level, msg, thrown);
    }

    public void log(final Level level, final String msg, final Object[] params) {
        logger.log(level, msg, params);
    }

    public void log(final Level level, final String msg, final Object param1) {
        logger.log(level, msg, param1);
    }

    public void log(final Level level, final String msg) {
        logger.log(level, msg);
    }

    public void log(final LogRecord record) {
        logger.log(record);
    }

    public boolean isLoggable(final Level level) {
        return logger.isLoggable(level);
    }

    public void info(final String msg) {
        logger.info(msg);
    }

    public boolean getUseParentHandlers() {
        return logger.getUseParentHandlers();
    }

    public String getResourceBundleName() {
        return logger.getResourceBundleName();
    }

    public ResourceBundle getResourceBundle() {
        return logger.getResourceBundle();
    }

    public Logger getParent() {
        return logger.getParent();
    }

    public String getName() {
        return logger.getName();
    }

    public Level getLevel() {
        return logger.getLevel();
    }

    public Handler[] getHandlers() {
        return logger.getHandlers();
    }

    public Filter getFilter() {
        return logger.getFilter();
    }

    public void finest(final String msg) {
        logger.finest(msg);
    }

    public void finer(final String msg) {
        logger.finer(msg);
    }

    public void fine(final String msg) {
        logger.fine(msg);
    }

    public void exiting(final String sourceClass, final String sourceMethod, final Object result) {
        logger.exiting(sourceClass, sourceMethod, result);
    }

    public void exiting(final String sourceClass, final String sourceMethod) {
        logger.exiting(sourceClass, sourceMethod);
    }

    public void entering(final String sourceClass, final String sourceMethod, final Object[] params) {
        logger.entering(sourceClass, sourceMethod, params);
    }

    public void entering(final String sourceClass, final String sourceMethod, final Object param1) {
        logger.entering(sourceClass, sourceMethod, param1);
    }

    public void entering(final String sourceClass, final String sourceMethod) {
        logger.entering(sourceClass, sourceMethod);
    }

    public void config(final String msg) {
        logger.config(msg);
    }

    public void addHandler(final Handler handler) throws SecurityException {
        logger.addHandler(handler);
    }
}
