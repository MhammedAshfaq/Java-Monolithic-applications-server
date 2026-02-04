package com.javainfraexample.spring_monolith_template.config;

import org.springframework.context.annotation.Configuration;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.config.annotation.PathMatchConfigurer;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.util.Set;

/**
 * Web MVC configuration to add global /api prefix to all REST controllers.
 * Excludes controllers with paths starting with /health from the prefix.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {
    
    private static final String API_PREFIX = "/api";
    
    // Paths that should NOT have the /api prefix
    private static final Set<String> EXCLUDED_PATHS = Set.of("/health");
    
    @Override
    public void configurePathMatch(PathMatchConfigurer configurer) {
        // Add /api prefix to all @RestController endpoints, except those in excluded paths
        configurer.addPathPrefix(API_PREFIX, c -> {
            if (!c.isAnnotationPresent(org.springframework.web.bind.annotation.RestController.class)) {
                return false;
            }
            // Dev dashboard and similar non-API controllers: no /api prefix
            if (c.getPackageName().startsWith("com.javainfraexample.spring_monolith_template.api.dev")) {
                return false;
            }
            // Check if the controller has a RequestMapping with an excluded path
            RequestMapping requestMapping = c.getAnnotation(RequestMapping.class);
            if (requestMapping != null) {
                String[] paths = requestMapping.value();
                for (String path : paths) {
                    if (EXCLUDED_PATHS.contains(path)) {
                        return false;
                    }
                }
            }
            return true;
        });
    }
}
