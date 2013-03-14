/*
 * DO NOT ALTER OR REMOVE COPYRIGHT NOTICES OR THIS HEADER.
 *
 * Copyright (c) 2010-2013 Oracle and/or its affiliates. All rights reserved.
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
package org.glassfish.jersey.server.mvc.jsp;

import java.io.IOException;
import java.io.PrintWriter;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpServletResponseWrapper;
import javax.servlet.jsp.JspException;
import javax.servlet.jsp.PageContext;
import javax.servlet.jsp.tagext.SimpleTagSupport;

/**
 * Includes a side JSP file for the {@code resolvingClass} class.
 * <p/>
 * This tag looks for a side JSP file of the given name
 * from the inheritance hierarchy of the "resolvingClass" class,
 * and includes the contents of it, just like &lt;jsp:include>.
 * <p/>
 * For example, if the {@code resolvingClass} class is the {@code Foo} class,
 * which looks like the following:
 * <pre>
 * class Foo extends Bar { ... }
 * class Bar extends Zot { ... }
 * </pre>
 * <p/>
 * And if you write:
 * <pre>
 * &lt;st:include page="abc.jsp"/&gt;
 * </pre>
 * then, it looks for the following files in this order,
 * and includes the first one found.
 * <ol>
 * <li>a side-file of the {@code Foo} class named {@code abc.jsp} ({@code /WEB-INF/Foo/abc.jsp})
 * <li>a side-file of the {@code Bar} class named {@code abc.jsp} ({@code /WEB-INF/Bar/abc.jsp})
 * <li>a side-file of the {@code Zot} class named {@code abc.jsp} ({@code /WEB-INF/Zot/abc.jsp})
 * </ol>
 *
 * @author Kohsuke Kawaguchi
 * @author Paul Sandoz (paul.sandoz at oracle.com)
 */
public class Include extends SimpleTagSupport {

    private Class<?> resolvingClass;
    private String page;

    /**
     * Specifies the name of the JSP to be included.
     */
    public void setPage(String page) {
        this.page = page;
    }

    /**
     * Specifies the resolving class for which JSP will be included.
     */
    public void setResolvingClass(Class<?> resolvingClass) {
        this.resolvingClass = resolvingClass;
    }

    private Object getPageObject(String name) {
        return getJspContext().getAttribute(name, PageContext.PAGE_SCOPE);
    }

    public void doTag() throws JspException, IOException {
        Class<?> resolvingClass = (Class<?>) getJspContext().getAttribute("resolvingClass", PageContext.REQUEST_SCOPE);
        final Class<?> oldResolvingClass = resolvingClass;
        if (this.resolvingClass != null) {
            resolvingClass = this.resolvingClass;
        }

        ServletConfig cfg = (ServletConfig) getPageObject(PageContext.CONFIG);
        ServletContext sc = cfg.getServletContext();

        String basePath = (String) getJspContext().getAttribute("_basePath", PageContext.REQUEST_SCOPE);
        for (Class c = resolvingClass; c != Object.class; c = c.getSuperclass()) {
            String name = basePath + "/" + c.getName().replace('.', '/') + '/' + page;
            if (sc.getResource(name) != null) {
                // Tomcat returns a RequestDispatcher even if the JSP file doesn't exist.
                // so check if the resource exists first.
                RequestDispatcher disp = sc.getRequestDispatcher(name);
                if (disp != null) {
                    getJspContext().setAttribute("resolvingClass", resolvingClass, PageContext.REQUEST_SCOPE);
                    try {
                        HttpServletRequest request = (HttpServletRequest) getPageObject(PageContext.REQUEST);
                        disp.include(request, new Wrapper((HttpServletResponse) getPageObject(PageContext.RESPONSE),
                                new PrintWriter(getJspContext().getOut())));
                    } catch (ServletException e) {
                        throw new JspException(e);
                    } finally {
                        getJspContext().setAttribute("resolvingClass", oldResolvingClass, PageContext.REQUEST_SCOPE);
                    }
                    return;
                }
            }
        }

        throw new JspException("Unable to find '" + page + "' for " + resolvingClass);
    }
}

class Wrapper extends HttpServletResponseWrapper {

    private final PrintWriter pw;

    public Wrapper(HttpServletResponse httpServletResponse, PrintWriter w) {
        super(httpServletResponse);
        this.pw = w;
    }

    public PrintWriter getWriter() throws IOException {
        return pw;
    }
}