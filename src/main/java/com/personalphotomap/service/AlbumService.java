package com.personalphotomap.service;

import com.personalphotomap.dto.AlbumRequestDTO;
import com.personalphotomap.dto.AlbumResponseDTO;
import com.personalphotomap.model.Album;
import com.personalphotomap.model.AppUser;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.AlbumRepository;
import com.personalphotomap.repository.ImageRepository;
import com.personalphotomap.repository.UserRepository;
import com.personalphotomap.security.JwtUtil;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * Service layer responsible for handling business logic related to albums,
 * including creation, retrieval, deletion, and user validation.
 */
@Service
public class AlbumService {

    private final AlbumRepository albumRepository;
    private final ImageRepository imageRepository;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;

    public AlbumService(AlbumRepository albumRepository,
            ImageRepository imageRepository,
            UserRepository userRepository,
            JwtUtil jwtUtil) {
        this.albumRepository = albumRepository;
        this.imageRepository = imageRepository;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
    }

    /**
     * Creates a new album associated with the authenticated user and selected
     * images.
     *
     * @param request Album creation request DTO
     * @param token   JWT token from Authorization header
     * @return AlbumResponseDTO representing the created album
     */
    public AlbumResponseDTO createAlbumFromRequest(AlbumRequestDTO request, String token) {
        AppUser user = getUserFromToken(token);

        List<Image> selectedImages = imageRepository.findAllById(request.getImageIds());

        Album album = new Album(request.getAlbumName(), request.getCountryId());
        album.setUser(user);
        album.setImages(selectedImages);

        Album saved = albumRepository.save(album);
        return convertToDTO(saved);
    }

    /**
     * Retrieves all albums created by the authenticated user.
     *
     * @param token JWT token from Authorization header
     * @return List of albums owned by the user
     */
    public List<Album> getAlbumsByUser(String token) {
        AppUser user = getUserFromToken(token);
        return albumRepository.findByUser(user);
    }

    /**
     * Retrieves all albums created by the authenticated user for a given country.
     *
     * @param countryId Country identifier
     * @param token     JWT token from Authorization header
     * @return List of user-specific albums
     */
    public List<Album> getAlbumsByCountryAndUser(String countryId, String token) {
        AppUser user = getUserFromToken(token);
        return albumRepository.findByCountryIdAndUser(countryId, user);
    }

    /**
     * Retrieves all albums for a specific country.
     *
     * @param countryId Country identifier
     * @return List of albums
     */
    public List<Album> getAlbumsByCountry(String countryId) {
        return albumRepository.findByCountryId(countryId);
    }

    /**
     * Retrieves all images from a specific album.
     *
     * @param albumId Album identifier
     * @return List of images in the album
     */
    public List<Image> getImagesByAlbum(Long albumId) {
        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new NoSuchElementException("Album not found"));
        return album.getImages();
    }

    /**
     * Deletes the specified album if it belongs to the authenticated user.
     *
     * @param albumId Album identifier
     * @param token   JWT token from Authorization header
     */
    public void deleteAlbum(Long albumId, String token) {
        AppUser user = getUserFromToken(token);

        Album album = albumRepository.findById(albumId)
                .orElseThrow(() -> new NoSuchElementException("Album not found"));

        if (!album.getUser().getId().equals(user.getId())) {
            throw new SecurityException("You are not authorized to delete this album");
        }

        album.getImages().clear(); // Prevent FK constraint
        albumRepository.save(album);
        albumRepository.delete(album);
    }

    /**
     * Retrieves all albums in the system (admin use).
     *
     * @return List of all albums
     */
    public List<Album> getAllAlbums() {
        return albumRepository.findAll();
    }

    /**
     * Converts an Album entity to a response DTO.
     *
     * @param album Album entity
     * @return AlbumResponseDTO
     */
    public AlbumResponseDTO convertToDTO(Album album) {
        return new AlbumResponseDTO(
                album.getId(),
                album.getName(),
                album.getCountryId(),
                album.getUser().getId(),
                album.getImages() != null ? album.getImages().size() : 0);
    }

    /**
     * Helper method to extract the authenticated user from a JWT token.
     *
     * @param token JWT Authorization header
     * @return Authenticated AppUser
     */
    private AppUser getUserFromToken(String token) {
        if (token == null || !token.startsWith("Bearer ")) {
            throw new SecurityException("Invalid token format");
        }

        String email = jwtUtil.extractUsername(token.substring(7));
        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new NoSuchElementException("User not found");
        }

        return user;
    }
}
