package com.login.login.dto;

import jakarta.validation.constraints.*;

public record RegisterFormDto(
  @Email @NotBlank String email,
  @NotBlank @Size(min = 8) String password,
  @NotBlank String name
) {}
