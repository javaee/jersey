package org.glassfish.jersey.server.model;

/**
 * Abstract implementation of {@link ResourceModelVisitor resource model visitor} containing empty implementations
 * of interface methods. This class can be derivered by validator implementing only methods needed for specific
 * validations.
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public abstract class AbstractResourceModelVisitor implements ResourceModelVisitor {
    @Override
    public void visitResource(Resource resource) {
    }

    @Override
    public void visitChildResource(Resource resource) {
    }

    @Override
    public void visitResourceMethod(ResourceMethod method) {
    }

    @Override
    public void visitInvocable(Invocable invocable) {
    }

    @Override
    public void visitMethodHandler(MethodHandler methodHandler) {
    }

    @Override
    public void visitResourceHandlerConstructor(HandlerConstructor constructor) {
    }

    @Override
    public void visitResourceModel(ResourceModel resourceModel) {
    }
}
