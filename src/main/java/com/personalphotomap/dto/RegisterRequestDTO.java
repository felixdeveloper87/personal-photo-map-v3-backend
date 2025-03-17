package com.personalphotomap.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class RegisterRequestDTO {

    @NotBlank(message = "O nome completo não pode estar vazio")
    @Size(min = 3, message = "O nome completo deve ter pelo menos 3 caracteres")
    private String fullname;

    @NotBlank(message = "O email não pode estar vazio")
    @Email(message = "Formato de email inválido")
    private String email;

    @NotBlank(message = "A senha não pode estar vazia")
    @Size(min = 6, message = "A senha deve ter pelo menos 6 caracteres")
    private String password;

    @NotBlank(message = "O país não pode estar vazio")
    private String country;

    // Construtores
    public RegisterRequestDTO() {
    }

    public RegisterRequestDTO(String fullname, String email, String password, String country) {
        this.fullname = fullname;
        this.email = email;
        this.password = password;
        this.country = country;
    }

    // Getters e Setters
    public String getFullname() {
        return fullname;
    }

    public void setFullname(String fullname) {
        this.fullname = fullname;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getCountry() {
        return country;
    }

    public void setCountry(String country) {
        this.country = country;
    }
}
