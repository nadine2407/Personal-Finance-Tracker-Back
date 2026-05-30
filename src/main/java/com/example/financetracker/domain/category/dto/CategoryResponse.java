package com.example.financetracker.domain.category.dto;

import com.example.financetracker.domain.category.Category;
import lombok.Data;

import java.time.OffsetDateTime;

@Data
public class CategoryResponse {

    private Long id;
    private String name;
    private String icon;
    private String color;
    private Boolean isDefault;
    private OffsetDateTime createdAt;

    public static CategoryResponse from(Category category) {
        CategoryResponse dto = new CategoryResponse();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setIcon(category.getIcon());
        dto.setColor(category.getColor());
        dto.setIsDefault(Boolean.TRUE.equals(category.getIsDefault()));
        dto.setCreatedAt(category.getCreatedAt());
        return dto;
    }
}
