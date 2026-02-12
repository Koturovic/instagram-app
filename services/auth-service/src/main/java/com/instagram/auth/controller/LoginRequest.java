package com.instagram.auth.controller;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class LoginRequest {

    @NotBlank(message = "Email je obavezan") // polje ne sme biti prazno
    @Email(message = "Email mora biti validan") // email mora biti validan
    private String email;

    @NotBlank(message = "Lozinka je obavezna") // polje ne sme biti prazno
    private String password;
}
