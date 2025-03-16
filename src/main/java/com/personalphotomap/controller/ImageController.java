package com.personalphotomap.controller;

import com.personalphotomap.model.Album;
import com.personalphotomap.model.AppUser;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.AlbumRepository;
import com.personalphotomap.repository.ImageRepository;
import com.personalphotomap.repository.UserRepository;
import com.personalphotomap.security.JwtUtil;
import com.personalphotomap.service.S3Service;
import org.apache.tika.Tika;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.*;

/**
 * ImageController
 *
 * This controller manages all image-related operations, including:
 * - Uploading images to AWS S3
 * - Deleting images from AWS S3 and the database
 * - Managing images in albums
 * - Retrieving images by country, year, or user
 *
 * Each endpoint ensures that only authenticated users can access or modify
 * their own images.
 */
@RestController
@RequestMapping("/api/images")
public class ImageController {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private UserRepository userRepository;

    /**
     * S3Service handles all AWS S3 operations, including file upload and deletion.
     */
    @Autowired
    private S3Service s3Service;

    /**
     * uploadImages
     *
     * Uploads the provided list of JPEG images to AWS S3, saves metadata to the
     * database,
     * and returns the public S3 URLs for each uploaded file.
     *
     * @param files     List of images to be uploaded (MultipartFile).
     * @param countryId The country identifier for grouping images (e.g., "br" for
     *                  Brazil).
     * @param year      The year associated with the images (e.g., 2023).
     * @param token     JWT token for user authentication.
     * @return HTTP response containing a success message and the list of uploaded
     *         image URLs.
     */
    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImages(
            @RequestParam("images") List<MultipartFile> files,
            @RequestParam("countryId") String countryId,
            @RequestParam("year") int year,
            @RequestHeader(value = "Authorization") String token) {

        // 1. Validate JWT token and extract user's email
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing JWT token.");
        }

        // 2. Check if the request has any files
        if (files == null || files.isEmpty()) {
            return ResponseEntity.badRequest().body("No files were provided.");
        }

