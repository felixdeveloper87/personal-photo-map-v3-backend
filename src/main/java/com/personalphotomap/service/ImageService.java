package com.personalphotomap.service;

import com.personalphotomap.dto.ImageDTO;
import com.personalphotomap.model.AppUser;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.ImageRepository;
import com.personalphotomap.repository.UserRepository;
import com.personalphotomap.security.JwtUtil;

import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;

@Service
public class ImageService {

    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ImageUploadService imageUploadService;
    private final ImageDeleteService imageDeleteService;

    public ImageService(ImageRepository imageRepository,
            UserRepository userRepository,
            JwtUtil jwtUtil,
            ImageUploadService imageUploadService,
            ImageDeleteService imageDeleteService) {
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.imageUploadService = imageUploadService;
        this.imageDeleteService = imageDeleteService;
    }

    /**
     * Extracts the AppUser associated with the provided JWT token.
     * Centralized method for token validation and user retrieval.
     */

    private AppUser getUserFromToken(String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("User Not Found.");
        }

        return user;
    }

    // ===============================
    // UPLOAD METHOD
    // ===============================

    /**
     * Handles asynchronous upload of multiple images.
     * Images are validated, uploaded to S3, and saved to the database.
     *
     * @return list of uploaded image URLs
     */

    public List<String> handleUpload(List<MultipartFile> files, String countryId, int year, String token) {
        AppUser user = getUserFromToken(token);

        if (files == null || files.isEmpty()) {
            throw new IllegalArgumentException("No files were provided.");
        }

        List<CompletableFuture<String>> futures = new ArrayList<>();
        for (MultipartFile file : files) {
            futures.add(imageUploadService.uploadAndSaveImage(file, countryId, year, user));
        }

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();

        return futures.stream()
                .map(CompletableFuture::join)
                .filter(Objects::nonNull)
                .collect(Collectors.toList());
    }

    // ===============================
    // DELETE METHODS
    // ===============================

    /**
     * Deletes all images from a given country for the authenticated user.
     * Uses async deletion for better performance.
     */

    public void deleteAllImagesByCountry(String countryId, String token) { // ✅
        AppUser user = getUserFromToken(token);

        List<Image> images = imageRepository.findByCountryIdAndUserId(countryId, user.getId());
        if (images.isEmpty()) {
            return;
        }

        imageDeleteService.deleteImagesInParallel(images);
    }

    /**
     * Deletes all images from a specific country and year.
     * Currently not used on frontend but kept for potential future use.
     */

    public void deleteImagesByCountryAndYear(String countryId, int year, String token) { // ✅ ANALIZAR PQ NAO TENHO
                                                                                         // BOTAO PARA DELETAR POR
                                                                                         // IMAGEM E ANO NO FRONT
        AppUser user = getUserFromToken(token);

        List<Image> images = imageRepository.findByCountryIdAndYearAndUserId(countryId, year, user.getId());
        if (images.isEmpty()) {
            return;
        }

        imageDeleteService.deleteImagesInParallel(images);
    }

    /**
     * Deletes a single image by ID if it belongs to the authenticated user.
     */

    public void deleteImageById(Long imageId, String token) { // ✅
        AppUser user = getUserFromToken(token);
        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found."));

        if (!image.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You do not have permission to delete this image.");
        }

        imageDeleteService.deleteImage(image).join(); // Single async delete
    }

    /**
     * Deletes multiple images by their IDs, if all belong to the authenticated
     * user.
     */
    public void deleteMultipleImages(List<Long> imageIds, String token) { // ✅
        AppUser user = getUserFromToken(token);
        List<Image> imagesToDelete = imageRepository.findAllById(imageIds);

        if (imagesToDelete.isEmpty())
            return;

        boolean hasUnauthorized = imagesToDelete.stream()
                .anyMatch(img -> !img.getUser().getId().equals(user.getId()));

        if (hasUnauthorized) {
            throw new SecurityException("You do not have permission to delete one or more images.");
        }

        imageDeleteService.deleteImagesInParallel(imagesToDelete);
    }

    // ===============================
    // GET METHODS
    // ===============================

    /**
     * Returns all images associated with a country for the authenticated user.
     */
    public List<ImageDTO> getImagesByCountry(String countryId, String token) { // ✅
        AppUser user = getUserFromToken(token);
        List<Image> images = imageRepository.findByCountryIdAndUserId(countryId, user.getId());
        return convertToDTOList(images);
    }

    /**
     * Returns the list of distinct countries where the user has uploaded photos.
     */

    public List<String> getCountriesWithPhotos(String token) { // ✅
        AppUser user = getUserFromToken(token);
        return imageRepository.findDistinctCountryIdsByUserId(user.getId());
    }

    /**
     * Returns a list of distinct years the user has uploaded photos for.
     */
    public List<Integer> getAvailableYears(String token) { // ✅
        AppUser user = getUserFromToken(token);
        return imageRepository.findDistinctYearsByUserId(user.getId());
    }

    /**
     * Returns all images from the authenticated user, optionally filtered by year.
     */
    public List<ImageDTO> getAllImages(String token, Integer year) { // ✅
        AppUser user = getUserFromToken(token);

        List<Image> images;
        if (year != null) {
            images = imageRepository.findByUserIdAndYear(user.getId(), year);
        } else {
            images = imageRepository.findByUserIdOrderByUploadDateDesc(user.getId());
        }

        return convertToDTOList(images);
    }

    /**
     * Returns the list of available years for a specific country.
     */
    public List<Integer> getYearsByCountry(String countryId, String token) { // ✅
        AppUser user = getUserFromToken(token);
        return imageRepository.findDistinctYearsByCountryIdAndUserId(countryId, user.getId());
    }

    /**
     * Returns all images from a specific country and year for the authenticated
     * user.
     */
    public List<ImageDTO> getImagesByCountryAndYear(String countryId, int year, String token) { // ✅
        AppUser user = getUserFromToken(token);
        List<Image> images = imageRepository.findByCountryIdAndYearAndUserId(countryId, year, user.getId());
        return convertToDTOList(images);
    }

    /**
     * Returns a summary containing the total number of photos and distinct
     * countries.
     */
    public Map<String, Object> countUserPhotosAndCountries(String token) { // ✅
        AppUser user = getUserFromToken(token);

        long photoCount = imageRepository.countByUserId(user.getId());
        long countryCount = imageRepository.countDistinctCountryByUserId(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("photoCount", photoCount);
        response.put("countryCount", countryCount);
        return response;
    }

    // ===============================
    // CONVERSION HELPERS
    // ===============================

    /**
     * Converts an Image entity to a DTO.
     */
    public ImageDTO convertToDTO(Image image) { // ✅
        return new ImageDTO(
                image.getId(),
                image.getCountryId(),
                image.getFileName(),
                image.getFilePath(),
                image.getYear(),
                image.getUploadDate());
    }

    /**
     * Converts a list of Image entities to DTOs.
     */
    public List<ImageDTO> convertToDTOList(List<Image> images) { // ✅
        return images.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

}