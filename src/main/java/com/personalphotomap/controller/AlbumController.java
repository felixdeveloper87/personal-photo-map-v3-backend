package com.personalphotomap.controller;

import com.personalphotomap.dto.AlbumRequestDTO;
import com.personalphotomap.dto.AlbumResponseDTO;
import com.personalphotomap.model.Album;
import com.personalphotomap.model.Image;
import com.personalphotomap.service.AlbumService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.NoSuchElementException;

/**
 * REST controller for handling album-related operations.
 * Supports creation, retrieval, and deletion of albums.
 * All user-specific operations require a valid JWT for authentication.
 */
@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    private final AlbumService albumService;

    public AlbumController(AlbumService albumService) {
        this.albumService = albumService;
    }

    /**
     * Creates a new album for the authenticated user.
     * Associates the album with selected images and a specific country.
     *
     * @param request DTO containing album name, countryId, and imageIds
     * @param token   Bearer JWT token for authentication
     * @return The created album details or appropriate error
     */
    @PostMapping
    public ResponseEntity<?> createAlbum(@RequestBody AlbumRequestDTO request,
            @RequestHeader("Authorization") String token) {
        try {
            AlbumResponseDTO response = albumService.createAlbumFromRequest(request, token);
            return ResponseEntity.ok(response);
        } catch (SecurityException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Error creating album: " + e.getMessage());
        }
    }

    /**
     * Retrieves all albums created by the authenticated user.
     *
     * @param token Bearer JWT token for authentication
     * @return List of user-created albums or an authentication error
     */
    @GetMapping("/user")
    public ResponseEntity<?> getAllAlbumsByUser(@RequestHeader("Authorization") String token) {
        try {
            List<Album> albums = albumService.getAlbumsByUser(token);
            return ResponseEntity.ok(albums);
        } catch (SecurityException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Retrieves all albums created by the authenticated user for a specific
     * country.
     *
     * @param countryId Country identifier
     * @param token     Bearer JWT token for authentication
     * @return List of albums or an authentication error
     */
    @GetMapping("/user/{countryId}")
    public ResponseEntity<?> getUserAlbumsByCountry(@PathVariable String countryId,
            @RequestHeader("Authorization") String token) {
        try {
            List<Album> albums = albumService.getAlbumsByCountryAndUser(countryId, token);
            return ResponseEntity.ok(albums);
        } catch (SecurityException | NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Retrieves all albums created for a specific country (public).
     *
     * @param countryId Country identifier
     * @return List of albums associated with the country
     */
    @GetMapping("/{countryId}")
    public ResponseEntity<List<Album>> getAlbumsByCountry(@PathVariable String countryId) {
        return ResponseEntity.ok(albumService.getAlbumsByCountry(countryId));
    }

    /**
     * Retrieves all images associated with a specific album.
     *
     * @param albumId Album identifier
     * @return List of images in the album or 404 if not found
     */
    @GetMapping("/{albumId}/images")
    public ResponseEntity<?> getImagesByAlbum(@PathVariable Long albumId) {
        try {
            List<Image> images = albumService.getImagesByAlbum(albumId);
            return ResponseEntity.ok(images);
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Deletes an album owned by the authenticated user.
     * Verifies album ownership before performing deletion.
     *
     * @param albumId Album identifier
     * @param token   Bearer JWT token for authentication
     * @return Success message or error if not authorized or not found
     */
    @DeleteMapping("/{albumId}")
    public ResponseEntity<?> deleteAlbum(@PathVariable Long albumId,
            @RequestHeader("Authorization") String token) {
        try {
            albumService.deleteAlbum(albumId, token);
            return ResponseEntity.ok("Album successfully deleted");
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN).body(e.getMessage());
        } catch (NoSuchElementException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

    /**
     * Retrieves all albums stored in the system.
     * Intended for administrative or analytic purposes.
     *
     * @return List of all albums
     */
    @GetMapping("/all")
    public ResponseEntity<List<Album>> getAllAlbums() {
        return ResponseEntity.ok(albumService.getAllAlbums());
    }
}
