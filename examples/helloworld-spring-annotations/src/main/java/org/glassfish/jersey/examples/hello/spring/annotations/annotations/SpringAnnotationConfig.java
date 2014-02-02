package org.glassfish.jersey.examples.hello.spring.annotations.annotations;

import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.Configuration;

/**
 * Spring configuration to include our services
 *
 * @author Geoffroy Warin (http://geowarin.github.io)
 */
@Configuration
@ComponentScan(basePackageClasses = {GreetingService.class})
public class SpringAnnotationConfig {
}
