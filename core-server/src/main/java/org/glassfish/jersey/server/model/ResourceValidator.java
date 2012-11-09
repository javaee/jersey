package org.glassfish.jersey.server.model;

import org.glassfish.jersey.server.internal.LocalizationMessages;
import org.glassfish.jersey.spi.Errors;

/**
 * Validator ensuring that resource are correct (for example that root resources contains path, etc.).
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
class ResourceValidator extends AbstractResourceModelVisitor {

    @Override
    public void visitResource(final Resource resource) {
        checkResource(resource);
    }

    private void checkResource(final Resource resource) {
        // uri template of the resource, if present should not contain a null value
        if (resource.isRootResource() && (null == resource.getPath())) {
            // TODO: is it really a fatal issue?
            Errors.fatal(resource, LocalizationMessages.RES_URI_PATH_INVALID(resource.getName(), resource.getPath()));
        }

        if (!resource.getResourceMethods().isEmpty() && resource.getResourceLocator() != null) {
            Errors.warning(resource, LocalizationMessages.RESOURCE_CONTAINS_RES_METHODS_AND_LOCATOR(resource,
                    resource.getPath()));
        }

        if (resource.isRootResource() && resource.getResourceMethods().isEmpty() && resource.getChildResources()
                .isEmpty() &&
                resource.getResourceLocator() == null) {
            Errors.warning(resource, LocalizationMessages.RESOURCE_EMPTY(resource, resource.getPath()));
        }

    }

    @Override
    public void visitChildResource(Resource resource) {
        checkResource(resource);
    }
}