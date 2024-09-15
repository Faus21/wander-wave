package com.dama.wanderwave.auth;

import com.dama.wanderwave.handler.TokenExpiredException;
import com.dama.wanderwave.role.RoleRepository;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.token.Token;
import com.dama.wanderwave.token.TokenRepository;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import jakarta.mail.MessagingException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

@Service
@RequiredArgsConstructor(onConstructor = @__(@Autowired))
public class AuthenticationService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;

    private final SecureRandom secureRandom = new SecureRandom();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    @Transactional
    public void register(@Valid RegistrationRequest request) {
        User user = createUser(request);
        userRepository.save(user);

        sendValidationEmail(user);
    }

    private User createUser(RegistrationRequest request) {
        var userRole = roleRepository.findByName("USER")
                               .orElseThrow(() -> new IllegalStateException("ROLE USER was not initiated"));

        return User.builder()
                       .nickname(request.getUsername())
                       .email(request.getEmail())
                       .password(passwordEncoder.encode(request.getPassword()))
                       .accountLocked(false)
                       .enabled(false)
                       .roles(Set.of(userRole))
                       .build();
    }

    public AuthenticationResponse authenticate(AuthenticationRequest request) {
        var auth = authenticationManager.authenticate(
                new UsernamePasswordAuthenticationToken(request.getEmail(), request.getPassword())
        );

        var user = (User) auth.getPrincipal();
        var claims = createClaims(user);

        var jwtToken = jwtService.generateToken(claims, user);
        return AuthenticationResponse.builder().token(jwtToken).build();
    }

    private Map<String, Object> createClaims(User user) {
        var claims = new HashMap<String, Object>();
        claims.put("username", user.getNickname());
        return claims;
    }

    @Transactional
    public void activateAccount(String token) throws MessagingException {
        Token savedToken = findTokenOrThrow(token);

        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendValidationEmail(savedToken.getUser());
            throw new TokenExpiredException("Activation token has expired. A new token has been sent to the same " +
                                 "email address.");
        }

        activateUser(savedToken.getUser());
        markTokenAsValidated(savedToken);
    }

    private Token findTokenOrThrow(String token) {
        return tokenRepository.findByContent(token)
                       .orElseThrow(() -> new RuntimeException("Invalid token"));
    }

    private void activateUser(User user) {
        var userToUpdate = userRepository.findById(user.getId())
                                   .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        userToUpdate.setEnabled(true);
        userRepository.save(userToUpdate);
    }

    private void markTokenAsValidated(Token token) {
        token.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private String generateAndSaveActivationToken(User user) {
        String tokenContent = generateActivationCode(6);
        Token token = Token.builder()
                              .content(tokenContent)
                              .createdAt(LocalDateTime.now())
                              .expiresAt(LocalDateTime.now().plusMinutes(15))
                              .user(user)
                              .build();
        tokenRepository.save(token);
        return tokenContent;
    }

    private void sendValidationEmail(User user)  {
        String newToken = generateAndSaveActivationToken(user);
        // TODO: Implement email sending logic using newToken and activationUrl

    }

    public String generateActivationCode(int length) {
        if (length < 1) {
            throw new IllegalArgumentException("Length must be greater than 0");
        }

        return IntStream.range(0, length)
                       .map(i -> secureRandom.nextInt(CHARACTERS.length()))
                       .mapToObj(CHARACTERS::charAt)
                       .map(String::valueOf)
                       .collect(Collectors.joining());
    }
}
