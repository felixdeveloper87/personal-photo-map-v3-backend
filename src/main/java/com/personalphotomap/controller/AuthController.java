package com.personalphotomap.controller;

import com.personalphotomap.model.AppUser;
import com.personalphotomap.dto.RegisterRequest;
import com.personalphotomap.repository.UserRepository;
import com.personalphotomap.security.JwtUtil;
import com.personalphotomap.dto.LoginRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

/**
 * AuthController
 * 
 * This controller handles user authentication, registration, and user management operations.
 * It provides endpoints for user login, registration, retrieving users, and updating user roles.
 */
@RestController
@RequestMapping("/api/auth") // Base URL for authentication-related routes
public class AuthController {

    @Autowired
    private UserRepository userRepository; // Repository for user-related database operations

    @Autowired
    private JwtUtil jwtUtil; // Utility class for generating and validating JWT tokens

    @Autowired
    private PasswordEncoder passwordEncoder; // Password encryption utility

    /**
     * Handles user login authentication.
     *
     * - Checks if the user exists and verifies the password.
     * - If successful, generates a JWT token and returns user details.
     *
     * @param loginRequest Request body containing user email and password.
     * @return ResponseEntity with JWT token and user details if successful, or error message if authentication fails.
     */
    @PostMapping("/login")
    public ResponseEntity<?> login(@RequestBody LoginRequest loginRequest) {
        // Find the user by email
        AppUser user = userRepository.findByEmail(loginRequest.getEmail());

        // Validate user credentials
        if (user == null || !passwordEncoder.matches(loginRequest.getPassword(), user.getPassword())) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).body("Invalid credentials");
        }

        // Generate a JWT token upon successful authentication
        String token = jwtUtil.generateToken(user.getEmail());

        // Prepare response with token and user details
        Map<String, String> response = new HashMap<>();
        response.put("token", token);
        response.put("fullname", user.getFullname());
        response.put("email", user.getEmail());
        response.put("premium", String.valueOf(user.isPremium()));

        return ResponseEntity.ok(response);
    }

    /**
     * Handles user registration.
     *
     * - Checks if the email is already in use.
     * - Creates a new user with encrypted password and stores it in the database.
     *
     * @param registerRequest Request body containing user details (fullname, email, country, password).
     * @return ResponseEntity with success message or error if email is already registered.
     */
    @PostMapping("/register")
    public ResponseEntity<?> register(@RequestBody RegisterRequest registerRequest) {
        // Check if email is already registered
        if (userRepository.findByEmail(registerRequest.getEmail()) != null) {
            return ResponseEntity.status(HttpStatus.CONFLICT).body("Email is already in use.");
        }

        // Create a new user instance
        AppUser newUser = new AppUser();
        newUser.setFullname(registerRequest.getFullname());
        newUser.setEmail(registerRequest.getEmail());
        newUser.setCountry(registerRequest.getCountry());
        newUser.setPassword(passwordEncoder.encode(registerRequest.getPassword())); // Encrypt the password
        newUser.setRole("ROLE_USER"); // Default role assigned to new users

        // Save the new user in the database
        userRepository.save(newUser);

        return ResponseEntity.ok("User successfully registered.");
    }

    /**
     * Retrieves all registered users (for administrative purposes).
     *
     * @return ResponseEntity containing a list of all users.
     */
    @GetMapping("/users")
    public ResponseEntity<?> getAllUsers() {
        return ResponseEntity.ok(userRepository.findAll());
    }

    /**
     * Deletes a user by ID.
     *
     * - Checks if the user exists before deletion.
     *
     * @param id The ID of the user to delete.
     * @return ResponseEntity with success or error message.
     */
    @DeleteMapping("/users/{id}")
    public ResponseEntity<?> deleteUser(@PathVariable Long id) {
        if (!userRepository.existsById(id)) {
            return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
        }

        userRepository.deleteById(id);
        return ResponseEntity.ok("User successfully deleted.");
    }

    /**
     * Upgrades the authenticated user to a premium account.
     *
     * - Extracts the user from the JWT token.
     * - Updates the user's premium status in the database.
     *
     * @param token JWT token from the request header.
     * @return ResponseEntity confirming the premium upgrade.
     */
    @PutMapping("/users/make-premium")
    public ResponseEntity<?> makeCurrentUserPremium(@RequestHeader("Authorization") String token) {
        String jwt = token.substring(7); // Remove "Bearer " prefix
        String email = jwtUtil.extractUsername(jwt); // Extract email from token

        AppUser user = userRepository.findByEmail(email);

        if (user != null) {
            user.setPremium(true);
            userRepository.save(user);

            // Return confirmation response
            Map<String, Object> response = new HashMap<>();
            response.put("premium", true);
            response.put("message", "User upgraded to premium!");

            return ResponseEntity.ok(response);
        }

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body("User not found.");
    }
}
