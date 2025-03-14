package com.personalphotomap.controller;

import com.personalphotomap.model.Album;
import com.personalphotomap.model.Image;
import com.personalphotomap.service.AlbumService;
import com.personalphotomap.repository.AlbumRepository;
import com.personalphotomap.repository.ImageRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import com.personalphotomap.security.JwtUtil;
import java.util.Optional;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/albums")
public class AlbumController {

    @Autowired
    private AlbumRepository albumRepository;

    @Autowired
    private JwtUtil jwtUtil;

    @Autowired
    private AlbumService albumService;

    @PostMapping
    public ResponseEntity<Album> createAlbum(@RequestBody Map<String, Object> requestBody) {
        String name = (String) requestBody.get("albumName");
        String countryId = (String) requestBody.get("countryId");
        List<Long> imageIds = ((List<Integer>) requestBody.get("imageIds")).stream().map(Long::valueOf).toList();

        Album album = albumService.createAlbum(name, countryId, imageIds);
        return ResponseEntity.ok(album);
    }

    @GetMapping("/{countryId}")
    public ResponseEntity<List<Album>> getAlbumsByCountry(@PathVariable String countryId) {
        List<Album> albums = albumService.getAlbumsByCountry(countryId);
        return ResponseEntity.ok(albums);
    }

    @GetMapping("/{albumId}/images")
    public ResponseEntity<List<Image>> getImagesByAlbum(@PathVariable Long albumId) {
        List<Image> images = albumService.getImagesByAlbum(albumId);
        return ResponseEntity.ok(images);
    }

    @DeleteMapping("/{albumId}")
    public ResponseEntity<?> deleteAlbum(@PathVariable Long albumId,
            @RequestHeader(value = "Authorization") String token) {
        String email = jwtUtil.extractUsernameFromToken(token);
        if (email == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Token inválido");
        }

        Optional<Album> optionalAlbum = albumRepository.findById(albumId);
        if (optionalAlbum.isPresent()) {
            Album album = optionalAlbum.get();

            // 🔥 Remover a relação entre imagens e álbum corretamente
            album.getImages().clear();
            albumRepository.save(album); // Salva a atualização para garantir que a relação seja removida

            // 🔥 Agora podemos deletar o álbum
            albumRepository.delete(album);

            return ResponseEntity.ok("Álbum deletado com sucesso.");
        } else {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("Álbum não encontrado.");
        }
    }
}