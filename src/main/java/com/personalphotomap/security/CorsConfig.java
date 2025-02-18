//package com.personalphotomap.security;
//
//import org.springframework.context.annotation.Bean;
//import org.springframework.context.annotation.Configuration;
//import org.springframework.web.cors.CorsConfiguration;
//import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
//import org.springframework.web.cors.CorsConfigurationSource;
//
//import java.util.Arrays;
//import java.util.List;
//
//@Configuration
//public class CorsConfig {
//
//    @Bean
//    public CorsConfigurationSource corsConfigurationSource() {
//        CorsConfiguration configuration = new CorsConfiguration();
//
//        // Obtém a URL do frontend do ambiente (Render)
//        String frontendUrl = System.getenv("FRONTEND_URL");
//
//        System.out.println("🚀 FRONTEND_URL carregado: " + frontendUrl); // Log para debug
//
//        // Lista de origens permitidas
//        List<String> allowedOrigins = List.of(
//                "http://localhost:5173",
//                "http://localhost",
//                "http://localhost:80",
//                "https://personal-photo-map-v2.onrender.com" // URL do frontend no Render
//        );
//
//        if (frontendUrl != null && !frontendUrl.isEmpty()) {
//            allowedOrigins = List.of(frontendUrl);
//        }
//
//        configuration.setAllowedOrigins(allowedOrigins);
//        configuration.setAllowCredentials(true);
//        configuration.setAllowedMethods(Arrays.asList("GET", "POST", "PUT", "DELETE", "OPTIONS"));
//        configuration.setAllowedHeaders(List.of("Authorization", "Content-Type", "Accept"));
//        configuration.setExposedHeaders(List.of("Authorization"));
//
//        UrlBasedCorsConfigurationSource source = new UrlBasedCorsConfigurationSource();
//        source.registerCorsConfiguration("/**", configuration);
//
//        return source;
//    }
//}
