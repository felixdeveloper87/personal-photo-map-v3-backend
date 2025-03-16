package com.personalphotomap.repository;

import com.personalphotomap.model.AppUser;
import com.personalphotomap.model.Image;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

@Repository
public interface ImageRepository extends JpaRepository<Image, Long> {

    // 🔥 Buscar todas as imagens de um país
    List<Image> findByCountryId(String countryId);

    // 🔥 Contar o número de fotos do usuário
    @Query("SELECT COUNT(i) FROM Image i WHERE i.user.id = :userId")
    long countByUserId(@Param("userId") Long userId);

    // 🔥 Buscar imagens por país e ano
    List<Image> findByCountryIdAndYear(String countryId, int year);

    // 🔥 Buscar todas as imagens do usuário
    List<Image> findByUserId(Long userId);

    // 🔥 Buscar imagens do usuário em um país específico
     
    // 🔥 Buscar imagens do usuário em um país e ano específico
    List<Image> findByUserAndCountryIdAndYear(AppUser user, String countryId, int year);


    // 🔥 Buscar todas as imagens do usuário ordenadas pela data de upload
    List<Image> findByUserIdOrderByUploadDateDesc(Long userId);

    // 🔥 Buscar imagens do usuário em um país específico
    List<Image> findByCountryIdAndUserId(String countryId, Long userId);

    // 🔥 Buscar imagens do usuário em um país e ano específico
    List<Image> findByCountryIdAndYearAndUserId(String countryId, int year, Long userId);

    // 🔥 Buscar países distintos onde o usuário tem imagens
    @Query("SELECT DISTINCT i.countryId FROM Image i WHERE i.user.id = :userId")
    List<String> findDistinctCountryIdsByUserId(@Param("userId") Long userId);

    // 🔥 Buscar todas as imagens ordenadas por uploadDate
    @Query("SELECT i FROM Image i ORDER BY i.uploadDate DESC")
    List<Image> findAllOrderedByUploadDateDesc();

    // 🔥 Buscar anos distintos onde o usuário tem imagens
    @Query("SELECT DISTINCT i.year FROM Image i WHERE i.user.id = :userId ORDER BY i.year DESC")
    List<Integer> findDistinctYearsByUserId(@Param("userId") Long userId);

    // 🔥 Buscar imagens do usuário por ano
    @Query("SELECT i FROM Image i WHERE i.user.id = :userId AND i.year = :year ORDER BY i.uploadDate DESC")
    List<Image> findByUserIdAndYear(@Param("userId") Long userId, @Param("year") Integer year);

    // 🔥 Buscar anos distintos onde o usuário tem imagens em um país específico
    @Query("SELECT DISTINCT i.year FROM Image i WHERE i.countryId = :countryId AND i.user.id = :userId ORDER BY i.year")
    List<Integer> findDistinctYearsByCountryIdAndUserId(@Param("countryId") String countryId,
            @Param("userId") Long userId);

    // 🔥 Contar países distintos onde o usuário tem imagens
    @Query("SELECT COUNT(DISTINCT i.countryId) FROM Image i WHERE i.user.id = :userId")
    long countDistinctCountryByUserId(@Param("userId") Long userId);
}
