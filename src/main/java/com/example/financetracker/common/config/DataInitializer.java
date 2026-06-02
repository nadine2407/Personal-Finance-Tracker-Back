package com.example.financetracker.common.config;

import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class DataInitializer implements ApplicationRunner {

    private final CategoryRepository categoryRepository;

    // Mapping: old English name → correct French name
    private static final Map<String, String> RENAME_MAP = Map.ofEntries(
        Map.entry("Food",           "Alimentation"),
        Map.entry("Housing",        "Logement"),
        Map.entry("Health",         "Santé"),
        Map.entry("Entertainment",  "Loisirs"),
        Map.entry("Education",      "Éducation"),
        Map.entry("Travel",         "Voyages"),
        Map.entry("Salary",         "Salaire"),
        Map.entry("Investment",     "Investissement"),
        Map.entry("Others",         "Autres"),
        Map.entry("Other",          "Autres"),
        Map.entry("Groceries",      "Alimentation"),
        Map.entry("Rent",           "Logement")
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        renameOldCategories();
        seedDefaultCategories();
    }

    /**
     * Renames old English default category names to their French equivalents.
     * Only renames when the French equivalent does not yet exist (avoids duplicates).
     * Never deletes — categories may have transactions linked.
     */
    private void renameOldCategories() {
        List<Category> allDefaults = categoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsDefault()))
                .toList();

        for (Category cat : allDefaults) {
            String frenchName = RENAME_MAP.get(cat.getName());
            if (frenchName == null) continue; // already French or not in map

            boolean frenchAlreadyExists = allDefaults.stream()
                    .anyMatch(c -> c.getName().equals(frenchName));

            if (!frenchAlreadyExists) {
                cat.setName(frenchName);
                categoryRepository.save(cat);
            }
            // If French name already exists, leave both — cannot delete due to FK constraints
        }
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
