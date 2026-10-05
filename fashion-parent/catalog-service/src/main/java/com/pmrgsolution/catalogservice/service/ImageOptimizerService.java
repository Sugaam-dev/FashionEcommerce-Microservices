package com.pmrgsolution.catalogservice.service;

import com.pmrgsolution.catalogservice.exception.ImageProcessingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import javax.imageio.ImageIO;
import java.awt.Graphics2D;
import java.awt.Image;
import java.awt.RenderingHints;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Set;

@Slf4j
@Service
public class ImageOptimizerService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg",
            "image/jpg",
            "image/png",
            "image/webp"
    );

    public record OptimizedImageResult(
            byte[] fullBytes,
            byte[] thumbnailBytes,
            String contentType,
            int fullWidth,
            int fullHeight
    ) {}

    public OptimizedImageResult optimize(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new ImageProcessingException("Uploaded file cannot be empty");
        }

        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ImageProcessingException("Unsupported image format. Allowed formats: JPEG, PNG, WEBP");
        }

        try {
            byte[] inputBytes = file.getBytes();
            BufferedImage originalImage = ImageIO.read(new ByteArrayInputStream(inputBytes));
            if (originalImage == null) {
                throw new ImageProcessingException("Could not decode image content. File may be corrupted or invalid.");
            }

            // Generate full-size optimized image (max 1000x1000)
            BufferedImage fullImage = resizeImage(originalImage, 1000, 1000);
            byte[] fullBytes = toJpegByteArray(fullImage, 0.85f);

            // Generate thumbnail (max 300x300)
            BufferedImage thumbImage = resizeImage(originalImage, 300, 300);
            byte[] thumbBytes = toJpegByteArray(thumbImage, 0.80f);

            return new OptimizedImageResult(
                    fullBytes,
                    thumbBytes,
                    "image/jpeg",
                    fullImage.getWidth(),
                    fullImage.getHeight()
            );

        } catch (IOException e) {
            log.error("Failed to process and optimize image: {}", e.getMessage(), e);
            throw new ImageProcessingException("Image processing failed: " + e.getMessage());
        }
    }

    private BufferedImage resizeImage(BufferedImage original, int maxWidth, int maxHeight) {
        int origWidth = original.getWidth();
        int origHeight = original.getHeight();

        if (origWidth <= maxWidth && origHeight <= maxHeight) {
            // Convert to RGB without resizing (strips EXIF / color profile metadata)
            BufferedImage rgbImage = new BufferedImage(origWidth, origHeight, BufferedImage.TYPE_INT_RGB);
            Graphics2D g2d = rgbImage.createGraphics();
            g2d.drawImage(original, 0, 0, java.awt.Color.WHITE, null);
            g2d.dispose();
            return rgbImage;
        }

        double ratio = Math.min((double) maxWidth / origWidth, (double) maxHeight / origHeight);
        int targetWidth = (int) Math.round(origWidth * ratio);
        int targetHeight = (int) Math.round(origHeight * ratio);

        BufferedImage resized = new BufferedImage(targetWidth, targetHeight, BufferedImage.TYPE_INT_RGB);
        Graphics2D g2d = resized.createGraphics();
        g2d.setRenderingHint(RenderingHints.KEY_INTERPOLATION, RenderingHints.VALUE_INTERPOLATION_BILINEAR);
        g2d.setRenderingHint(RenderingHints.KEY_RENDERING, RenderingHints.VALUE_RENDER_QUALITY);
        g2d.setRenderingHint(RenderingHints.KEY_ANTIALIASING, RenderingHints.VALUE_ANTIALIAS_ON);
        g2d.drawImage(original, 0, 0, targetWidth, targetHeight, java.awt.Color.WHITE, null);
        g2d.dispose();

        return resized;
    }

    private byte[] toJpegByteArray(BufferedImage image, float quality) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, "jpg", baos);
        return baos.toByteArray();
    }
}
