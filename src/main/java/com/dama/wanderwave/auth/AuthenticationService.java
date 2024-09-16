package com.dama.wanderwave.auth;

import com.dama.wanderwave.email.EmailService;
import com.dama.wanderwave.handler.RoleNotFoundException;
import com.dama.wanderwave.handler.TokenExpiredException;
import com.dama.wanderwave.handler.UniqueConstraintViolationException;
import com.dama.wanderwave.role.RoleRepository;
import com.dama.wanderwave.security.JwtService;
import com.dama.wanderwave.token.Token;
import com.dama.wanderwave.token.TokenRepository;
import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import jakarta.mail.MessagingException;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.DataIntegrityViolationException;
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

    private static final Integer RECOVERY_TOKEN_LENGTH = 9;
    private static final Integer ACTIVATION_TOKEN_LENGTH = 6;

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final AuthenticationManager authenticationManager;
    private final RoleRepository roleRepository;
    private final TokenRepository tokenRepository;
    private final EmailService emailService;

    private final SecureRandom secureRandom = new SecureRandom();
    private static final String CHARACTERS = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";

    private static final Map<String, String> CONSTRAINT_NAME_TO_FIELD_MAP = Map.of(
            "uc_users_email", "email",
            "uc_users_nickname", "nickname"
    );


    @Transactional
    public void register(RegistrationRequest request) {
        try {
            checkForExistingUser(request.getUsername(), request.getEmail());
            User user = createUser(request);
            userRepository.save(user);
            sendValidationEmail(user);
        } catch (DataIntegrityViolationException ex) {
            handleDataIntegrityViolation(ex);
        }
    }

    private void handleDataIntegrityViolation(DataIntegrityViolationException ex) {
        if (ex.getCause() instanceof ConstraintViolationException cve) {
            String constraintName = cve.getConstraintName();
            String fieldName = CONSTRAINT_NAME_TO_FIELD_MAP.getOrDefault(constraintName, "unknown");
            throw new UniqueConstraintViolationException("Unique constraint violation: " + constraintName, fieldName);
        }
        throw ex;
    }

    private void checkForExistingUser(String username, String email) {
        if (userRepository.existsByNickname(username)) {
            throw new UniqueConstraintViolationException("Username already exists", "nickname");
        }
        if (userRepository.existsByEmail(email)) {
            throw new UniqueConstraintViolationException("Email already exists", "email");
        }
    }

    private User createUser(RegistrationRequest request) {
        var userRole = roleRepository.findByName("USER")
                               .orElseThrow(() -> new RoleNotFoundException("ROLE USER was not initiated"));

        return User.builder()
                       .nickname(request.getUsername())
                       .email(request.getEmail())
                       .description("")
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
    public void activateAccount(String token) {
        Token savedToken = findTokenOrThrow(token);

        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendValidationEmail(savedToken.getUser());
            throw new TokenExpiredException("Activation token has expired. A new token has been sent to the same " +
                                                    "email address.");
        }

        activateUser(savedToken.getUser());
        markTokenAsValidated(savedToken);
    }

    public void recoverAccount(String email) {
        var userToRecover = userRepository.findByEmail(email);
        userToRecover.ifPresent(this::sendRecoveryEmail);
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

    @Transactional
    public void changeUserPassword(String token, String password) {
        Token savedToken = findTokenOrThrow(token);

        if (LocalDateTime.now().isAfter(savedToken.getExpiresAt())) {
            sendRecoveryEmail(savedToken.getUser());
            throw new TokenExpiredException("Recovery token has expired. A new token has been sent to the same " +
                    "email address.");
        }

        changeUserPassword(savedToken.getUser(), password);
        markTokenAsValidated(savedToken);
    }

    private void changeUserPassword(User user, String password) {
        var userToUpdate = userRepository.findById(user.getId())
                .orElseThrow(() -> new UsernameNotFoundException("User not found"));
        userToUpdate.setPassword(passwordEncoder.encode(password));
        userRepository.save(userToUpdate);
    }

    private void markTokenAsValidated(Token token) {
        token.setValidatedAt(LocalDateTime.now());
        tokenRepository.save(token);
    }

    private String generateAndSaveActivationToken(User user) {
        String tokenContent = generateTokenCode(ACTIVATION_TOKEN_LENGTH);
        Token token = Token.builder()
                              .content(tokenContent)
                              .createdAt(LocalDateTime.now())
                              .expiresAt(LocalDateTime.now().plusMinutes(15))
                              .user(user)
                              .build();
        tokenRepository.save(token);
        return tokenContent;
    }

    private String generateAndSaveRecoveryToken(User user) {
        String tokenContent = generateTokenCode(RECOVERY_TOKEN_LENGTH);
        Token token = Token.builder()
                              .content(tokenContent)
                              .createdAt(LocalDateTime.now())
                              .expiresAt(LocalDateTime.now().plusMinutes(15))
                              .user(user)
                              .build();
        tokenRepository.save(token);
        return tokenContent;
    }

    private void sendValidationEmail(User user) {
        emailService.sendValidationEmail(generateAndSaveActivationToken(user), user.getEmail());
    }
    private void sendRecoveryEmail(User user) {
        emailService.sendRecoveryEmail(generateAndSaveRecoveryToken(user), user.getEmail());
    }

    public String generateTokenCode(int length) {
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
