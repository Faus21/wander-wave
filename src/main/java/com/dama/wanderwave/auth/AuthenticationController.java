package com.dama.wanderwave.auth;

import com.dama.wanderwave.handler.token.TokenRefreshException;
import com.dama.wanderwave.refreshToken.RefreshToken;
import com.dama.wanderwave.refreshToken.RefreshTokenService;
import com.dama.wanderwave.refreshToken.TokenRefreshRequest;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.utils.ResponseRecord;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
@Tag(name = "Authentication", description = "Endpoints for user authentication and registration")
public class AuthenticationController {

    private final AuthenticationService service;
    private final RefreshTokenService refreshTokenService;
    private final JwtService jwtService;

    @PostMapping("/register")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Register a new user", description = "Registers a new user and sends a validation email.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "User registered successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid request parameters", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> register(@RequestBody @Valid RegistrationRequest request) {
        String message = service.register(request);
        return ResponseEntity.accepted().body(new ResponseRecord(HttpStatus.ACCEPTED.value(), message));
    }

    @PostMapping("/authenticate")
    @ResponseStatus(HttpStatus.OK)
    @Operation(summary = "Authenticate a user", description = "Authenticates a user and returns a JWT token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Authentication successful", content = @Content()),
            @ApiResponse(responseCode = "401", description = "Invalid credentials", content = @Content()),
            @ApiResponse(responseCode = "404", description = "User not found", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<AuthenticationResponse> authenticate(@Valid @RequestBody AuthenticationRequest request) {
        AuthenticationResponse response = service.authenticate(request);
        return ResponseEntity.ok(response);
    }

	@PostMapping("/logout")
	@ResponseStatus(HttpStatus.OK)
	@Operation(summary = "Log out a user", description = "Logs out a user by invalidating their token.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Logout successful", content = @Content()),
			@ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
	})
	public ResponseEntity<ResponseRecord> logout() {
		var response = service.logout();
		return ResponseEntity.ok(new ResponseRecord(HttpStatus.OK.value(), response));
	}


	@GetMapping("/activate-account")
    @ResponseStatus(HttpStatus.ACCEPTED)
    @Operation(summary = "Activate user account", description = "Activates a user account using the provided token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Account activated successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Expired token", content = @Content()),
            @ApiResponse(responseCode = "404", description = "Invalid token", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> activateAccount(@RequestParam String emailToken) {
        String jwtToken = service.activateAccount(emailToken);
        return ResponseEntity.accepted().body(new ResponseRecord(HttpStatus.ACCEPTED.value(), jwtToken));
    }

    @GetMapping("/recover-account")
    @Operation(summary = "Recover user account", description = "Sends a token for password recovery.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "202", description = "Account recovered successfully", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> recoverByEmail(@RequestParam String email) {
        String responseMessage = service.recoverAccount(email);
        return ResponseEntity.accepted().body(new ResponseRecord(HttpStatus.ACCEPTED.value(), responseMessage));
    }

    @PostMapping("/change-password")
    @Operation(summary = "Change user password", description = "Changes user password using the provided token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Password changed successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid or expired token", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> changePassword(@RequestBody @Valid RecoveryRequest request) {
        ResponseRecord responseRecord = service.changeUserPassword(request.getToken(), request.getPassword());
        return ResponseEntity.ok().body(responseRecord);
    }

    @PostMapping("/refresh-token")
    @Operation(summary = "Refresh access token", description = "Refreshes the access token using a valid refresh token.")
    @ApiResponses(value = {
            @ApiResponse(responseCode = "200", description = "Token refreshed successfully", content = @Content()),
            @ApiResponse(responseCode = "400", description = "Invalid or expired refresh token", content = @Content()),
            @ApiResponse(responseCode = "500", description = "Internal server error", content = @Content())
    })
    public ResponseEntity<ResponseRecord> refreshToken(@RequestBody TokenRefreshRequest request) {
        String requestRefreshToken = request.getRefreshToken();

        return refreshTokenService.findByToken(requestRefreshToken)
                .map(refreshTokenService::verifyExpiration)
                .map(RefreshToken::getUser)
                .map(user -> {
                    Map<String, Object> claims = Map.of("username", user.getNickname());
                    String newAccessToken = jwtService.generateToken(claims, user);
                    return ResponseEntity.ok()
                            .body(ResponseRecord.builder()
                                    .code(HttpStatus.OK.value())
                                    .message(newAccessToken)
                                    .build());
                })
                .orElseThrow(() -> new TokenRefreshException("Refresh token is invalid!"));
    }


}