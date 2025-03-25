package com.personalphotomap.service;

import com.personalphotomap.model.Album;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.AlbumRepository;
import com.personalphotomap.repository.ImageRepository;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ImageDeleteService {

    private static final Logger logger = LoggerFactory.getLogger(ImageDeleteService.class);

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

    // @Async // 🔥 Cada imagem será deletada em uma thread separada
    // public CompletableFuture<Void> deleteImageAsync(Image image) {
    // String threadName = Thread.currentThread().getName();
    // try {
    // logger.info("🗑️ Deletando imagem: {} na thread: {}", image.getFilePath(),
    // threadName);
    // s3Service.deleteFile(image.getFilePath()); // Deletar do S3
    // imageRepository.delete(image); // Remover do banco de dados
    // logger.info("✅ Imagem deletada com sucesso: {} | Thread: {}",
    // image.getFilePath(), threadName);
    // return CompletableFuture.completedFuture(null);
    // } catch (Exception e) {
    // logger.error("❌ Erro ao deletar a imagem: {} | Thread: {}",
    // image.getFilePath(), threadName, e);
    // return CompletableFuture.failedFuture(e);
    // }
    // }
}
