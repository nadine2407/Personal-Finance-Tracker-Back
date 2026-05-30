package com.example.financetracker.domain.category;

import com.example.financetracker.domain.auth.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface CategoryRepository extends JpaRepository<Category, Long> {

    @Query("SELECT c FROM Category c WHERE c.user = :user OR c.isDefault = true ORDER BY c.name")
    List<Category> findByUserOrDefault(@Param("user") User user);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND (c.user = :user OR c.isDefault = true)")
    Optional<Category> findByIdAndUserOrDefault(@Param("id") Long id, @Param("user") User user);

    @Query("SELECT c FROM Category c WHERE c.id = :id AND c.user = :user")
    Optional<Category> findByIdAndUser(@Param("id") Long id, @Param("user") User user);

    List<Category> findByUser(User user);
    boolean existsByNameAndIsDefault(String name, boolean isDefault);
}
