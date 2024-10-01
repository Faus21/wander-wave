package com.dama.wanderwave.security;

import com.dama.wanderwave.user.User;
import com.dama.wanderwave.user.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.core.userdetails.UsernameNotFoundException;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("UserDetailsServiceImpl Tests")
class UserDetailsServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private UserDetailsServiceImpl userDetailsService;

    @Nested
    @DisplayName("loadUserByUsername Method")
    class LoadUserByUsernameTests {

        @Test
        @DisplayName("Should Return UserDetails When User Exists")
        void loadUserByUsernameShouldReturnUserDetailsWhenUserExists() {
            String username = "test@example.com";
            User mockUser = new User();
            mockUser.setEmail(username);
            mockUser.setPassword("password");
            mockUser.setEnabled(true);

            when(userRepository.findByEmail(username)).thenReturn(Optional.of(mockUser));

            UserDetails userDetails = userDetailsService.loadUserByUsername(username);
            assertThat(userDetails).isNotNull();
            assertThat(userDetails.getUsername()).isEqualTo(username);
            verify(userRepository, times(1)).findByEmail(username);
        }

        @Test
        @DisplayName("Should Throw UsernameNotFoundException When User Does Not Exist")
        void loadUserByUsernameShouldThrowUsernameNotFoundExceptionWhenUserDoesNotExist() {
            String username = "test@example.com";

            when(userRepository.findByEmail(username)).thenReturn(Optional.empty());

            UsernameNotFoundException thrownException = assertThrows(UsernameNotFoundException.class, () -> userDetailsService.loadUserByUsername(username));

            assertThat(thrownException).hasMessageContaining("User not found");
            verify(userRepository, times(1)).findByEmail(username);
        }
    }
}