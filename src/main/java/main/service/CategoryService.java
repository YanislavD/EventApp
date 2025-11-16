package main.service;

import main.exception.CategoryAlreadyExistsException;
import main.model.Category;
import main.repository.CategoryRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@Service
public class CategoryService {

    private static final Logger logger = LoggerFactory.getLogger(CategoryService.class);

    private final CategoryRepository categoryRepository;

    public CategoryService(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Cacheable(value = "categories")
    public List<Category> getAll() {
        return categoryRepository.findAll(Sort.by(Sort.Direction.ASC, "name"));
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public Category create(String name) {
        String trimmed = name == null ? "" : name.trim();
        if (trimmed.isEmpty()) {
            throw new IllegalArgumentException("Името на категорията е задължително");
        }

        categoryRepository.findByNameIgnoreCase(trimmed)
                .ifPresent(existing -> {
                    throw new CategoryAlreadyExistsException("Категория с това име вече съществува");
                });

        Category category = Category.builder()
                .name(trimmed)
                .build();

        Category saved = categoryRepository.save(category);
        logger.info("Category created successfully: {}", saved.getName());
        return saved;
    }

    public Category getById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Липсва идентификатор на категория");
        }
        return categoryRepository.findById(id)
                .orElseThrow(() -> new IllegalArgumentException("Категорията не е намерена"));
    }

    public Long getCount() {
        return categoryRepository.count();
    }

    @Transactional
    @CacheEvict(value = "categories", allEntries = true)
    public void deleteById(UUID id) {
        if (id == null) {
            throw new IllegalArgumentException("Липсва идентификатор на категория");
        }
        if (!categoryRepository.existsById(id)) {
            throw new IllegalArgumentException("Категорията не е намерена");
        }
        categoryRepository.deleteById(id);
        logger.info("Category deleted successfully: {}", id);
    }
}

