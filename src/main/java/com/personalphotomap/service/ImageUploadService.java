package com.personalphotomap.service;

import org.apache.tika.Tika;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.util.StringUtils;

import com.personalphotomap.model.AppUser;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.ImageRepository;

import java.io.IOException;
import java.util.UUID;
import java.util.concurrent.CompletableFuture;

/**
 * ImageUploadService
 *
 * Service responsible for handling asynchronous image uploads to Amazon S3.
 *
 * Responsibilities:
 * - Validates uploaded files using Apache Tika (accepts only JPEG).
 * - Renames files using UUID to ensure uniqueness.
 * - Uploads images to S3 storage.
 * - Persists image metadata (e.g. country, user, year, path) in the database.
 * - Uses @Async and CompletableFuture to support parallel uploads.
 *
 * Designed to be called from ImageService, separating file handling from core logic.
 */

@Service
public class ImageUploadService {

    private static final Logger logger = LoggerFactory.getLogger(ImageUploadService.class);
    private static final Tika tika = new Tika(); 
    @Autowired
    private S3Service s3Service;

    @Autowired
    private ImageRepository imageRepository;

    @Async
    public CompletableFuture<String> uploadAndSaveImage(MultipartFile file, String countryId, int year, AppUser user) {
        String threadName = Thread.currentThread().getName();

        try {
            logger.info("Starting image upload: {} on thread: {}", file.getOriginalFilename(), threadName);

            // File type validation
            String mimeType = tika.detect(file.getInputStream());
            if (!mimeType.equalsIgnoreCase("image/jpeg")) {
                logger.warn("Invalid file detected: {} | MIME Type: {}", file.getOriginalFilename(), mimeType);
                return CompletableFuture.completedFuture(null);
            }

            // Generate unique file name
            String fileName = UUID.randomUUID().toString() + "_" + StringUtils.cleanPath(file.getOriginalFilename());

            // Upload to S3
            String fileUrl = s3Service.uploadFile(file, fileName);

            logger.info("✅ Upload complete: {} | URL: {} | Thread: {}", file.getOriginalFilename(), fileUrl,
                    threadName);

            // Save image metadata to the database
            Image image = new Image();
            image.setUser(user);
            image.setCountryId(countryId);
            image.setFileName(fileName);
            image.setFilePath(fileUrl);
            image.setYear(year);
            imageRepository.save(image);

            return CompletableFuture.completedFuture(fileUrl);
        } catch (IOException e) {
            logger.error("Image upload error: {}", file.getOriginalFilename(), e);
            return CompletableFuture.failedFuture(e);
        }
    }

}
