package com.personalphotomap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;

/**
 * S3Config
 * 
 * This class configures and provides an AWS S3 client for interacting with Amazon S3 storage.
 * It retrieves the AWS credentials and region from environment variables to ensure security
 * and flexibility when deploying the application in different environments.
 */
@Configuration
public class S3Config {

    /**
     * Creates and configures an Amazon S3 client bean.
     * 
     * - Uses the AWS SDK for Java (v2).
     * - Retrieves AWS region, access key, and secret key from environment variables.
     * - Uses `StaticCredentialsProvider` to provide credentials for authentication.
     * - Returns an instance of `S3Client` that can be used for file operations (upload, delete, fetch).
     * 
     * @return A configured S3Client instance.
     */
    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(System.getenv("AWS_REGION"))) // Retrieves the AWS region
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                System.getenv("AWS_ACCESS_KEY_ID"), // Retrieves AWS access key
                                System.getenv("AWS_SECRET_ACCESS_KEY") // Retrieves AWS secret key
                        )
                ))
                .build();
    }
}
