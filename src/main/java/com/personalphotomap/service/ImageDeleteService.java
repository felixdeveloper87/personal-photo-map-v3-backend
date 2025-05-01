package com.personalphotomap.service;

import com.personalphotomap.model.Album;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.AlbumRepository;
import com.personalphotomap.repository.ImageRepository;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

/**
 * ImageDeleteService
 *
 * Service responsible for handling asynchronous and safe deletion of images.
 *
 * Responsibilities:
 * - Removes image references from all associated albums.
 * - Deletes albums that become empty after image removal.
 * - Deletes image files from Amazon S3.
 * - Deletes image records from the database.
 * - Supports bulk deletion in parallel using CompletableFuture and @Async.
 *
 * This class is used to decouple deletion logic from the main ImageService,
 * ensuring better separation of concerns and performance in batch operations.
 */

@Service
public class ImageDeleteService {

    private final S3Service s3Service;
    private final ImageRepository imageRepository;
    private final AlbumRepository albumRepository;

    public ImageDeleteService(S3Service s3Service, ImageRepository imageRepository, AlbumRepository albumRepository) {
        this.s3Service = s3Service;
        this.imageRepository = imageRepository;
        this.albumRepository = albumRepository;
    }

    @Async
    public CompletableFuture<Void> deleteImage(Image image) {
        try {
            List<Album> albums = albumRepository.findByImageId(image.getId());
            for (Album album : albums) {
                if (album.getImages().removeIf(img -> img.getId().equals(image.getId()))) {
                    if (album.getImages().isEmpty()) {
                        albumRepository.delete(album);
                    } else {
                        albumRepository.save(album);
                    }
                }
            }

            s3Service.deleteFile(image.getFilePath());
            imageRepository.delete(image);

            return CompletableFuture.completedFuture(null);
        } catch (Exception e) {
            return CompletableFuture.failedFuture(e);
        }
    }

    public void deleteImagesInParallel(List<Image> images) {
        List<CompletableFuture<Void>> futures = images.stream()
                .map(this::deleteImage)
                .toList();

        CompletableFuture.allOf(futures.toArray(new CompletableFuture[0])).join();
    }
}
