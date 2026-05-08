package com.carsrecommend.system.service.impl;

import com.carsrecommend.system.auth.AuthContext;
import com.carsrecommend.system.common.BusinessException;
import com.carsrecommend.system.common.ErrorCode;
import com.carsrecommend.system.common.PageResult;
import com.carsrecommend.system.common.enums.AuditStatus;
import com.carsrecommend.system.config.CarImageStorageProperties;
import com.carsrecommend.system.dto.CarImageAssetQuery;
import com.carsrecommend.system.dto.CarImageAuditRequest;
import com.carsrecommend.system.dto.CarImageUploadRequest;
import com.carsrecommend.system.entity.CarImageAsset;
import com.carsrecommend.system.entity.CarModel;
import com.carsrecommend.system.mapper.CarImageAssetMapper;
import com.carsrecommend.system.mapper.CarModelMapper;
import com.carsrecommend.system.service.CarImageAssetService;
import com.carsrecommend.system.vo.CarImageAssetVO;
import java.awt.Color;
import java.awt.Graphics2D;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.HexFormat;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.UUID;
import javax.imageio.IIOImage;
import javax.imageio.ImageIO;
import javax.imageio.ImageReader;
import javax.imageio.ImageWriteParam;
import javax.imageio.ImageWriter;
import javax.imageio.stream.ImageInputStream;
import javax.imageio.stream.ImageOutputStream;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

@Service
@ConditionalOnProperty(prefix = "spring.datasource", name = "url")
public class CarImageAssetServiceImpl implements CarImageAssetService {

    private static final long DEFAULT_DEMO_ADMIN_ID = 1L;
    private static final String JPEG_CONTENT_TYPE = "image/jpeg";
    private static final String PNG_CONTENT_TYPE = "image/png";

    private final CarImageAssetMapper carImageAssetMapper;
    private final CarModelMapper carModelMapper;
    private final CarImageStorageProperties storageProperties;

    public CarImageAssetServiceImpl(
            CarImageAssetMapper carImageAssetMapper,
            CarModelMapper carModelMapper,
            CarImageStorageProperties storageProperties) {
        this.carImageAssetMapper = carImageAssetMapper;
        this.carModelMapper = carModelMapper;
        this.storageProperties = storageProperties;
    }

    @Override
    @Transactional
    public CarImageAssetVO upload(CarImageUploadRequest request) {
        ensureActiveCarExists(request.getCarId());
        StoredImage storedImage = storeImage(request.getFile(), request.getCarId());

        CarImageAsset asset = new CarImageAsset();
        asset.setCarId(request.getCarId());
        asset.setOriginalFilename(sanitizeOriginalFilename(request.getFile().getOriginalFilename(), storedImage.extension()));
        asset.setStoredFilename(storedImage.storedFilename());
        asset.setContentType(storedImage.contentType());
        asset.setSizeBytes(storedImage.sizeBytes());
        asset.setWidth(storedImage.width());
        asset.setHeight(storedImage.height());
        asset.setPublicUrl(storedImage.publicUrl());
        asset.setStoragePath(storedImage.storagePath());
        asset.setChecksum(storedImage.checksum());
        asset.setAuditStatus(AuditStatus.PENDING.getCode());
        asset.setCreatedByAdminId(currentAdminId());
        CarImageAsset created = carImageAssetMapper.insert(asset);
        return getExisting(created.getId());
    }

    @Override
    @Transactional(readOnly = true)
    public PageResult<CarImageAssetVO> page(CarImageAssetQuery query) {
        validateQuery(query);
        long total = carImageAssetMapper.count(query);
        List<CarImageAssetVO> records = carImageAssetMapper.page(query).stream()
                .map(this::toVO)
                .toList();
        return PageResult.of(records, total, query.getPage(), query.getSize());
    }

    @Override
    @Transactional
    public CarImageAssetVO audit(Long id, CarImageAuditRequest request) {
        CarImageAsset asset = findExisting(id);
        AuditStatus targetStatus = request.getAuditStatus();
        if (targetStatus == AuditStatus.PENDING) {
            throw new BusinessException("auditStatus must be APPROVED or REJECTED");
        }
        if (!AuditStatus.PENDING.getCode().equals(asset.getAuditStatus())) {
            throw new BusinessException("only pending image assets can be audited");
        }

        if (targetStatus == AuditStatus.APPROVED) {
            ensureActiveCarExists(asset.getCarId());
            if (carImageAssetMapper.updateAudit(
                    asset.getId(),
                    AuditStatus.APPROVED.getCode(),
                    null,
                    currentAdminId()) == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "car image asset not found");
            }
            if (carModelMapper.updateImageUrl(asset.getCarId(), asset.getPublicUrl()) == 0) {
                throw new BusinessException(ErrorCode.NOT_FOUND, "car model not found");
            }
            return getExisting(asset.getId());
        }

