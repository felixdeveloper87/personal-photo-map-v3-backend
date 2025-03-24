package com.personalphotomap.service;

import com.personalphotomap.model.Album;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.AlbumRepository;
import com.personalphotomap.repository.ImageRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.concurrent.CompletableFuture;


import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@Service
public class ImageDeleteService {

    private static final Logger logger = LoggerFactory.getLogger(ImageDeleteService.class);


    @Autowired
    private S3Service s3Service;

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private AlbumRepository albumRepository;

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
}
