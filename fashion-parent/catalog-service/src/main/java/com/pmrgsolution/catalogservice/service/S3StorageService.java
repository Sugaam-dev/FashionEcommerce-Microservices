package com.pmrgsolution.catalogservice.service;

import com.pmrgsolution.catalogservice.config.S3Properties;
import com.pmrgsolution.catalogservice.dto.response.ImageUploadResponse;
import com.pmrgsolution.catalogservice.exception.ImageProcessingException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.model.DeleteObjectRequest;
import software.amazon.awssdk.services.s3.model.PutObjectRequest;

import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class S3StorageService {

    private final S3Client s3Client;
    private final S3Properties s3Properties;
    private final ImageOptimizerService imageOptimizerService;

    public ImageUploadResponse uploadImage(MultipartFile file, String folder) {
        ImageOptimizerService.OptimizedImageResult optimized = imageOptimizerService.optimize(file);

        String sanitizedFolder = (folder == null || folder.isBlank()) ? "products" : folder.replaceAll("[^a-zA-Z0-9_-]", "");
        String uniqueId = UUID.randomUUID().toString();
        String fullKey = String.format("%s/%s.jpg", sanitizedFolder, uniqueId);
        String thumbKey = String.format("%s/thumbnails/%s_thumb.jpg", sanitizedFolder, uniqueId);

        try {
            // Upload Full Size Image
            PutObjectRequest fullPutRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(fullKey)
                    .contentType("image/jpeg")
                    .contentLength((long) optimized.fullBytes().length)
                    .build();

            s3Client.putObject(fullPutRequest, RequestBody.fromBytes(optimized.fullBytes()));

            // Upload Thumbnail
            PutObjectRequest thumbPutRequest = PutObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(thumbKey)
                    .contentType("image/jpeg")
                    .contentLength((long) optimized.thumbnailBytes().length)
                    .build();

            s3Client.putObject(thumbPutRequest, RequestBody.fromBytes(optimized.thumbnailBytes()));

            String fullUrl = buildPublicUrl(fullKey);
            String thumbUrl = buildPublicUrl(thumbKey);

            log.info("Successfully uploaded image key={}, thumbKey={}, size={}", fullKey, thumbKey, optimized.fullBytes().length);

            return new ImageUploadResponse(
                    fullUrl,
                    thumbUrl,
                    "image/jpeg",
                    optimized.fullBytes().length
            );

        } catch (Exception e) {
            log.error("Failed to upload image to S3 bucket {}: {}", s3Properties.getBucketName(), e.getMessage(), e);
            throw new ImageProcessingException("Failed to upload image: " + e.getMessage());
        }
    }

    public void deleteImage(String key) {
        if (key == null || key.isBlank()) {
            return;
        }
        try {
            String sanitizedKey = extractKeyFromUrl(key);
            DeleteObjectRequest deleteRequest = DeleteObjectRequest.builder()
                    .bucket(s3Properties.getBucketName())
                    .key(sanitizedKey)
                    .build();
            s3Client.deleteObject(deleteRequest);
            log.info("Deleted S3 object key={}", sanitizedKey);
        } catch (Exception e) {
            log.warn("Failed to delete S3 object key {}: {}", key, e.getMessage());
        }
    }

    private String buildPublicUrl(String key) {
        if (s3Properties.getPublicBaseUrl() != null && !s3Properties.getPublicBaseUrl().isBlank()) {
            String base = s3Properties.getPublicBaseUrl().replaceAll("/+$", "");
            return String.format("%s/%s", base, key);
        }
        String endpoint = s3Properties.getEndpoint().replaceAll("/+$", "");
        return String.format("%s/%s/%s", endpoint, s3Properties.getBucketName(), key);
    }

    private String extractKeyFromUrl(String urlOrKey) {
        if (!urlOrKey.startsWith("http://") && !urlOrKey.startsWith("https://")) {
            return urlOrKey;
        }
        String bucketMarker = "/" + s3Properties.getBucketName() + "/";
        int idx = urlOrKey.indexOf(bucketMarker);
        if (idx != -1) {
            return urlOrKey.substring(idx + bucketMarker.length());
        }
        int lastSlash = urlOrKey.lastIndexOf('/');
        return lastSlash != -1 ? urlOrKey.substring(lastSlash + 1) : urlOrKey;
    }
}
