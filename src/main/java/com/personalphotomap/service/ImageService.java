package com.personalphotomap.service;

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
    private final S3Service s3Service;
    private final UserRepository userRepository;
    private final JwtUtil jwtUtil;
    private final ImageUploadService imageUploadService;
    private final ImageDeleteService imageDeleteService;

    public ImageService(ImageRepository imageRepository,
            S3Service s3Service,
            UserRepository userRepository,
            JwtUtil jwtUtil,
            ImageUploadService imageUploadService,
            ImageDeleteService imageDeleteService) {
        this.imageRepository = imageRepository;
        this.s3Service = s3Service;
        this.userRepository = userRepository;
        this.jwtUtil = jwtUtil;
        this.imageUploadService = imageUploadService;
        this.imageDeleteService = imageDeleteService;
    }

    public List<String> handleUpload(List<MultipartFile> files, String countryId, int year, String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

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

    public void deleteAllImagesByCountry(String countryId, String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        List<Image> images = imageRepository.findByCountryIdAndUserId(countryId, user.getId());
        if (images.isEmpty()) {
            return;
        }

        List<CompletableFuture<Void>> futures = images.stream()
                .map(imageDeleteService::deleteImage)
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public void deleteImagesByCountryAndYear(String countryId, int year, String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        List<Image> images = imageRepository.findByCountryIdAndYearAndUserId(countryId, year, user.getId());
        if (images.isEmpty()) {
            return; // Ou pode lançar uma exceção se quiser retornar 404
        }

        List<CompletableFuture<Void>> futures = images.stream()
                .map(imageDeleteService::deleteImage)
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public void deleteImageById(Long imageId, String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        Image image = imageRepository.findById(imageId)
                .orElseThrow(() -> new IllegalArgumentException("Image not found."));

        if (!image.getUser().getId().equals(user.getId())) {
            throw new SecurityException("Você não tem permissão para deletar esta imagem.");
        }

        // Chama a service assíncrona, mas espera a finalização
        try {
            imageDeleteService.deleteImage(image).join();
        } catch (Exception e) {
            throw new RuntimeException("Erro ao deletar imagem: " + e.getMessage(), e);
        }
    }

    public void deleteMultipleImages(List<Long> imageIds, String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        List<Image> imagesToDelete = imageRepository.findAllById(imageIds);

        // Verifica se o usuário é dono de todas as imagens
        for (Image image : imagesToDelete) {
            if (!image.getUser().getId().equals(user.getId())) {
                throw new SecurityException("You do not have permission to delete some images.");
            }
        }

        // Deleção assíncrona com join
        List<CompletableFuture<Void>> futures = imagesToDelete.stream()
                .map(imageDeleteService::deleteImage)
                .collect(Collectors.toList());

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }

    public List<Image> getImagesByCountry(String countryId, String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        return imageRepository.findByCountryIdAndUserId(countryId, user.getId());
    }

    public List<String> getCountriesWithPhotos(String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        return imageRepository.findDistinctCountryIdsByUserId(user.getId());
    }

    public List<Integer> getAvailableYears(String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        return imageRepository.findDistinctYearsByUserId(user.getId());
    }

    public List<Image> getAllImages(String token, Integer year) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        if (year != null) {
            return imageRepository.findByUserIdAndYear(user.getId(), year);
        } else {
            return imageRepository.findByUserIdOrderByUploadDateDesc(user.getId());
        }
    }

    public List<Integer> getYearsByCountry(String countryId, String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        return imageRepository.findDistinctYearsByCountryIdAndUserId(countryId, user.getId());
    }

    public void deleteImages(List<Image> images) {
        for (Image image : images) {
            s3Service.deleteFile(image.getFilePath());
        }
        imageRepository.deleteAll(images);
    }

    public List<Image> getImagesByCountryAndYear(String countryId, int year, String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        return imageRepository.findByCountryIdAndYearAndUserId(countryId, year, user.getId());
    }

    public Map<String, Object> countUserPhotosAndCountries(String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            throw new IllegalArgumentException("Invalid or missing JWT token.");
        }

        AppUser user = userRepository.findByEmail(email);
        if (user == null) {
            throw new IllegalArgumentException("Usuário não encontrado.");
        }

        long photoCount = imageRepository.countByUserId(user.getId());
        long countryCount = imageRepository.countDistinctCountryByUserId(user.getId());

        Map<String, Object> response = new HashMap<>();
        response.put("photoCount", photoCount);
        response.put("countryCount", countryCount);
        return response;
    }

}