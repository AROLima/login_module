package com.login.login.dto;

import jakarta.validation.constraints.*;

public record LoginFormDto(
  @Email 
  @NotBlank 
  String email,
  @NotBlank 
  String password
) {}