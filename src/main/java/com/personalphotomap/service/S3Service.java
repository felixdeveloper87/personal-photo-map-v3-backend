package com.personalphotomap.service;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import software.amazon.awssdk.core.sync.RequestBody;
import software.amazon.awssdk.services.s3.S3Client;

import java.net.URL;
import java.util.UUID;

/**
 * S3Service
 * 
 * Manages file uploads to and deletions from AWS S3.
 * Provides two overloads for uploadFile(), allowing either:
 * - auto-generated file names, or
 * - custom-specified file names.
 */

@Service
public class S3Service {

    private final S3Client s3Client;

    /**
     * Constructor for dependency injection.
     *
     * @param s3Client The AWS S3 client.
     */
    public S3Service(S3Client s3Client) {
        this.s3Client = s3Client;
    }

    /**
     * Uploads a file to S3 with an auto-generated key (UUID + original filename).
     *
     * @param file The MultipartFile to upload.
     * @return The public URL of the uploaded file.
     */
    public String uploadFile(MultipartFile file) {
        // Generate a unique file name
        String fileName = UUID.randomUUID().toString() + "-" + file.getOriginalFilename();
        return uploadFile(file, fileName);
    }

    /**
     * Uploads a file to S3 using a custom file name (key).
     *
     * @param file The MultipartFile to upload.
     * @param customFileName The exact file name (key) to store in S3.
     * @return The public URL of the uploaded file.
     */
    public String uploadFile(MultipartFile file, String customFileName) {
        try {
            // Perform the S3 upload with the specified key
            s3Client.putObject(
                b -> b.bucket(System.getenv("S3_BUCKET_NAME")).key(customFileName),
                RequestBody.fromBytes(file.getBytes())
            );

            // Retrieve the public URL of the uploaded file
            URL fileUrl = s3Client.utilities().getUrl(
                b -> b.bucket(System.getenv("S3_BUCKET_NAME")).key(customFileName)
            );

            return fileUrl.toString();
        } catch (Exception e) {
            throw new RuntimeException("Failed to upload file to S3", e);
        }
    }

    /**
     * Deletes a file from S3 based on its full public URL.
     * <p>
     * Note: This assumes that the S3 object's key is everything after the last '/' in the URL.
     * In production, you might store the key separately to avoid guesswork.
     *
     * @param fileUrl The full URL of the S3 object to delete.
     */
    public void deleteFile(String fileUrl) {
        try {
            String bucketName = System.getenv("S3_BUCKET_NAME");
            // Extract the key from the URL by taking the substring after the last '/'
            String key = fileUrl.substring(fileUrl.lastIndexOf('/') + 1);

            s3Client.deleteObject(b -> b.bucket(bucketName).key(key));
        } catch (Exception e) {
            throw new RuntimeException("Error deleting file on S3", e);
        }
    }
}
