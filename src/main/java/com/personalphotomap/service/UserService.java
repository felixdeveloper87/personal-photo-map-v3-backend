package com.personalphotomap.service;

import com.personalphotomap.model.AppUser;
import com.personalphotomap.dto.RegisterRequestDTO;
import com.personalphotomap.repository.UserRepository;
import com.personalphotomap.controller.WebSocketController;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final WebSocketController webSocketController;

    public UserService(UserRepository userRepository, PasswordEncoder passwordEncoder, WebSocketController webSocketController) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
        this.webSocketController = webSocketController;
    }

    // Método para registrar novo usuário
    public String registerUser(RegisterRequestDTO registerRequest) {
        if (userRepository.findByEmail(registerRequest.getEmail()) != null) {
            return "Email já está em uso.";
        }

        AppUser newUser = new AppUser();
        newUser.setFullname(registerRequest.getFullname()); // Agora salva o fullname
        newUser.setEmail(registerRequest.getEmail());
        newUser.setCountry(registerRequest.getCountry()); // Agora salva o país
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword()));
        newUser.setRole("ROLE_USER");

        userRepository.save(newUser);
        return "Usuário registrado com sucesso.";
    }

    // Buscar usuário pelo email
    public Optional<AppUser> findByEmail(String email) {
        return Optional.ofNullable(userRepository.findByEmail(email));
    }

    // Tornar usuário premium e enviar notificação
    public void makeUserPremium(String email) {
        AppUser user = userRepository.findByEmail(email);
        if (user != null) {
            user.setPremium(true);
            userRepository.save(user);

            // Enviar notificação WebSocket
            webSocketController.sendNotification(email, "Parabéns! Você agora é um usuário Premium! 🎉");
        }
    }

    // Verificar milestones e enviar notificação
    public void checkAndSendCountryMilestone(String email) {
        AppUser user = userRepository.findByEmail(email);
        if (user != null) {
            int totalCountries = (int) user.getImages()
                                           .stream()
                                           .map(image -> image.getCountryId())
                                           .distinct()
                                           .count();

            List<Integer> milestones = List.of(10, 20, 30, 50, 100, 150, 195);
            if (milestones.contains(totalCountries)) {
                String message = "Você agora tem fotos em " + totalCountries + " países! 🌍✨";
                webSocketController.sendNotification(email, message);
            }
        }
    }
}
