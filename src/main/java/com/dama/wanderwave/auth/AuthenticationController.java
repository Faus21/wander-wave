package com.dama.wanderwave.auth;

import com.dama.wanderwave.handler.TokenRefreshException;
import com.dama.wanderwave.refresh_token.RefreshToken;
import com.dama.wanderwave.refresh_token.RefreshTokenService;
import com.dama.wanderwave.refresh_token.TokenRefreshRequest;
import com.dama.wanderwave.security.JwtService;
import io.swagger.v3.oas.annotations.Operation;
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
			@ApiResponse(responseCode = "202", description = "User registered successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid request parameters"),
			@ApiResponse(responseCode = "500", description = "Internal server error")})
	public ResponseEntity<ResponseRecord> register( @RequestBody @Valid RegistrationRequest request ) {
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
			@ApiResponse(responseCode = "500", description = "Internal server error")})
	public ResponseEntity<AuthenticationResponse> authenticate( @Valid @RequestBody AuthenticationRequest request ) {
		AuthenticationResponse response = service.authenticate(request);
		return ResponseEntity.ok(response);
	}


	@GetMapping("/activate-account")
	@Operation(summary = "Activate user account", description = "Activates a user account using the provided token.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Account activated successfully"),
			@ApiResponse(responseCode = "400", description = "Expired token"),
			@ApiResponse(responseCode = "404", description = "Invalid token"),
			@ApiResponse(responseCode = "500", description = "Internal server error")})
	public ResponseEntity<ResponseRecord> activateAccount( @RequestParam String emailToken ) {
		String jwtToken = service.activateAccount(emailToken);
		return ResponseEntity.accepted().body(new ResponseRecord(202, jwtToken));
	}

	@GetMapping("/recover-account")
	@Operation(summary = "Recover user account", description = "Sends a token for password recovery.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "202", description = "Account recovered successfully"),
			@ApiResponse(responseCode = "500", description = "Internal server error")
	})
	public ResponseEntity<ResponseRecord> recoverByEmail( @RequestParam String email ) {
		String responseMessage = service.recoverAccount(email);
		return ResponseEntity.accepted().body(new ResponseRecord(202, responseMessage));
	}

	@PostMapping("/change-password")
	@Operation(summary = "Change user password", description = "Changes user password using the provided token.")
	@ApiResponses(value = {
			@ApiResponse(responseCode = "200", description = "Password changed successfully"),
			@ApiResponse(responseCode = "400", description = "Invalid or expired token"),
			@ApiResponse(responseCode = "500", description = "Internal server error")})
	public ResponseEntity<ResponseRecord> changePassword( @RequestBody @Valid RecoveryRequest request ) {
		ResponseRecord responseRecord = service.changeUserPassword(request.getToken(), request.getPassword());
		return ResponseEntity.ok().body(responseRecord);
	}


	@PostMapping("/refresh-token")
	public ResponseEntity<ResponseRecord> refreshToken(@RequestBody TokenRefreshRequest request) {
		String requestRefreshToken = request.getRefreshToken();

		return refreshTokenService.findByToken(requestRefreshToken)
				       .map(refreshTokenService::verifyExpiration)
				       .map(RefreshToken::getUser)
				       .map(user -> {

					       Map<String, Object> claims = Map.of("username", user.getNickname());

					       String newAccessToken =
							       jwtService.generateToken(claims, user);

					       return ResponseEntity.ok()
							              .body(ResponseRecord.builder()
									                    .code(200)
									                    .message(newAccessToken)
									                    .build());
				       })
				       .orElseThrow(() -> new TokenRefreshException("Refresh token is invalid!"));
	}


}
