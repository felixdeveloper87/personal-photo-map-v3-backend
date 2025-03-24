package com.personalphotomap.service;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileStorageService {
    String saveImageFile(MultipartFile file, String countryId) throws IOException;
    void deleteFile(String pathOrUrl) throws IOException;
}
