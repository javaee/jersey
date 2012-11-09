package org.glassfish.jersey.server.model;

import java.util.Map;

import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.spi.Errors;
import org.glassfish.jersey.uri.PathPattern;

import com.google.common.collect.Maps;

/**
 * Validator ensuring that resource model is correct (for example that resources do not have ambiguous path).
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
class ResourceModelValidator extends AbstractResourceModelVisitor {

    @Override
    public void visitResourceModel(ResourceModel resourceModel) {
        Map<PathPattern, Resource> resourceMap = Maps.newHashMap();
        for (Resource resource : resourceModel.getResources()) {
            final PathPattern pathPattern = resource.getPathPattern();
            final Resource resourceFromMap = resourceMap.get(pathPattern);
            if (resourceFromMap != null) {
                Errors.error(resource, LocalizationMessages.RESOURCE_AMBIGUOUS(resource, resourceFromMap,
                        pathPattern), true);
            }
            resourceMap.put(pathPattern, resource);
        }
    }
}