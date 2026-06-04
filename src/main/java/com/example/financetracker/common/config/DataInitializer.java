package com.example.financetracker.common.config;

import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryRepository;
import com.example.financetracker.domain.category.CategoryType;
import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
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

    @PersistenceContext
    private EntityManager em;

    // Mapping: non-canonical name → canonical French name
    // Covers both old English names and French synonyms/duplicates
    private static final Map<String, String> RENAME_MAP = Map.ofEntries(
        // English → French
        Map.entry("Food",            "Alimentation"),
        Map.entry("Groceries",       "Alimentation"),
        Map.entry("Housing",         "Logement"),
        Map.entry("Rent",            "Logement"),
        Map.entry("Health",          "Santé"),
        Map.entry("Entertainment",   "Loisirs"),
        Map.entry("Education",       "Éducation"),
        Map.entry("Travel",          "Voyages"),
        Map.entry("Salary",          "Salaire"),
        Map.entry("Investment",      "Investissement"),
        Map.entry("Others",          "Autres"),
        Map.entry("Other",           "Autres"),
        // French synonyms/duplicates → canonical French
        Map.entry("Nourriture",      "Alimentation"),
        Map.entry("loyer",           "Logement"),
        Map.entry("divertissement",  "Loisirs"),
        Map.entry("Investissements", "Investissement"),
        Map.entry("Vêtements",       "Shopping")
    );

    // Canonical type per default category French name
    private static final Map<String, CategoryType> TYPE_MAP = Map.ofEntries(
        Map.entry("Alimentation",   CategoryType.EXPENSE),
        Map.entry("Transport",      CategoryType.EXPENSE),
        Map.entry("Logement",       CategoryType.EXPENSE),
        Map.entry("Santé",          CategoryType.EXPENSE),
        Map.entry("Loisirs",        CategoryType.EXPENSE),
        Map.entry("Shopping",       CategoryType.EXPENSE),
        Map.entry("Éducation",      CategoryType.EXPENSE),
        Map.entry("Voyages",        CategoryType.EXPENSE),
        Map.entry("Restaurants",    CategoryType.EXPENSE),
        Map.entry("Salaire",        CategoryType.INCOME),
        Map.entry("Investissement", CategoryType.BOTH),
        Map.entry("Autres",         CategoryType.BOTH)
    );

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        mergeEnglishDuplicates();
        seedDefaultCategories();
        migrateDefaultCategoryTypes();
    }

    /**
     * For each English default category that still exists:
     * - If its French equivalent exists: migrate all transactions + budgets to the French one, then delete the English one.
     * - If its French equivalent does NOT exist: simply rename it.
     * This consolidates duplicates (e.g. "Food" + "Alimentation" → keeps only "Alimentation").
     */
    private void mergeEnglishDuplicates() {
        List<Category> allDefaults = categoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsDefault()))
                .toList();

        for (Map.Entry<String, String> entry : RENAME_MAP.entrySet()) {
            String englishName = entry.getKey();
            String frenchName  = entry.getValue();

            Category englishCat = allDefaults.stream()
                    .filter(c -> c.getName().equals(englishName))
                    .findFirst().orElse(null);

            if (englishCat == null) continue; // already gone or never existed

            Category frenchCat = allDefaults.stream()
                    .filter(c -> c.getName().equals(frenchName))
                    .findFirst().orElse(null);

            if (frenchCat != null) {
                // Both exist — migrate references then delete the English duplicate
                em.createQuery("UPDATE Transaction t SET t.category = :to WHERE t.category = :from")
                        .setParameter("to", frenchCat)
                        .setParameter("from", englishCat)
                        .executeUpdate();
                em.createQuery("UPDATE Budget b SET b.category = :to WHERE b.category = :from")
                        .setParameter("to", frenchCat)
                        .setParameter("from", englishCat)
                        .executeUpdate();
                em.flush();
                categoryRepository.delete(englishCat);
            } else {
                // French doesn't exist yet — just rename in place
                englishCat.setName(frenchName);
                categoryRepository.save(englishCat);
            }
        }
    }

    private void seedDefaultCategories() {
        List<Object[]> defaults = List.of(
            new Object[]{"Alimentation",    "bi-basket",          "#e74c3c", CategoryType.EXPENSE},
            new Object[]{"Transport",       "bi-car-front",       "#3498db", CategoryType.EXPENSE},
            new Object[]{"Logement",        "bi-house",           "#2ecc71", CategoryType.EXPENSE},
            new Object[]{"Santé",           "bi-heart-pulse",     "#e91e63", CategoryType.EXPENSE},
            new Object[]{"Loisirs",         "bi-controller",      "#9b59b6", CategoryType.EXPENSE},
            new Object[]{"Shopping",        "bi-bag",             "#f39c12", CategoryType.EXPENSE},
            new Object[]{"Éducation",       "bi-book",            "#1abc9c", CategoryType.EXPENSE},
            new Object[]{"Voyages",         "bi-airplane",        "#00bcd4", CategoryType.EXPENSE},
            new Object[]{"Restaurants",     "bi-cup-hot",         "#ff5722", CategoryType.EXPENSE},
            new Object[]{"Salaire",         "bi-briefcase",       "#27ae60", CategoryType.INCOME},
            new Object[]{"Investissement",  "bi-graph-up-arrow",  "#2980b9", CategoryType.BOTH},
            new Object[]{"Autres",          "bi-three-dots",      "#95a5a6", CategoryType.BOTH}
        );

        for (Object[] cat : defaults) {
            String name = (String) cat[0];
            if (!categoryRepository.existsByNameAndIsDefault(name, true)) {
                categoryRepository.save(Category.builder()
                        .name(name)
                        .icon((String) cat[1])
                        .color((String) cat[2])
                        .type((CategoryType) cat[3])
                        .isDefault(true)
                        .build());
            }
        }
    }

    /**
     * Sets the correct type on existing default categories whose type is still null
     * (created before the type column was introduced).
     */
    private void migrateDefaultCategoryTypes() {
        List<Category> allDefaults = categoryRepository.findAll().stream()
                .filter(c -> Boolean.TRUE.equals(c.getIsDefault()))
                .toList();

        for (Category cat : allDefaults) {
            if (cat.getType() == null) {
                CategoryType type = TYPE_MAP.getOrDefault(cat.getName(), CategoryType.BOTH);
                cat.setType(type);
                categoryRepository.save(cat);
            }
        }
    }
}
