package com.personalphotomap.dto;

import java.time.LocalDateTime;

/**
 * DTO used to expose public information about images
 * without exposing internal entity references or sensitive data.
 *
 * This class is essential for separating the persistence layer (Image entity)
 * from the data sent over the network, providing a clean API contract.
 */
public class ImageDTO {

    // Unique identifier of the image
    private Long id;

    // ISO code of the country where the image is associated
    private String countryId;

    // Original name of the uploaded image file
    private String fileName;

    // Relative or public file path to access the image
    private String filePath;

    // Year the photo is associated with (e.g., year of the trip)
    private int year;

    // Date and time when the image was uploaded
    private LocalDateTime uploadDate;

    // Default constructor required by frameworks
    public ImageDTO() {
    }

    // Constructor for quick initialization of all fields
    public ImageDTO(Long id, String countryId, String fileName, String filePath, int year, LocalDateTime uploadDate) {
        this.id = id;
        this.countryId = countryId;
        this.fileName = fileName;
        this.filePath = filePath;
        this.year = year;
        this.uploadDate = uploadDate;
    }

    // Getters — used to access fields when serializing the object to JSON
    public Long getId() {
        return id;
    }

    public String getCountryId() {
        return countryId;
    }

    public String getFileName() {
        return fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public int getYear() {
        return year;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    // Setters — used when populating DTOs from the backend
    public void setId(Long id) {
        this.id = id;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }
}
