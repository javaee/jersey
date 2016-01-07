package org.glassfish.jersey.ext.mvc.jmustache;

import com.samskivert.mustache.Mustache;
import com.samskivert.mustache.Template;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.nio.charset.Charset;
import javax.inject.Inject;
import javax.inject.Singleton;
import javax.servlet.ServletContext;
import javax.ws.rs.core.Configuration;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.MultivaluedMap;
import org.glassfish.hk2.api.ServiceLocator;
import org.glassfish.jersey.server.mvc.Viewable;
import org.glassfish.jersey.server.mvc.spi.AbstractTemplateProcessor;
import org.jvnet.hk2.annotations.Optional;

/**
 *
 * @author Anshul Sao <anshul.sao at capillarytech.com>
 */
@Singleton
public class JMustacheTemplateProcessor extends
    AbstractTemplateProcessor<Template> {

    @Inject
    public JMustacheTemplateProcessor(final Configuration config,
        final ServiceLocator serviceLocator,
        @Optional final ServletContext servletContext) {
        super(config, servletContext, "jmustache", "mustache", "xml", "json", "html");
    }

    @Override
    public void writeTo(Template template, Viewable viewable,
        MediaType mediaType, MultivaluedMap<String, Object> httpHeaders,
        OutputStream out) throws IOException {
        Charset encoding = setContentType(mediaType, httpHeaders);
        OutputStreamWriter outputStreamWriter =
            new OutputStreamWriter(out, encoding);
        template.execute(viewable.getModel(), outputStreamWriter);
        outputStreamWriter.flush();
    }

    @Override
    protected Template resolve(String templatePath, Reader reader)
        throws Exception {
        Mustache.Compiler compiler = Mustache.compiler().defaultValue("");
        return compiler.compile(reader);
    }

}
