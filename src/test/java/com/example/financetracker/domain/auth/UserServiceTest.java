package com.example.financetracker.domain.auth;

import com.example.financetracker.common.exception.EmailAlreadyUsedException;
import com.example.financetracker.domain.auth.dto.AuthResponse;
import com.example.financetracker.domain.auth.dto.UpdatePasswordRequest;
import com.example.financetracker.domain.auth.dto.UpdateProfileRequest;
import com.example.financetracker.security.JwtService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class UserServiceTest {

    @Mock private UserRepository userRepository;
    @Mock private PasswordEncoder passwordEncoder;
    @Mock private JwtService jwtService;

    @InjectMocks
    private UserService userService;

    private User user;

    @BeforeEach
    void setUp() {
        user = User.builder().id(1L).email("test@test.com")
                .firstName("Alice").lastName("Dupont").passwordHash("encoded_password").build();

        Authentication auth = mock(Authentication.class);
        when(auth.getName()).thenReturn("test@test.com");
        SecurityContext ctx = mock(SecurityContext.class);
        when(ctx.getAuthentication()).thenReturn(auth);
        SecurityContextHolder.setContext(ctx);

        when(userRepository.findByEmail("test@test.com")).thenReturn(Optional.of(user));
    }

    @Test
    void updateProfile_shouldUpdateUserAndReturnNewToken() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Bob");
        request.setLastName("Martin");
        request.setEmail("nouveau@test.com");

        when(userRepository.existsByEmailAndIdNot("nouveau@test.com", 1L)).thenReturn(false);
        when(userRepository.save(any())).thenReturn(user);
        when(jwtService.generateToken(any())).thenReturn("new_jwt_token");

        AuthResponse response = userService.updateProfile(request);

        assertThat(response.getToken()).isEqualTo("new_jwt_token");
        assertThat(response.getUser().getEmail()).isEqualTo("nouveau@test.com");
        assertThat(response.getUser().getFirstName()).isEqualTo("Bob");
        verify(userRepository).save(user);
    }

    @Test
    void updateProfile_withDuplicateEmail_shouldThrowEmailAlreadyUsedException() {
        UpdateProfileRequest request = new UpdateProfileRequest();
        request.setFirstName("Alice");
        request.setLastName("Dupont");
        request.setEmail("pris@test.com");

        when(userRepository.existsByEmailAndIdNot("pris@test.com", 1L)).thenReturn(true);

        assertThatThrownBy(() -> userService.updateProfile(request))
                .isInstanceOf(EmailAlreadyUsedException.class);

        verify(userRepository, never()).save(any());
    }

    @Test
    void updatePassword_withCorrectCurrentPassword_shouldEncodeAndSave() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setCurrentPassword("motdepasse");
        request.setNewPassword("nouveau123");

        when(passwordEncoder.matches("motdepasse", "encoded_password")).thenReturn(true);
        when(passwordEncoder.encode("nouveau123")).thenReturn("encoded_nouveau");
        when(userRepository.save(any())).thenReturn(user);

        assertThatNoException().isThrownBy(() -> userService.updatePassword(request));

        assertThat(user.getPasswordHash()).isEqualTo("encoded_nouveau");
        verify(userRepository).save(user);
    }

    @Test
    void updatePassword_withWrongCurrentPassword_shouldThrow() {
        UpdatePasswordRequest request = new UpdatePasswordRequest();
        request.setCurrentPassword("mauvais");
        request.setNewPassword("nouveau123");

        when(passwordEncoder.matches("mauvais", "encoded_password")).thenReturn(false);

        assertThatThrownBy(() -> userService.updatePassword(request))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("incorrect");

        verify(userRepository, never()).save(any());
    }

    @Test
    void getProfile_shouldReturnCurrentUserInfo() {
        AuthResponse.UserInfo profile = userService.getProfile();

        assertThat(profile.getId()).isEqualTo(1L);
        assertThat(profile.getEmail()).isEqualTo("test@test.com");
        assertThat(profile.getFirstName()).isEqualTo("Alice");
        assertThat(profile.getLastName()).isEqualTo("Dupont");
    }
}
