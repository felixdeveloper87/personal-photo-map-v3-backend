package com.personalphotomap.dto;

/**
 * DTO for sending album data to the client in a secure and structured way.
 */
public class AlbumResponseDTO {

    private Long id;
    private String albumName;
    private String countryId;
    private Long userId;
    private int numberOfImages;

    public AlbumResponseDTO() {
    }

    public AlbumResponseDTO(Long id, String albumName, String countryId, Long userId, int numberOfImages) {
        this.id = id;
        this.albumName = albumName;
        this.countryId = countryId;
        this.userId = userId;
        this.numberOfImages = numberOfImages;
    }

    public Long getId() {
        return id;
    }

    public String getAlbumName() {
        return albumName;
    }

    public String getCountryId() {
        return countryId;
    }

    public Long getUserId() {
        return userId;
    }

    public int getNumberOfImages() {
        return numberOfImages;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public void setAlbumName(String albumName) {
        this.albumName = albumName;
    }

    public void setCountryId(String countryId) {
        this.countryId = countryId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public void setNumberOfImages(int numberOfImages) {
        this.numberOfImages = numberOfImages;
    }
}
