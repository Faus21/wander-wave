package com.dama.wanderwave.auth;

import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Builder;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@Builder
public class RecoveryRequest {

    @NotEmpty(message = "Token is mandatory")
    @NotNull(message = "Token is mandatory")
    private String token;

    @Size(max = 100, message = "Password length must be less than or equal to 100 characters")
    @NotEmpty(message = "Password is mandatory")
    @NotNull(message = "Password is mandatory")
    @Size(min = 8, message = "Password should be 8 characters long minimum")
    private String password;

}