        String rejectReason = trimToNull(request.getRejectReason());
        if (!StringUtils.hasText(rejectReason)) {
            throw new BusinessException("rejectReason is required when auditStatus is REJECTED");
        }
        if (carImageAssetMapper.updateAudit(
                asset.getId(),
                AuditStatus.REJECTED.getCode(),
                rejectReason,
                currentAdminId()) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "car image asset not found");
        }
        return getExisting(asset.getId());
    }

    @Override
    @Transactional
    public void softDelete(Long id) {
        CarImageAsset asset = findExisting(id);
        carModelMapper.findById(asset.getCarId()).ifPresent(carModel -> {
            if (AuditStatus.APPROVED.getCode().equals(asset.getAuditStatus())
                    && asset.getPublicUrl().equals(carModel.getImageUrl())) {
                throw new BusinessException("approved image is currently used by the car model");
            }
        });
        if (carImageAssetMapper.softDelete(id) == 0) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "car image asset not found");
        }
    }

    private CarImageAssetVO getExisting(Long id) {
        return toVO(findExisting(id));
    }

    private Long currentAdminId() {
        Long currentAdminId = AuthContext.currentAdminIdOrNull();
        return currentAdminId == null ? DEFAULT_DEMO_ADMIN_ID : currentAdminId;
    }

    private CarImageAsset findExisting(Long id) {
        return carImageAssetMapper.findById(id)
                .orElseThrow(() -> new BusinessException(ErrorCode.NOT_FOUND, "car image asset not found"));
    }

    private void ensureActiveCarExists(Long carId) {
        if (!carModelMapper.existsActiveById(carId)) {
            throw new BusinessException(ErrorCode.NOT_FOUND, "car model not found");
        }
    }

    private void validateQuery(CarImageAssetQuery query) {
        if (StringUtils.hasText(query.getAuditStatus())) {
            AuditStatus.fromCode(query.getAuditStatus().trim());
        }
    }

    private StoredImage storeImage(MultipartFile file, Long carId) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("image file is required");
        }
        long maxSize = storageProperties.getCarImageMaxSizeBytes();
        if (file.getSize() > maxSize) {
            throw new BusinessException("image file size must not exceed 5MB");
        }

        try {
            byte[] sourceBytes = file.getBytes();
            if (sourceBytes.length > maxSize) {
                throw new BusinessException("image file size must not exceed 5MB");
            }
            DecodedImage decodedImage = decodeImage(sourceBytes);
            BufferedImage outputImage = resize(decodedImage.image(), decodedImage.format());
            byte[] outputBytes = writeImage(outputImage, decodedImage.format());
            String extension = extension(decodedImage.format());
            String contentType = contentType(decodedImage.format());
            String storedFilename = "car-" + carId + "-" + UUID.randomUUID().toString().replace("-", "") + "." + extension;
            Path root = storageRoot();
            Path target = root.resolve(storedFilename).normalize();
            if (!target.startsWith(root)) {
                throw new BusinessException("invalid image storage path");
            }
            Files.write(target, outputBytes, StandardOpenOption.CREATE_NEW);
            return new StoredImage(
                    storedFilename,
                    extension,
                    contentType,
                    (long) outputBytes.length,
                    outputImage.getWidth(),
                    outputImage.getHeight(),
                    storageProperties.normalizedPublicPath() + "/" + storedFilename,
                    root.relativize(target).toString().replace('\\', '/'),
                    checksum(outputBytes));
        } catch (BusinessException exception) {
            throw exception;
        } catch (IOException exception) {
            throw new BusinessException(ErrorCode.SYSTEM_ERROR, "failed to store car image");
        }
    }

    private Path storageRoot() throws IOException {
        Path root = storageProperties.resolveCarImageRoot();
        Files.createDirectories(root);
        return root;
    }

    private DecodedImage decodeImage(byte[] sourceBytes) throws IOException {
        try (ImageInputStream imageInputStream = ImageIO.createImageInputStream(new ByteArrayInputStream(sourceBytes))) {
            if (imageInputStream == null) {
                throw new BusinessException("unsupported image file");
            }
            Iterator<ImageReader> readers = ImageIO.getImageReaders(imageInputStream);
            if (!readers.hasNext()) {
                throw new BusinessException("unsupported image file");
            }
            ImageReader reader = readers.next();
            try {
                String format = normalizeFormat(reader.getFormatName());
                if (!isSupportedFormat(format)) {
                    throw new BusinessException("only JPEG and PNG images are allowed");
                }
                imageInputStream.seek(0);
                reader.setInput(imageInputStream, true, true);
                BufferedImage image = reader.read(0);
                if (image == null || image.getWidth() <= 0 || image.getHeight() <= 0) {
                    throw new BusinessException("invalid image content");
                }
                return new DecodedImage(format, image);
            } finally {
                reader.dispose();
            }
        }
    }

    private BufferedImage resize(BufferedImage source, String format) {
        int sourceWidth = source.getWidth();
        int sourceHeight = source.getHeight();
        int maxEdge = Math.max(1, storageProperties.getCarImageMaxEdge());
        double scale = Math.min(1.0D, (double) maxEdge / Math.max(sourceWidth, sourceHeight));
        int targetWidth = Math.max(1, (int) Math.round(sourceWidth * scale));
        int targetHeight = Math.max(1, (int) Math.round(sourceHeight * scale));
        int imageType = "jpeg".equals(format) ? BufferedImage.TYPE_INT_RGB : BufferedImage.TYPE_INT_ARGB;
        BufferedImage target = new BufferedImage(targetWidth, targetHeight, imageType);
        Graphics2D graphics = target.createGraphics();
        try {
            graphics.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BICUBIC);
            graphics.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
            graphics.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
            if ("jpeg".equals(format)) {
                graphics.setColor(Color.WHITE);
                graphics.fillRect(0, 0, targetWidth, targetHeight);
            }
            graphics.drawImage(source, 0, 0, targetWidth, targetHeight, null);
        } finally {
            graphics.dispose();
        }
        return target;
    }

    private byte[] writeImage(BufferedImage image, String format) throws IOException {
        if ("png".equals(format)) {
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            if (!ImageIO.write(image, "png", outputStream)) {
                throw new IOException("no png writer available");
            }
            return outputStream.toByteArray();
        }

        Iterator<ImageWriter> writers = ImageIO.getImageWritersByFormatName("jpeg");
        if (!writers.hasNext()) {
            throw new IOException("no jpeg writer available");
        }
        ImageWriter writer = writers.next();
        try (ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
             ImageOutputStream imageOutputStream = ImageIO.createImageOutputStream(outputStream)) {
            writer.setOutput(imageOutputStream);
            ImageWriteParam param = writer.getDefaultWriteParam();
            if (param.canWriteCompressed()) {
                param.setCompressionMode(ImageWriteParam.MODE_EXPLICIT);
                param.setCompressionQuality(clampedJpegQuality());
            }
            writer.write(null, new IIOImage(image, null, null), param);
            imageOutputStream.flush();
            return outputStream.toByteArray();
        } finally {
            writer.dispose();
        }
    }

    private float clampedJpegQuality() {
        return Math.max(0.1F, Math.min(1.0F, storageProperties.getCarImageJpegQuality()));
    }

    private String normalizeFormat(String formatName) {
        String format = formatName == null ? "" : formatName.toLowerCase(Locale.ROOT);
        if ("jpg".equals(format)) {
            return "jpeg";
        }
        return format;
    }

    private boolean isSupportedFormat(String format) {
        return "jpeg".equals(format) || "png".equals(format);
    }

    private String extension(String format) {
        return "jpeg".equals(format) ? "jpg" : "png";
    }

    private String contentType(String format) {
        return "jpeg".equals(format) ? JPEG_CONTENT_TYPE : PNG_CONTENT_TYPE;
    }

    private String checksum(byte[] bytes) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            return HexFormat.of().formatHex(digest.digest(bytes));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException("SHA-256 is not available", exception);
        }
    }

    private String sanitizeOriginalFilename(String originalFilename, String extension) {
        String filename = StringUtils.hasText(originalFilename) ? originalFilename.trim() : "";
        filename = filename.replace('\\', '/');
        int lastSlash = filename.lastIndexOf('/');
        if (lastSlash >= 0) {
            filename = filename.substring(lastSlash + 1);
        }
        filename = filename.replaceAll("[\\r\\n]", "").trim();
        if (!StringUtils.hasText(filename)) {
            filename = "upload." + extension;
        }
        if (filename.length() > 255) {
            filename = filename.substring(filename.length() - 255);
        }
        return filename;
    }

    private String trimToNull(String value) {
        if (!StringUtils.hasText(value)) {
            return null;
        }
        return value.trim();
    }

    private CarImageAssetVO toVO(CarImageAsset asset) {
        CarImageAssetVO vo = new CarImageAssetVO();
        vo.setId(asset.getId());
        vo.setCarId(asset.getCarId());
        vo.setOriginalFilename(asset.getOriginalFilename());
        vo.setStoredFilename(asset.getStoredFilename());
        vo.setContentType(asset.getContentType());
        vo.setSizeBytes(asset.getSizeBytes());
        vo.setWidth(asset.getWidth());
        vo.setHeight(asset.getHeight());
        vo.setPublicUrl(asset.getPublicUrl());
        vo.setStoragePath(asset.getStoragePath());
        vo.setChecksum(asset.getChecksum());
        vo.setAuditStatus(asset.getAuditStatus());
        vo.setRejectReason(asset.getRejectReason());
        vo.setCreatedByAdminId(asset.getCreatedByAdminId());
        vo.setReviewedByAdminId(asset.getReviewedByAdminId());
        vo.setCreateTime(asset.getCreateTime());
        vo.setUpdateTime(asset.getUpdateTime());
        vo.setReviewTime(asset.getReviewTime());
        return vo;
    }

    private record DecodedImage(String format, BufferedImage image) {
    }

    private record StoredImage(
            String storedFilename,
            String extension,
            String contentType,
            Long sizeBytes,
            Integer width,
            Integer height,
            String publicUrl,
            String storagePath,
            String checksum) {
    }
}
