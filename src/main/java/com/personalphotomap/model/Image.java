package com.personalphotomap.model;

import jakarta.persistence.*;
import java.time.LocalDateTime;

import com.fasterxml.jackson.annotation.JsonBackReference;

@Entity
@Table(name = "images", indexes = {
        @Index(name = "idx_images_user", columnList = "user_id"),
        @Index(name = "idx_images_country", columnList = "countryId"),
        @Index(name = "idx_images_year", columnList = "year")
})
public class Image {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String countryId;
    private String fileName;
    private String filePath;
    private int year;

    @Column(name = "upload_date", updatable = false)
    private LocalDateTime uploadDate;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false) // Define a chave estrangeira
    @JsonBackReference
    private AppUser user; // Associação com AppUser

    public Image() {
    }

    public Image(Long id, String countryId, String fileName, AppUser user, String filePath, int year) {
        this.id = id;
        this.countryId = countryId;
        this.fileName = fileName;
        this.user = user;
        this.filePath = filePath;
        this.year = year;
    }

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getCountryId() {
        return countryId;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public String getFilePath() {
        return filePath;
    }

    public void setFilePath(String filePath) {
        this.filePath = filePath;
    }

    public int getYear() {
        return year;
    }

    public void setYear(int year) {
        this.year = year;
    }

    public LocalDateTime getUploadDate() {
        return uploadDate;
    }

    public void setUploadDate(LocalDateTime uploadDate) {
        this.uploadDate = uploadDate;
    }

    public AppUser getUser() {
        return user;
    }

    public void setUser(AppUser user) {
        this.user = user;
    }

    @PrePersist
    protected void onCreate() {
        this.uploadDate = LocalDateTime.now();
    }
}
