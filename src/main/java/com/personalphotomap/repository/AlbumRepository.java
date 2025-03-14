package com.personalphotomap.repository;

import com.personalphotomap.model.Album;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import java.util.List;

@Repository
public interface AlbumRepository extends JpaRepository<Album, Long> {
    List<Album> findByCountryId(String countryId);

    @Query("SELECT a FROM Album a JOIN a.images i WHERE i.id = :imageId")
    List<Album> findByImageId(@Param("imageId") Long imageId);

}
