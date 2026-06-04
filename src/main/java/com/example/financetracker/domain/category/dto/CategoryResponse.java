package com.example.financetracker.domain.category.dto;

import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryType;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CategoryResponse {

    private Long id;
    private String name;
    private String icon;
    private String color;
    private Boolean isDefault;
    private CategoryType type;
    private OffsetDateTime createdAt;

    public static CategoryResponse from(Category category) {
        CategoryResponse dto = new CategoryResponse();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setColor(category.getColor());
        dto.setIsDefault(Boolean.TRUE.equals(category.getIsDefault()));
        dto.setType(category.getType() != null ? category.getType() : CategoryType.BOTH);
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }
}
