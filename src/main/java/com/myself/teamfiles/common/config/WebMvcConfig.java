package com.myself.teamfiles.common.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.web.servlet.config.annotation.CorsRegistry;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

import java.io.File;
import java.io.IOException;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${app.frontend-dist:./teamfiles-web/dist}")
    private String frontendDist;

    @Override
    public void addCorsMappings(CorsRegistry registry) {
        registry.addMapping("/**")
                .allowedOriginPatterns("*")
                .allowedMethods("GET", "POST", "PUT", "DELETE", "OPTIONS")
                .allowedHeaders("*")
                .allowCredentials(true)
                .maxAge(3600);
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        File dist = new File(frontendDist);
        if (dist.exists() && dist.isDirectory()) {
            registry.addResourceHandler("/**")
                    .addResourceLocations("file:" + dist.getAbsolutePath() + "/")
                    .setCachePeriod(3600)
                    .resourceChain(true)
                    .addResolver(new PathResourceResolver() {
                        @Override
                        protected Resource getResource(String resourcePath, Resource location) throws IOException {
                            Resource requested = location.createRelative(resourcePath);
                            if (requested.isReadable()) {
                                return requested;
                            }
                            return location.createRelative("index.html");
                        }
                    });
        }
    }
}
