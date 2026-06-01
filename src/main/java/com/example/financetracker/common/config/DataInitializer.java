package com.example.financetracker.common.config;

import com.example.financetracker.domain.auth.User;
import com.example.financetracker.domain.auth.UserRepository;
import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final UserRepository userRepository;
    private final CategoryRepository categoryRepository;
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
        seedDefaultCategories();
    }

    private void seedDefaultCategories() {
        List<Object[]> defaults = List.of(
            new Object[]{"Alimentation",    "bi-basket",          "#e74c3c"},
            new Object[]{"Transport",       "bi-car-front",       "#3498db"},
            new Object[]{"Logement",        "bi-house",           "#2ecc71"},
            new Object[]{"Santé",           "bi-heart-pulse",     "#e91e63"},
            new Object[]{"Loisirs",         "bi-controller",      "#9b59b6"},
            new Object[]{"Shopping",        "bi-bag",             "#f39c12"},
            new Object[]{"Éducation",       "bi-book",            "#1abc9c"},
            new Object[]{"Voyages",         "bi-airplane",        "#00bcd4"},
            new Object[]{"Restaurants",     "bi-cup-hot",         "#ff5722"},
            new Object[]{"Salaire",         "bi-briefcase",       "#27ae60"},
            new Object[]{"Investissement",  "bi-graph-up-arrow",  "#2980b9"},
            new Object[]{"Autres",          "bi-three-dots",      "#95a5a6"}
        );

        for (Object[] cat : defaults) {
            String name = (String) cat[0];
            if (!categoryRepository.existsByNameAndIsDefault(name, true)) {
                categoryRepository.save(Category.builder()
                        .name(name)
                        .icon((String) cat[1])
                        .color((String) cat[2])
                        .isDefault(true)
                        .build());
            }
        }
    }
}
