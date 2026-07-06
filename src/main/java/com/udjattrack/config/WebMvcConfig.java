package com.udjattrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${application.storage.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        String absoluteUploadPath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/files/**")
                .addResourceLocations(absoluteUploadPath)
                .setCachePeriod(3600); //cache for 1 hour on client side
    }
}
