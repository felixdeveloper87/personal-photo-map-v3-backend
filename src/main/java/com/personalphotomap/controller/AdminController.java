package com.personalphotomap.controller;

import com.personalphotomap.dto.RegisterRequestDTO;
import com.personalphotomap.dto.UserDTO;
import com.personalphotomap.dto.UserSummaryDTO;
import com.personalphotomap.service.UserService;
import com.personalphotomap.dto.LoginRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/admin")
@PreAuthorize("hasRole('ADMIN')") // já protege toda a classe
public class AdminController {

    private final UserService userService;

    public AdminController(UserService userService) {
        this.userService = userService;
    }

    @GetMapping("/users")
    public List<UserSummaryDTO> getAllUsersWithPhotoCount() {
        return userService.getAllUsersWithPhotoCount();
    }

    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUserAndImagesById(id);
        if (deleted) {
            return ResponseEntity.ok().build();
        } else {
            return ResponseEntity.notFound().build();
        }
    }
}
