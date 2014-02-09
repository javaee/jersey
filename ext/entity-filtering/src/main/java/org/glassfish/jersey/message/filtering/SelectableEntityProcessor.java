package org.glassfish.jersey.message.filtering;

import java.lang.annotation.Annotation;
import java.util.Set;

import javax.annotation.Priority;
import javax.inject.Singleton;

import jersey.repackaged.com.google.common.collect.Sets;

import org.glassfish.jersey.message.filtering.spi.AbstractEntityProcessor;
import org.glassfish.jersey.message.filtering.spi.EntityGraph;
import org.glassfish.jersey.message.filtering.spi.EntityProcessor;

@Singleton
@Priority(Integer.MAX_VALUE - 5000)
public class SelectableEntityProcessor extends AbstractEntityProcessor {

	protected Result process(String fieldName, Class<?> fieldClass, Annotation[] fieldAnnotations,
            Annotation[] annotations, EntityGraph graph) {

        if (fieldName != null) {
            Set<String> scopes = Sets.newHashSet();

            // add default selectable scope in case of none requested
            scopes.add(SelectableScopeResolver.DEFAULT_SCOPE);
            
            // add specific scope in case of specific request
            scopes.add(SelectableScopeResolver.PREFIX + fieldName);

            addFilteringScopes(fieldName, fieldClass, scopes, graph);
        }

        return EntityProcessor.Result.APPLY;
    }

}
