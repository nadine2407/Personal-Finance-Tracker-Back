package com.example.financetracker.domain.auth;

import com.example.financetracker.common.exception.EmailAlreadyUsedException;
import com.example.financetracker.common.exception.ResourceNotFoundException;
import com.example.financetracker.domain.auth.dto.AuthResponse;
import com.example.financetracker.domain.auth.dto.UpdatePasswordRequest;
import com.example.financetracker.domain.auth.dto.UpdateProfileRequest;
import com.example.financetracker.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
@RequiredArgsConstructor
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthResponse.UserInfo getProfile() {
        User user = currentUser();
        return new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
    }

    public AuthResponse updateProfile(UpdateProfileRequest request) {
        User user = currentUser();
        if (userRepository.existsByEmailAndIdNot(request.getEmail(), user.getId())) {
            throw new EmailAlreadyUsedException(request.getEmail());
        }
        user.setFirstName(request.getFirstName());
        user.setLastName(request.getLastName());
        user.setEmail(request.getEmail());
        userRepository.save(user);
        String newToken = jwtService.generateToken(user);
        AuthResponse.UserInfo userInfo = new AuthResponse.UserInfo(user.getId(), user.getEmail(), user.getFirstName(), user.getLastName());
        return new AuthResponse(newToken, userInfo);
    }

    public void updatePassword(UpdatePasswordRequest request) {
        User user = currentUser();
        if (!passwordEncoder.matches(request.getCurrentPassword(), user.getPasswordHash())) {
            throw new IllegalArgumentException("Mot de passe actuel incorrect");
        }
        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        userRepository.save(user);
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("User", 0L));
    }
}
