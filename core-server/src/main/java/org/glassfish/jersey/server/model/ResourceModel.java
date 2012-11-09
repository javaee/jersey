package org.glassfish.jersey.server.model;

import java.util.List;


/**
 * Resource model of the deployed application which contains set of root resources. As it implements {@link
 * ResourceModelComponent} it can be validated by {@link ComponentModelValidator component model validator} which will perform
 * validation of the entire resource model including all sub components ({@link Resource resources},
 * {@link ResourceMethod resource methods} ...).
 *
 * @author Miroslav Fuksa (miroslav.fuksa at oracle.com)
 *
 */
public class ResourceModel implements ResourceModelComponent {
    private final List<Resource> resources;

    /**
     * Creates new instance from root resources.
     * @param resources Root resource of the resource model.
     */
    public ResourceModel(List<Resource> resources) {
        this.resources = resources;
    }

    public List<Resource> getResources() {
        return resources;
    }

    @Override
    public void accept(ResourceModelVisitor visitor) {
        visitor.visitResourceModel(this);
    }

    @Override
    public List<? extends ResourceModelComponent> getComponents() {
        return resources;
    }
}