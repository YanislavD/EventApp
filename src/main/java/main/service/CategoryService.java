package main.service;

import main.model.Category;
import main.repository.CategoryRepository;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    public List<Category> getAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional
    @SuppressWarnings("null")
    public Category create(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Името на категорията е задължително");
        }

        categoryRepository.findByNameIgnoreCase(trimmed)
                .ifPresent(existing -> {
                    throw new IllegalArgumentException("Категория с това име вече съществува");
                });

        Category category = Category.builder()
                .name(trimmed)
                .build();

        return categoryRepository.save(category);
    }

    public Category getById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Липсва идентификатор на категория");
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категорията не е намерена"));
    }
}

