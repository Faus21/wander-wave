package com.dama.wanderwave.auth;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

import java.io.IOException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
public class AuthenticationController {

    private final AuthenticationService service;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Register a new user", description = "Registers a new user and sends a validation email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "User registered successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> register( @RequestBody @Valid RegistrationRequest request ) throws MessagingException, IOException {
        String message = service.register(request);
        return ResponseEntity.accepted().body(new ResponseRecord(202, message));
    }

    @PostMapping("/authenticate")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Authenticate a user", description = "Authenticates a user and returns a JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful"),
            @ApiResponse(responseCode = "401", description = "Invalid credentials"),
            @ApiResponse(responseCode = "404", description = "User not found"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<AuthenticationResponse> authenticate(
            @Valid @RequestBody AuthenticationRequest request
    ) {
        AuthenticationResponse response = service.authenticate(request);
        return ResponseEntity.ok(response);
    }


    @GetMapping("/activate-account")
    @Operation(summary = "Activate user account", description = "Activates a user account using the provided token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Account activated successfully"),
            @ApiResponse(responseCode = "400", description = "Expired token"),
            @ApiResponse(responseCode = "404", description = "Invalid token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> activateAccount( @RequestParam String token ) throws MessagingException, IOException {
        String jwtToken = service.activateAccount(token);
        return ResponseEntity.accepted().body(new ResponseRecord(202, jwtToken));
    }

    @GetMapping("/recover-account")
    @Operation(summary = "Recover user account", description = "Sends a token for password recovery.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Account recovered successfully"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> recoverByEmail( @RequestParam String email ) throws MessagingException, IOException {
        String responseMessage = service.recoverAccount(email);
        return ResponseEntity.accepted().body(new ResponseRecord(202, responseMessage));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change user password", description = "Changes user password using the provided token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully"),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token"),
            @ApiResponse(responseCode = "500", description = "Internal server error")
    })
    public ResponseEntity<ResponseRecord> changePassword( @RequestBody @Valid RecoveryRequest request ) throws MessagingException, IOException {
        ResponseRecord responseRecord = service.changeUserPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok().body(responseRecord);
    }
}
