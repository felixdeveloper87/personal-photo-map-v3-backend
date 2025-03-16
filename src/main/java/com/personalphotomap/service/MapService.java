package com.personalphotomap.service;

import com.personalphotomap.model.AppUser;
import com.personalphotomap.model.Image;
import com.personalphotomap.repository.ImageRepository;
import com.personalphotomap.repository.UserRepository;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MapService {

    @Autowired
    private ImageRepository imageRepository;

    @Autowired
    private UserRepository userRepository;

    public List<String> getImageUrlsByCountryAndUser(String countryId, String email) {
        Optional<AppUser> userOpt = Optional.ofNullable(userRepository.findByEmail(email));
        if (userOpt.isEmpty()) {
            throw new RuntimeException("Usuário não encontrado: " + email);
        }
        AppUser user = userOpt.get();

        // 🔥 Use user.getId() para buscar as imagens no repositório
        List<Image> images = imageRepository.findByCountryIdAndUserId(countryId, user.getId());

        String envBackendUrl = System.getenv("BACKEND_URL");
        final String backendUrl = (envBackendUrl == null || envBackendUrl.isEmpty())
                ? "http://localhost:8092"
                : envBackendUrl;

        return images.stream()
                .map(image -> backendUrl + image.getFilePath())
                .collect(Collectors.toList());
    }
}
