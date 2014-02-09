package org.glassfish.jersey.message.filtering;

import java.lang.annotation.Annotation;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.inject.Singleton;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.UriInfo;

import jersey.repackaged.com.google.common.collect.Sets;

import org.glassfish.jersey.internal.util.Tokenizer;
import org.glassfish.jersey.message.filtering.spi.ScopeResolver;

@Singleton
public class SelectableScopeResolver implements ScopeResolver {

    // TODO make configurable
    private static String SELECTABLE_PARAM_NAME = "select";
    
    public static String PREFIX = SelectableScopeResolver.class.getName() + "_";
    
    public static String DEFAULT_SCOPE = PREFIX + "*";

    @Context
    private UriInfo uriInfo;

    @Override
    public Set<String> resolve(Annotation[] annotations) {
        Set<String> scopes = new HashSet<String>();

        List<String> fields = uriInfo.getQueryParameters().get(SELECTABLE_PARAM_NAME);
        if (fields != null && !fields.isEmpty()) {
            for (String field : fields) {
                scopes.addAll(getScopesForField(field));
            }
        }
        else {
            scopes.add(DEFAULT_SCOPE);
        }
        return scopes;
    }

    private Set<String> getScopesForField(String fieldName) {
        Set<String> scopes = Sets.newHashSet();

        // add specific scope in case of specific request
        String[] fields = Tokenizer.tokenize(fieldName, ",");
        for (String field : fields) {
        	String[] subfields = Tokenizer.tokenize(field, ".");
        	// in case of nested path, add first level as stand-alone to ensure subgraph is added
    		scopes.add(SelectableScopeResolver.PREFIX + subfields[0]);
        	if(subfields.length > 1){
        		scopes.add(SelectableScopeResolver.PREFIX + field);
        	}
        }

        return scopes;
    }
}