        // 🔥 Busca o usuário pelo email no banco de dados
        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new RuntimeException("Usuário não encontrado");
        }

        // 3. Prepare variables to store results
        List<String> imageUrls = new ArrayList<>();
        List<String> invalidFiles = new ArrayList<>();
        Tika tika = new Tika(); // Used to detect MIME type

        try {
            // 4. Iterate through each file and upload valid JPEG images to S3
            for (MultipartFile file : files) {
                if (file.isEmpty()) {
                    continue;
                }

                // Validate MIME type is JPEG
                String mimeType = tika.detect(file.getInputStream());
                if (!mimeType.equalsIgnoreCase("image/jpeg")) {
                    invalidFiles.add(file.getOriginalFilename());
                    continue;
                }

                // Create a unique filename for S3
                String fileName = UUID.randomUUID().toString() + "_"
                        + StringUtils.cleanPath(file.getOriginalFilename());

                // 5. Upload file to AWS S3 using S3Service
                String fileUrl;
                try {
                    fileUrl = s3Service.uploadFile(file, fileName); // You might pass countryId or other info if needed
                } catch (Exception e) {
                    return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body("Error uploading to S3: " + e.getMessage());
                }

                // 6. Persist image metadata to the database
                Image image = new Image();
                image.setCountryId(countryId);
                image.setFileName(fileName);
                image.setFilePath(fileUrl); // Store the full S3 URL in the database
                image.setYear(year);
                imageRepository.save(image);

                imageUrls.add(fileUrl);
            }

            // 7. Check if any valid images were uploaded
            if (imageUrls.isEmpty()) {
                return ResponseEntity.badRequest().body("No valid JPEG images were uploaded.");
            }

            // Optionally return info about invalid files
            Map<String, Object> response = new HashMap<>();
            response.put("message", "Images uploaded successfully.");
            response.put("imageUrls", imageUrls);
            if (!invalidFiles.isEmpty()) {
                response.put("invalidFiles", invalidFiles);
            }

            return ResponseEntity.ok(response);

        } catch (IOException e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error processing images: " + e.getMessage());
        }
    }

    /**
     * deleteAllImagesByCountry
     *
     * Deletes all images for a specific country from AWS S3 and the database,
     * after removing them from any albums they are associated with.
     *
     * @param countryId The country identifier (e.g., "br").
     * @param token     JWT token for user authentication.
     * @return A success or failure message.
     */
    @DeleteMapping("/delete-all-images/{countryId}")
    public ResponseEntity<?> deleteAllImagesByCountry(
            @PathVariable String countryId,
            @RequestHeader(value = "Authorization") String token) {

        // 1. Validate JWT token and extract user's email
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
        }

        // 2. Find all images that match the countryId and user
        List<Image> images = imageRepository.findByCountryIdAndUserId(countryId, user.getId());
        if (images.isEmpty()) {
            return ResponseEntity.ok("No images found for country " + countryId);
        }

        // 3. Remove the images from any albums before deletion
        List<Album> allAlbums = albumRepository.findAll();
        for (Image img : images) {
            for (Album album : allAlbums) {
                if (album.getImages().removeIf(image -> image.getId().equals(img.getId()))) {
                    albumRepository.save(album);
                }
            }
        }

        // 4. Delete each image from AWS S3
        for (Image image : images) {
            try {
                s3Service.deleteFile(image.getFilePath());
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting file from S3: " + e.getMessage());
            }
        }

        // 5. Remove image records from the database
        imageRepository.deleteAll(images);

        return ResponseEntity.ok("All images for country " + countryId + " have been successfully deleted.");
    }

    /**
     * deleteImagesByCountryAndYear
     *
     * Deletes all images from a specific country and year. Removes the images from
     * albums,
     * deletes them from AWS S3, and finally removes them from the database.
     *
     * @param countryId The country identifier (e.g., "br").
     * @param year      The year associated with the images (e.g., 2023).
     * @param token     JWT token for user authentication.
     * @return A success or failure message.
     */
    @DeleteMapping("/{countryId}/{year}")
    public ResponseEntity<?> deleteImagesByCountryAndYear(
            @PathVariable String countryId,
            @PathVariable int year,
            @RequestHeader(value = "Authorization") String token) {

        // 1. Validate JWT token and extract user's email
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                    .body("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
        }

        // 2. Retrieve images based on country, year, and user
        List<Image> images = imageRepository.findByCountryIdAndYearAndUserId(countryId, year, user.getId());
        if (images.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND)
                    .body("No images found for year " + year + ".");
        }

        // 3. Remove images from all albums before deleting
        List<Album> allAlbums = albumRepository.findAll();
        for (Image img : images) {
            for (Album album : allAlbums) {
                if (album.getImages().removeIf(i -> i.getId().equals(img.getId()))) {
                    albumRepository.save(album);
                }
            }
        }

        // 4. Delete images from AWS S3
        for (Image image : images) {
            try {
                s3Service.deleteFile(image.getFilePath());
            } catch (Exception e) {
                e.printStackTrace();
                return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                        .body("Error deleting file from S3: " + e.getMessage());
            }
        }

        // 5. Remove images from the database
        imageRepository.deleteAll(images);

        return ResponseEntity.ok("Images from " + countryId + " in year " + year + " were successfully deleted.");
    }

    /**
     * deleteImageById
     *
     * Deletes a single image by its ID from AWS S3 and the database,
     * ensuring the user has permission and removing the image from any albums
     * first.
     *
     * @param id    The ID of the image to delete.
     * @param token JWT token for user authentication.
     * @return A success or failure message.
     */
    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteImageById(
            @PathVariable Long id,
            @RequestHeader(value = "Authorization") String token) {

        // 1. Validate JWT token and extract email
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
        }

        // 2. Check if the image exists and if the user owns it
        Optional<Image> optionalImage = imageRepository.findById(id);
        if (optionalImage.isEmpty()) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Image not found.");
        }

        Image image = optionalImage.get();
        if (!image.getUser().getId().equals(user.getId())) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body("Você não tem permissão para deletar esta imagem.");
        }

        try {
            // 3. Remove the image from any album that contains it
            List<Album> albums = albumRepository.findByImageId(id);
            for (Album album : albums) {
                if (album.getImages().removeIf(img -> img.getId().equals(id))) {
                    // If the album becomes empty, you can optionally delete it
                    if (album.getImages().isEmpty()) {
                        albumRepository.delete(album);
                    } else {
                        albumRepository.save(album);
                    }
                }
            }

            // 4. Delete the image from S3
            s3Service.deleteFile(image.getFilePath());

            // 5. Remove the image record from the database
            imageRepository.delete(image);
            return ResponseEntity.ok("Image successfully deleted.");

        } catch (Exception e) {
            e.printStackTrace();
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body("Error deleting image: " + e.getMessage());
        }
    }

    /**
     * deleteMultipleImages
     *
     * Deletes multiple images identified by their IDs, removing them from AWS S3
     * and the database, as well as any albums that reference them.
     *
     * @param imageIds List of image IDs to delete.
     * @param token    JWT token for user authentication.
     * @return A success or failure message.
     */
    @DeleteMapping("/delete-multiple")
    public ResponseEntity<?> deleteMultipleImages(
            @RequestBody List<Long> imageIds,
            @RequestHeader(value = "Authorization") String token) {

        System.out.println("🔥 Batch delete request for images: " + imageIds);

        // 1. Validate JWT token and extract email
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Usuário não encontrado.");
        }

        // 2. Retrieve all images to delete
        List<Image> imagesToDelete = imageRepository.findAllById(imageIds);

        // 3. Check ownership and remove from albums
        for (Image image : imagesToDelete) {
            if (!image.getUser().getId().equals(user.getId())) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body("You do not have permission to delete some images.");
            }

            // Remove the image from any albums that contain it
            List<Album> albums = albumRepository.findAll();
            for (Album album : albums) {
                if (album.getImages().removeIf(img -> img.getId().equals(image.getId()))) {
                    albumRepository.save(album);
                }
            }

            // Delete from S3
            try {
                s3Service.deleteFile(image.getFilePath());
            } catch (Exception e) {
                System.out.println("❌ Error deleting file from S3: " + e.getMessage());
            }
        }

        // 4. Delete image records from the database
        imageRepository.deleteAll(imagesToDelete);
        System.out.println("✅ All images were successfully deleted!");

        return ResponseEntity.ok("Images deleted successfully.");
    }

    /**
     * getImagesByCountry
     *
     * Retrieves all images for a given country that belong to the authenticated
     * user.
     * The returned file paths are assumed to be full S3 URLs.
     *
     * @param countryId Country identifier.
     * @param token     JWT token for user authentication.
     * @return A list of Image entities belonging to the user for that country.
     */
    @GetMapping("/{countryId}")
    public ResponseEntity<List<Image>> getImagesByCountry(
            @PathVariable String countryId,
            @RequestHeader(value = "Authorization") String token) {

        // 1. Validate token and extract username
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        // 2. Fetch images from the database
        List<Image> images = imageRepository.findByCountryIdAndUserId(countryId, user.getId());
        if (images.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        // Return the images as-is. The filePath should already be an S3 URL.
        return ResponseEntity.ok(images);
    }

    /**
     * getCountriesWithPhotos
     *
     * Retrieves a list of distinct country IDs where the authenticated user has
     * images.
     *
     * @param token JWT token for user authentication.
     * @return A list of country IDs.
     */
    @GetMapping("/countries-with-photos")
    public ResponseEntity<List<String>> getCountriesWithPhotos(@RequestHeader(value = "Authorization") String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<String> countries = imageRepository.findDistinctCountryIdsByUserId(user.getId());
        if (countries.isEmpty()) {
            return ResponseEntity.ok(Collections.emptyList());
        }

        return ResponseEntity.ok(countries);
    }

    /**
     * getAvailableYears
     *
     * Returns a list of distinct years for which the user has uploaded images.
     *
     * @param token JWT token for user authentication.
     * @return A list of years.
     */
    @GetMapping("/available-years")
    public ResponseEntity<List<Integer>> getAvailableYears(@RequestHeader(value = "Authorization") String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<Integer> years = imageRepository.findDistinctYearsByUserId(user.getId());
        return ResponseEntity.ok(years);
    }

    /**
     * getAllImages
     *
     * Retrieves all images belonging to the authenticated user, optionally filtered
     * by year.
     *
     * @param token JWT token for user authentication.
     * @param year  (Optional) year filter for images.
     * @return A list of Image entities.
     */
    @GetMapping("/allPictures")
    public ResponseEntity<List<Image>> getAllImages(
            @RequestHeader(value = "Authorization") String token,
            @RequestParam(required = false) Integer year) {

        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<Image> images;
        if (year != null) {
            // 🔥 Busca as imagens do usuário por ano
            images = imageRepository.findByUserIdAndYear(user.getId(), year);
        } else {
            // 🔥 Busca todas as imagens do usuário ordenadas pela data de upload
            images = imageRepository.findByUserIdOrderByUploadDateDesc(user.getId());
        }

        return ResponseEntity.ok(images);
    }

    /**
     * getYearsByCountry
     *
     * Retrieves all distinct years for a specific country that belong to the
     * authenticated user.
     *
     * @param countryId Country identifier.
     * @param token     JWT token for user authentication.
     * @return A list of years in which the user has images for the specified
     *         country.
     */
    @GetMapping("/{countryId}/available-years")
    public ResponseEntity<List<Integer>> getYearsByCountry(
            @PathVariable String countryId,
            @RequestHeader(value = "Authorization") String token) {

        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<Integer> years = imageRepository.findDistinctYearsByCountryIdAndUserId(countryId, user.getId());
        return ResponseEntity.ok(years);
    }

    /**
     * getImagesByCountryAndYear
     *
     * Retrieves all images for a given country and year belonging to the
     * authenticated user.
     *
     * @param countryId Country identifier.
     * @param year      Year of the images.
     * @param token     JWT token for user authentication.
     * @return A list of Image entities.
     */
    @GetMapping("/{countryId}/{year}")
    public ResponseEntity<List<Image>> getImagesByCountryAndYear(
            @PathVariable String countryId,
            @PathVariable int year,
            @RequestHeader(value = "Authorization") String token) {

        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        List<Image> images = imageRepository.findByCountryIdAndYearAndUserId(countryId, year, user.getId());
        return ResponseEntity.ok(images);
    }

    /**
     * countUserPhotosAndCountries
     *
     * Returns the total count of photos and distinct countries for the
     * authenticated user.
     *
     * @param token JWT token for user authentication.
     * @return A JSON response with "photoCount" and "countryCount" fields.
     */
    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> countUserPhotosAndCountries(
            @RequestHeader(value = "Authorization") String token) {

        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(null);
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(null);
        }

        long photoCount = imageRepository.countByUserId(user.getId());
        long countryCount = imageRepository.countDistinctCountryByUserId(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("photoCount", photoCount);
        response.put("countryCount", countryCount);

        return ResponseEntity.ok(response);
    }

}
