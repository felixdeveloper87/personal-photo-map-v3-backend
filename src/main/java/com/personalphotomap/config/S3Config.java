/**
 * S3Config
 * 
 * This class configures and provides an AWS S3 client for interacting with Amazon S3 storage.
 * It retrieves the AWS credentials and region from environment variables to ensure security
 * and flexibility when deploying the application in different environments.
 */

package com.personalphotomap.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.auth.credentials.AwsBasicCredentials;
import software.amazon.awssdk.auth.credentials.StaticCredentialsProvider;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;


@Configuration
public class S3Config {

    /**
     * Creates and configures an Amazon S3 client bean.
     * 
     * - Uses the AWS SDK for Java (v2).
     * - Retrieves AWS region, access key, and secret key from environment variables.
     * - Uses `StaticCredentialsProvider` to provide credentials for authentication.
     * - Registers the configured `S3Client` as a Spring Bean using `@Bean`,
     * - allowing it to be injected and reused em any part of the application.
     * - Ensures that a single, reusable instance is managed by the Spring container.
     * 
     * @return A configured S3Client instance.
     */
    @Bean 
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(System.getenv("AWS_REGION"))) 
                .credentialsProvider(StaticCredentialsProvider.create(
                        AwsBasicCredentials.create(
                                System.getenv("AWS_ACCESS_KEY_ID"), 
                                System.getenv("AWS_SECRET_ACCESS_KEY") 
                        )
                ))
                .build();
    }
}
