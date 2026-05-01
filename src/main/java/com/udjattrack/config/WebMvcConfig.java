package com.udjattrack.config;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

import java.nio.file.Paths;

/**
 * WebMvcConfig — configures Spring MVC to serve static files from the local
 * upload directory as public HTTP resources.
 *
 * <p>When a driver photo is uploaded to {@code /uploads/drivers/uuid.jpg},
 * it becomes accessible via {@code GET /api/v1/files/drivers/uuid.jpg}.
 *
 * <p>The path mappings are driven by the same {@code application.storage.*}
 * properties used by LocalFileStorageServiceImpl, keeping configuration DRY.
 */
@Configuration
public class WebMvcConfig implements WebMvcConfigurer {

    @Value("${application.storage.upload-dir:uploads}")
    private String uploadDir;

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        // Convert the upload directory to an absolute file URI so Spring can serve it
        String absoluteUploadPath = Paths.get(uploadDir).toAbsolutePath().toUri().toString();

        registry.addResourceHandler("/files/**")
                .addResourceLocations(absoluteUploadPath)
                .setCachePeriod(3600); // Cache for 1 hour on client side
    }
}
