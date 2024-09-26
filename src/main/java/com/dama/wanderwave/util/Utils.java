package com.dama.wanderwave.util;

import com.dama.wanderwave.handler.BannedUserException;
import com.dama.wanderwave.user.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.stereotype.Component;

@Component
@Slf4j
@RequiredArgsConstructor(onConstructor_ = @__(@Autowired))
public class Utils {

    private final UserRepository userRepository;

    public void checkUserBan(Authentication authentication) {
        String authenticatedEmail = authentication.getName();
        var userOptional = userRepository.findByEmail(authenticatedEmail);

        if (userOptional.isEmpty()) {
            log.error("User '{}' not found.", authenticatedEmail);
            throw new UsernameNotFoundException("User not found");
        }

        var user = userOptional.get();

        if (user.isAccountLocked()) {
            log.error("Banned user '{}' attempted to perform an action.", authenticatedEmail);
            throw new BannedUserException("User is locked");
        }
    }
}
