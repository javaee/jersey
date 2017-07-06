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

package org.glassfish.jersey.linking.contributing;

import java.lang.annotation.Annotation;
import java.lang.reflect.AnnotatedElement;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.logging.Level;
import java.util.logging.Logger;

import javax.ws.rs.core.Context;

import org.glassfish.jersey.linking.ProvideLink;
import org.glassfish.jersey.linking.ProvideLinkDescriptor;
import org.glassfish.jersey.linking.ProvideLinks;
import org.glassfish.jersey.server.ExtendedResourceContext;
import org.glassfish.jersey.server.model.HandlerConstructor;
import org.glassfish.jersey.server.model.Invocable;
import org.glassfish.jersey.server.model.MethodHandler;
import org.glassfish.jersey.server.model.Resource;
import org.glassfish.jersey.server.model.ResourceMethod;
import org.glassfish.jersey.server.model.ResourceModel;
import org.glassfish.jersey.server.model.ResourceModelComponent;
import org.glassfish.jersey.server.model.ResourceModelVisitor;
import org.glassfish.jersey.server.model.RuntimeResource;

/**
 * Simple map based implementation of the ResourceLinkContributionContext.
 *
 * @author Leonard Brünings
 */
public class NaiveResourceLinkContributionContext implements ResourceLinkContributionContext {

    private final ExtendedResourceContext erc;

    /**
     * Mappings holds a single-level mapping between a class, and it's {@link ProvideLinkDescriptor}s
     */
    private Map<Class<?>, List<ProvideLinkDescriptor>> mappings;

    /**
     * Contributions holds all contributions for a class, this includes all contributions from it's ancestors.
     */
    private Map<Class<?>, List<ProvideLinkDescriptor>> contributions = new ConcurrentHashMap<>();

    /**
     * C'tor
     * @param erc the ExtendedResourceContext
     */
    public NaiveResourceLinkContributionContext(@Context final ExtendedResourceContext erc) {
        this.erc = erc;
    }

    @Override
    public List<ProvideLinkDescriptor> getContributorsFor(Class<?> entityClass) {
        buildMappings();
        return contributions.computeIfAbsent(entityClass,
                aClass -> Collections.unmodifiableList(collectContributors(aClass, new ArrayList<>())));
    }

    /**
     * Collects contributors for a class recursively up the ancestors chain.
     *
     * @param entityClass the class to
     * @param contributors collector list
     * @return contributors list for easier use in lambdas
     */
    private List<ProvideLinkDescriptor> collectContributors(Class<?> entityClass, List<ProvideLinkDescriptor> contributors) {
        contributors.addAll(mappings.getOrDefault(entityClass, Collections.emptyList()));
        Class<?> sc = entityClass.getSuperclass();
        if ((sc != null) && (sc != Object.class)) {
            collectContributors(sc, contributors);
        }
        for (Class<?> ic : entityClass.getInterfaces()) {
            collectContributors(ic, contributors);
        }
        return contributors;
    }

