package com.example.financetracker.common.config;

import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (!userRepository.existsByEmail("demo@lumen.app")) {
            userRepository.save(User.builder()
                    .email("demo@lumen.app")
                    .passwordHash(passwordEncoder.encode("demo123"))
                    .firstName("Demo")
                    .lastName("User")
                    .build());
        }
    }
}
