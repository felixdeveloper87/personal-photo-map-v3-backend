package com.personalphotomap.service;

import com.personalphotomap.model.Album;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.AlbumRepository;
import com.personalphotomap.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import java.util.List;
import java.util.Optional;

@Service
public class AlbumService {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private ImageRepository imageRepository;

    public Album createAlbum(String name, String countryId, List<Long> imageIds) {
        System.out.println("🔎 Recebido no createAlbum -> name: " + name + ", countryId: " + countryId + ", imageIds: "
                + imageIds);

        List<Image> selectedImages = imageRepository.findAllById(imageIds);
        System.out.println("🔎 selectedImages size: " + selectedImages.size());

        Album album = new Album(name, countryId);
        album.setImages(selectedImages);

        Album saved = albumRepository.save(album);
        System.out.println("🔎 Álbum salvo -> id: " + saved.getId() + ", imagesCount: " + saved.getImages().size());

        return saved;
    }

    public List<Album> getAlbumsByCountry(String countryId) {
        return albumRepository.findByCountryId(countryId);
    }

    public List<Image> getImagesByAlbum(Long albumId) {
        Optional<Album> album = albumRepository.findById(albumId);
        return album.map(Album::getImages).orElseThrow(() -> new RuntimeException("Álbum não encontrado"));
    }
}
