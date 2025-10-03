package com.login.login.dto;

import jakarta.validation.constraints.*;

public record ResetFormDto(
    @NotBlank 
    @Size(min = 8) 
    String newpassword
) {
    
}
