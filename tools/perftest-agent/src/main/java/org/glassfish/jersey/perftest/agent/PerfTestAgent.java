/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2015 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.perftest.agent;

import java.lang.instrument.ClassFileTransformer;
import java.lang.instrument.IllegalClassFormatException;
import java.lang.instrument.Instrumentation;

import java.security.ProtectionDomain;

import javassist.ClassPool;
import javassist.CtClass;
import javassist.CtField;
import javassist.CtMethod;

/**
 * Java agent that instruments any Jersey 2 ApplicationHandler found with a new
 * Metrics timer. If parameter is given, it will be understood as another class.method
 * (delimited by a single dot) that has to be instrumented instead of the Jersey handler.
 *
 * This one has been tested with Grizzly 2, Catalina and WLS.
 *
 * @author Jakub Podlesak (jakub.podlesak at oracle.com)
 */
public class PerfTestAgent {

    private static final String HANDLER_CLASS_NAME = "org.glassfish.jersey.server.ApplicationHandler";
    private static final String HANDLER_METHOD_NAME = "handle";

    public static void premain(String agentArgs, Instrumentation instrumentation) {

        final String handlerClassName = (agentArgs != null && !agentArgs.isEmpty()) ? agentArgs.substring(0, agentArgs.lastIndexOf('.')) : HANDLER_CLASS_NAME;
        final String handlerMethodName = (agentArgs != null && !agentArgs.isEmpty()) ? agentArgs.substring(agentArgs.lastIndexOf('.') + 1) : HANDLER_METHOD_NAME;

        instrumentation.addTransformer(new ClassFileTransformer() {

            @Override
            public byte[] transform(ClassLoader loader, String className, Class<?> aClass, ProtectionDomain protectionDomain, byte[] bytes) throws IllegalClassFormatException {
                if (handlerClassName.replaceAll("\\.", "/").equals(className)) {
                    try {
                        ClassPool cp = ClassPool.getDefault();
                        cp.appendSystemPath();
                        CtClass cc = cp.makeClass(new java.io.ByteArrayInputStream(bytes));

                        final CtField ctxField = CtField.make("public static final agent.metrics.Timer.Context agentTimerCtx;", cc);
                        final CtField registryField = CtField.make("public static final agent.metrics.MetricRegistry agentREG = new agent.metrics.MetricRegistry();", cc);
                        final CtField reporterField = CtField.make("public static final agent.metrics.JmxReporter agentReporter = agent.metrics.JmxReporter.forRegistry(agentREG).build();", cc);
                        final CtField timerField = CtField.make("public static final agent.metrics.Timer agentTimer = "
                                + "agentREG.timer(agent.metrics.MetricRegistry.name(\"" + handlerClassName + "\", new String[] {\"" + handlerMethodName + "\"}));", cc);
                        cc.addField(registryField);
                        cc.addField(reporterField);
                        cc.addField(timerField);
                        cc.makeClassInitializer().insertAfter("agentReporter.start();");

                        CtMethod m = cc.getDeclaredMethod(handlerMethodName);
                        m.addLocalVariable("agentCtx", ctxField.getType());
                        m.insertBefore("agentCtx = agentTimer.time();");
                        m.insertAfter("agentCtx.stop();", true);
                        byte[] byteCode = cc.toBytecode();
                        cc.detach();
                        System.out.printf("Jersey Perf Agent Instrumentation Done! (instrumented method: %s)\n", m.getLongName());
                        return byteCode;
                    } catch (Exception ex) {
                        ex.printStackTrace();
                    }
                }
                return null;
            }
        });
    }
}