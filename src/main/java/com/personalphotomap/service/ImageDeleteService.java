package com.personalphotomap.service;

import com.personalphotomap.model.Album;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.AlbumRepository;
import com.personalphotomap.repository.ImageRepository;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

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
            // Remove de todos os álbuns
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

            // Remove do S3 e do banco
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
