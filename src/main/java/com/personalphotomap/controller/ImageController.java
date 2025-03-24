package com.personalphotomap.controller;

import com.personalphotomap.model.Image;
import com.personalphotomap.service.ImageService;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;

@RestController
@RequestMapping("/api/images")
public class ImageController {

    private final ImageService imageService;

    public ImageController(ImageService imageService) {
        this.imageService = imageService;
    }

    @PostMapping(value = "/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<?> uploadImages(
            @RequestParam("images") List<MultipartFile> files,
            @RequestParam("countryId") String countryId,
            @RequestParam("year") int year,
            @RequestHeader("Authorization") String token) {
        try {
            List<String> urls = imageService.handleUpload(files, countryId, year, token);
            return ResponseEntity.ok(Map.of("message", "Images uploaded successfully.", "imageUrls", urls));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Upload failed.");
        }
    }

    @DeleteMapping("/delete-all-images/{countryId}")
    public ResponseEntity<?> deleteAllImagesByCountry(
            @PathVariable String countryId,
            @RequestHeader("Authorization") String token) {
        try {
            imageService.deleteAllImagesByCountry(countryId, token);
            return ResponseEntity.ok("All images for country " + countryId + " have been successfully deleted.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete images.");
        }
    }

    @DeleteMapping("/{countryId}/{year}")
    public ResponseEntity<?> deleteImagesByCountryAndYear(
            @PathVariable String countryId,
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            imageService.deleteImagesByCountryAndYear(countryId, year, token);
            return ResponseEntity.ok("Images from " + countryId + " in year " + year + " were successfully deleted.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete images.");
        }
    }

    @DeleteMapping("/delete/{id}")
    public ResponseEntity<?> deleteImageById(
            @PathVariable Long id,
            @RequestHeader("Authorization") String token) {
        try {
            imageService.deleteImageById(id, token);
            return ResponseEntity.ok("Image successfully deleted.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete image.");
        }
    }

    @DeleteMapping("/delete-multiple")
    public ResponseEntity<?> deleteMultipleImages(
            @RequestBody List<Long> imageIds,
            @RequestHeader("Authorization") String token) {
        try {
            imageService.deleteMultipleImages(imageIds, token);
            return ResponseEntity.ok("Images deleted successfully.");
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(e.getMessage());
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed to delete images.");
        }
    }

    @GetMapping("/{countryId}")
    public ResponseEntity<List<Image>> getImagesByCountry(
            @PathVariable String countryId,
            @RequestHeader("Authorization") String token) {
        try {
            List<Image> images = imageService.getImagesByCountry(countryId, token);
            return ResponseEntity.ok(images);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/countries-with-photos")
    public ResponseEntity<List<String>> getCountriesWithPhotos(@RequestHeader("Authorization") String token) {
        try {
            List<String> countries = imageService.getCountriesWithPhotos(token);
            return ResponseEntity.ok(countries);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/available-years")
    public ResponseEntity<List<Integer>> getAvailableYears(@RequestHeader("Authorization") String token) {
        try {
            List<Integer> years = imageService.getAvailableYears(token);
            return ResponseEntity.ok(years);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/allPictures")
    public ResponseEntity<List<Image>> getAllImages(
            @RequestHeader("Authorization") String token,
            @RequestParam(required = false) Integer year) {
        try {
            List<Image> images = imageService.getAllImages(token, year);
            return ResponseEntity.ok(images);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{countryId}/available-years")
    public ResponseEntity<List<Integer>> getYearsByCountry(
            @PathVariable String countryId,
            @RequestHeader("Authorization") String token) {
        try {
            List<Integer> years = imageService.getYearsByCountry(countryId, token);
            return ResponseEntity.ok(years);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/{countryId}/{year}")
    public ResponseEntity<List<Image>> getImagesByCountryAndYear(
            @PathVariable String countryId,
            @PathVariable int year,
            @RequestHeader("Authorization") String token) {
        try {
            List<Image> images = imageService.getImagesByCountryAndYear(countryId, year, token);
            return ResponseEntity.ok(images);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

    @GetMapping("/count")
    public ResponseEntity<Map<String, Object>> countUserPhotosAndCountries(
            @RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> response = imageService.countUserPhotosAndCountries(token);
            return ResponseEntity.ok(response);
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(null);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(null);
        }
    }

}
