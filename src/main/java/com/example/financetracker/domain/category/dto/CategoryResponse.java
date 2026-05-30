package com.example.financetracker.domain.category.dto;

import com.example.financetracker.domain.category.Category;
import com.example.financetracker.domain.category.CategoryType;
import lombok.Data;

@Data
public class CategoryResponse {

    private Long id;
    private String name;
    private CategoryType type;

    public static CategoryResponse from(Category category) {
        CategoryResponse dto = new CategoryResponse();
        dto.setId(category.getId());
        dto.setName(category.getName());
        dto.setType(category.getType());
        return dto;
    }
}
