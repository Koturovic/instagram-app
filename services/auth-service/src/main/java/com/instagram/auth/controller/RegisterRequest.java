package com.instagram.auth.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
@Builder
public class RegisterRequest {

    @NotBlank(message = "Ime je obavezno")
    @Size(max = 255)
    private String firstName;

    @NotBlank(message = "Prezime je obavezno")
    @Size(max = 255)
    private String lastName;

    @NotBlank(message = "Username je obavezan")
    @Size(min = 3, max = 50)
    private String username;

    @NotBlank(message = "Email je obavezan")
    @Email(message = "Email mora biti validan")
    private String email;

    @NotBlank(message = "Lozinka je obavezna")
    @Size(min = 6, message = "Lozinka mora imati najmanje 6 karaktera")
    private String password;
}
