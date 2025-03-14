package com.personalphotomap.model;

import jakarta.persistence.*;
import java.util.List;

@Entity
@Table(name = "albums")
public class Album {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String countryId;

    @ManyToMany
    @JoinTable(
        name = "album_images",
        joinColumns = @JoinColumn(name = "album_id"),
        inverseJoinColumns = @JoinColumn(name = "image_id")
    )
    private List<Image> images;

    public Album() {}

    public Album(String name, String countryId) {
        this.name = name;
        this.countryId = countryId;
    }

    public Long getId() { return id; }
    public String getName() { return name; }
    public String getCountryId() { return countryId; }
    public List<Image> getImages() { return images; }

    public void setName(String name) { this.name = name; }
    public void setCountryId(String countryId) { this.countryId = countryId; }
    public void setImages(List<Image> images) { this.images = images; }
}
