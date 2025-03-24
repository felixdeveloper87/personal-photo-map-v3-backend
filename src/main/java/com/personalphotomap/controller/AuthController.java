package com.personalphotomap.controller;

import com.personalphotomap.dto.RegisterRequestDTO;
import com.personalphotomap.dto.UserDTO;
import com.personalphotomap.service.UserService;
import com.personalphotomap.dto.LoginRequestDTO;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;

import java.util.List;
import java.util.Map;

/**
 * Authentication controller responsible for handling user login,
 * registration, role updates, and user administration functionalities.
 */
@RestController
@RequestMapping("/api/auth")
public class AuthController {

    private final UserService userService;

    public AuthController(
            UserService userService) {

        this.userService = userService;
    }

    /**
     * Authenticates the user with email and password.
     * Returns a JWT token and user info on success.
     *
     * @param loginRequest DTO with login credentials
     * @return ResponseEntity with JWT and user info or 401 error
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody LoginRequestDTO loginRequest) {
        try {
            Map<String, String> response = userService.authenticateUser(loginRequest);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        }
    }

    /**
     * Registers a new user in the system.
     * Validates uniqueness and sets default role.
     *
     * @param registerRequest DTO with registration info
     * @return Success message or conflict if email is already in use
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@Valid @RequestBody RegisterRequestDTO registerRequest) {
        String result = userService.registerUser(registerRequest);

        if (result.equals("Email is already in use.")) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body(result);
        }

        return ResponseEntity.ok(result);
    }

     /**
     * Retrieves a list of all users (for admin or diagnostic use).
     *
     * @return List of UserDTOs
     */
    @GetMapping("/users")
    public ResponseEntity<List<UserDTO>> getAllUsers() {
        return ResponseEntity.ok(userService.getAllUsers());
    }

    /**
     * Deletes a user by ID.
     * Typically used in administrative scenarios.
     *
     * @param id The ID of the user to be deleted
     * @return A success or not-found response
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        boolean deleted = userService.deleteUserById(id);
        if (!deleted) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }
        return ResponseEntity.ok("User successfully deleted.");
    }

    /**
     * Upgrades the authenticated user to Premium.
     * Extracts user info from JWT token and updates role.
     *
     * @param token Authorization header containing Bearer token
     * @return Confirmation of premium upgrade
     */
    @PutMapping("/users/make-premium")
    public ResponseEntity<?> makeCurrentUserPremium(@RequestHeader("Authorization") String token) {
        try {
            Map<String, Object> response = userService.upgradeCurrentUserToPremium(token);
            return ResponseEntity.ok(response);
        } catch (SecurityException e) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body(e.getMessage());
        } catch (IllegalArgumentException e) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body(e.getMessage());
        }
    }

}
