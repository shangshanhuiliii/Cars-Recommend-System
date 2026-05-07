package com.carsrecommend.system.config;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.web.servlet.config.annotation.ResourceHandlerRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;
import org.springframework.web.servlet.resource.PathResourceResolver;

@Configuration
@EnableConfigurationProperties(CarImageStorageProperties.class)
public class CarImageResourceConfig implements WebMvcConfigurer {

    private final CarImageStorageProperties properties;

    public CarImageResourceConfig(CarImageStorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public void addResourceHandlers(ResourceHandlerRegistry registry) {
        registry.addResourceHandler(properties.publicPathPattern())
                .addResourceLocations(properties.resolveCarImageRoot().toUri().toString())
                .resourceChain(true)
                .addResolver(new SafeCarImagePathResolver(properties));
    }

    private static class SafeCarImagePathResolver extends PathResourceResolver {

        private final CarImageStorageProperties properties;

        private SafeCarImagePathResolver(CarImageStorageProperties properties) {
            this.properties = properties;
        }

        @Override
        protected Resource getResource(String resourcePath, Resource location) throws IOException {
            Path root = storageRoot();
            Path resolved = root.resolve(resourcePath).normalize();
            if (!resolved.startsWith(root)) {
                return null;
            }
            if (!Files.exists(resolved) || !Files.isRegularFile(resolved)) {
                return null;
            }
            Path realRoot = root.toRealPath();
            Path realResource = resolved.toRealPath();
            if (!realResource.startsWith(realRoot)) {
                return null;
            }
            Resource resource = resource(realResource);
            return resource.exists() && resource.isReadable() ? resource : null;
        }

        private Path storageRoot() throws IOException {
            Path root = properties.resolveCarImageRoot();
            Files.createDirectories(root);
            return root;
        }

        private Resource resource(Path path) throws MalformedURLException {
            return new UrlResource(path.toUri());
        }
    }
}
