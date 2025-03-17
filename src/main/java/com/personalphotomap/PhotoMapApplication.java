package com.personalphotomap;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.scheduling.annotation.EnableAsync;

@SpringBootApplication
@EnableAsync
public class PhotoMapApplication {
    public static void main(String[] args) {
        SpringApplication.run(PhotoMapApplication.class, args);
    }
}
