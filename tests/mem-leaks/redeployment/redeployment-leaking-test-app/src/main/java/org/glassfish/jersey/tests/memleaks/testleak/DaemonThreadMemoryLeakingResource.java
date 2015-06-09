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
package org.glassfish.jersey.tests.memleaks.testleak;

import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import javax.ws.rs.DefaultValue;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.QueryParam;

import javax.inject.Singleton;

import com.google.common.util.concurrent.ThreadFactoryBuilder;

/**
 * Resource that causes {@link OutOfMemoryError} exception upon repetitive call of {@link #invoke(int)} of an application that is
 * being redeployed.
 * <p/>
 * The purpose of this resource (and the app) is to test whether the memory leaking infrastructure for redeployment scenarios
 * works.
 *
 * @author Stepan Vavra (stepan.vavra at oracle.com)
 */
@Path("/")
@Singleton
public class DaemonThreadMemoryLeakingResource {

    @POST
    @Path("invoke")
    public String invoke(@DefaultValue("1048576") @QueryParam("size") final int size) {

        final Future<?> future = Executors.newSingleThreadExecutor(new ThreadFactoryBuilder().setDaemon(true).build())
                .submit(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            System.out.println("Running a Thread!");
                            final int mbytes = size / (1024 * 1024);
                            final byte[][] bytes = new byte[mbytes][];
                            for (int i = 1; i <= mbytes; ++i) {
                                bytes[i - 1] = new byte[1024 * 1024];
                                System.out.println("Allocated: " + i + "MB!");
                            }

                            System.out.println("Memory allocated! Total: " + mbytes + "MB! Sleeping...");
                            for (int i = 0; i < 1000000; ++i) {
                                System.out.println("Thread " + Thread.currentThread() + " sleeping!");
                                Thread.sleep(10000);
                            }
                            System.out.println("Freeing: " + size + " of bytes. " + bytes);
                        } catch (InterruptedException e) {
                            throw new IllegalStateException("Thread Interrupted!", e);
                        } catch (Throwable e) {
                            e.printStackTrace();
                            throw e;
                        }

                    }
                });

        System.out.println("Trying to allocate bytes from the thread itself.");
        final byte[] bytes = new byte[size];
        return "Future submitted: " + future + " bytes allocated: " + bytes;
    }

    @GET
    @Path("hello")
    @Produces("text/plain")
    public String helloWorld() {
        System.out.println("HELLO WORLD!");
        return "HELLO WORLD!";
    }

}