    private void buildMappings() {
        if (mappings != null) {
            return;
        }
        final Map<Class<?>, List<ProvideLinkDescriptor>> newMappings = new HashMap<>();

        erc.getResourceModel().accept(new ResourceModelVisitor() {

            private void processComponents(final ResourceModelComponent component) {

                final List<? extends ResourceModelComponent> components = component.getComponents();
                if (components != null) {
                    for (final ResourceModelComponent rc : components) {
                        rc.accept(this);
                    }
                }
            }

            @Override
            public void visitInvocable(final Invocable invocable) {
                processComponents(invocable);
            }

            @Override
            public void visitRuntimeResource(final RuntimeResource runtimeResource) {
                processComponents(runtimeResource);
            }

            @Override
            public void visitResourceModel(final ResourceModel resourceModel) {
                processComponents(resourceModel);
            }

            @Override
            public void visitResourceHandlerConstructor(final HandlerConstructor handlerConstructor) {
                processComponents(handlerConstructor);
            }

            @Override
            public void visitMethodHandler(final MethodHandler methodHandler) {
                processComponents(methodHandler);
            }

            @Override
            public void visitChildResource(final Resource resource) {
                processComponents(resource);
            }

            @Override
            public void visitResource(final Resource resource) {
                processComponents(resource);
            }

            @Override
            public void visitResourceMethod(final ResourceMethod resourceMethod) {

                if (resourceMethod.isExtended()) {
                    return;
                }

                if (resourceMethod.getInvocable() != null) {
                    final Invocable i = resourceMethod.getInvocable();

                    Method method = i.getDefinitionMethod();

                    List<ProvideLinkDescriptor> linkDescriptors = new ArrayList<>();

                    handleMetaAnnotations(resourceMethod, method, linkDescriptors);

                    handleAnnotations(resourceMethod, linkDescriptors, method, null);

                    for (ProvideLinkDescriptor linkDescriptor : linkDescriptors) {
                        for (Class<?> target : linkDescriptor.getProvideLink().value()) {
                            target = handleInheritedTarget(linkDescriptor, target);
                            newMappings.computeIfAbsent(target, aClass -> new ArrayList<>()).add(linkDescriptor);
                        }
                    }
                }

                processComponents(resourceMethod);
            }

            private Class<?> handleInheritedTarget(ProvideLinkDescriptor linkDescriptor, Class<?> target) {
                if (Objects.equals(ProvideLink.InheritFromAnnotation.class, target)) {
                    Annotation parentAnnotation = linkDescriptor.getParentAnnotation();
                    if (parentAnnotation == null) {
                        throw new IllegalArgumentException("InheritFromAnnotation can only be used for Annotations");
                    }
                    return findTarget(parentAnnotation);
                }
                return target;
            }

            private Class<?> findTarget(Annotation parentAnnotation) {
                Method[] methods = parentAnnotation.annotationType().getDeclaredMethods();
                for (Method method : methods) {
                    if (method.isAccessible() || Class.class.isAssignableFrom(method.getReturnType())) {

                        try {
                            return (Class<?>) method.invoke(parentAnnotation);
                        } catch (IllegalArgumentException | IllegalAccessException | InvocationTargetException ex) {
                            Logger.getLogger(NaiveResourceLinkContributionContext.class.getName()).log(Level.FINE, null, ex);
                        }

                    }
                }
                throw new IllegalArgumentException("No suitable element of type Class<?> found on: "
                        + parentAnnotation.getClass());
            }

            private void handleMetaAnnotations(ResourceMethod resourceMethod, Method method,
                    List<ProvideLinkDescriptor> linkDescriptors) {
                Annotation[] annotations = method.getDeclaredAnnotations();
                for (Annotation annotation : annotations) {
                    handleAnnotations(resourceMethod, linkDescriptors, annotation.annotationType(), annotation);
                }
            }

            private void handleAnnotations(ResourceMethod resourceMethod, List<ProvideLinkDescriptor> linkDescriptors,
                    AnnotatedElement element, Annotation parentAnnotation) {
                if (element.isAnnotationPresent(ProvideLink.class) || element.isAnnotationPresent(ProvideLinks.class)) {
                    ProvideLink provideLink = element.getAnnotation(ProvideLink.class);
                    if (provideLink != null) {
                        linkDescriptors.add(new ProvideLinkDescriptor(resourceMethod, provideLink, parentAnnotation));
                    }
                    ProvideLinks provideLinks = element.getAnnotation(ProvideLinks.class);
                    if (provideLinks != null) {
                        for (ProvideLink link : provideLinks.value()) {
                            linkDescriptors.add(new ProvideLinkDescriptor(resourceMethod, link, parentAnnotation));
                        }
                    }

                }
            }

        });

        mappings = newMappings;
    }
}
