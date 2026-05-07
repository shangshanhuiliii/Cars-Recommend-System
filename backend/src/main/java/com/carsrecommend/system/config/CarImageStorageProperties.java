package com.carsrecommend.system.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.util.StringUtils;

@ConfigurationProperties(prefix = "app.storage")
public class CarImageStorageProperties {

    private String carImageRoot = ".run/uploads/car-images";
    private String carImagePublicPath = "/uploads/car-images";
    private long carImageMaxSizeBytes = 5L * 1024L * 1024L;
    private int carImageMaxEdge = 1600;
    private float carImageJpegQuality = 0.82F;

    public String getCarImageRoot() {
        return carImageRoot;
    }

    public void setCarImageRoot(String carImageRoot) {
        this.carImageRoot = carImageRoot;
    }

    public String getCarImagePublicPath() {
        return carImagePublicPath;
    }

    public void setCarImagePublicPath(String carImagePublicPath) {
        this.carImagePublicPath = carImagePublicPath;
    }

    public long getCarImageMaxSizeBytes() {
        return carImageMaxSizeBytes;
    }

    public void setCarImageMaxSizeBytes(long carImageMaxSizeBytes) {
        this.carImageMaxSizeBytes = carImageMaxSizeBytes;
    }

    public int getCarImageMaxEdge() {
        return carImageMaxEdge;
    }

    public void setCarImageMaxEdge(int carImageMaxEdge) {
        this.carImageMaxEdge = carImageMaxEdge;
    }

    public float getCarImageJpegQuality() {
        return carImageJpegQuality;
    }

    public void setCarImageJpegQuality(float carImageJpegQuality) {
        this.carImageJpegQuality = carImageJpegQuality;
    }

    public Path resolveCarImageRoot() {
        String root = StringUtils.hasText(carImageRoot) ? carImageRoot.trim() : ".run/uploads/car-images";
        return Paths.get(root).toAbsolutePath().normalize();
    }

    public String normalizedPublicPath() {
        String path = StringUtils.hasText(carImagePublicPath)
                ? carImagePublicPath.trim().replace('\\', '/')
                : "/uploads/car-images";
        if (!path.startsWith("/")) {
            path = "/" + path;
        }
        while (path.endsWith("/") && path.length() > 1) {
            path = path.substring(0, path.length() - 1);
        }
        return path;
    }

    public String publicPathPattern() {
        return normalizedPublicPath() + "/**";
    }
}
