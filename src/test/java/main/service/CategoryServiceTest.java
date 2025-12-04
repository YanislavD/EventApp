package main.service;

import main.exception.CategoryAlreadyExistsException;
import main.model.Category;
import main.repository.CategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Sort;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class CategoryServiceTest {

    @Mock
    private CategoryRepository categoryRepository;

    @InjectMocks
    private CategoryService categoryService;

    private Category testCategory;
    private UUID testCategoryId;

    @BeforeEach
    void setUp() {
        testCategoryId = UUID.randomUUID();
        testCategory = Category.builder()
                .id(testCategoryId)
                .name("Test Category")
                .isActive(true)
                .build();
    }

    @Test
    void whenGetAll_thenAllCategoriesAreReturned() {
        List<Category> categories = new ArrayList<>();
        categories.add(testCategory);

        when(categoryRepository.findAll(any(Sort.class))).thenReturn(categories);

        List<Category> result = categoryService.getAll();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertEquals("Test Category", result.get(0).getName());
        verify(categoryRepository).findAll(any(Sort.class));
    }

    @Test
    void whenGetAllActive_thenOnlyActiveCategoriesAreReturned() {
        List<Category> activeCategories = new ArrayList<>();
        activeCategories.add(testCategory);

        when(categoryRepository.findByIsActiveTrue(any(Sort.class))).thenReturn(activeCategories);

        List<Category> result = categoryService.getAllActive();

        assertNotNull(result);
        assertEquals(1, result.size());
        assertTrue(result.get(0).getIsActive());
        verify(categoryRepository).findByIsActiveTrue(any(Sort.class));
    }

    @Test
    void whenGetActiveByIdWithValidId_thenCategoryIsReturned() {
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));

        Category result = categoryService.getActiveById(testCategoryId);

        assertNotNull(result);
        assertEquals(testCategoryId, result.getId());
        assertTrue(result.getIsActive());
        verify(categoryRepository).findById(testCategoryId);
    }

    @Test
    void whenGetActiveByIdWithNullId_thenExceptionIsThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.getActiveById(null));

        assertEquals("Идентификаторът на категорията е задължителен", exception.getMessage());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void whenGetActiveByIdWithNonExistentId_thenExceptionIsThrown() {
        UUID nonExistentId = UUID.randomUUID();
        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.getActiveById(nonExistentId));

        assertEquals("Категорията не е намерена", exception.getMessage());
        verify(categoryRepository).findById(nonExistentId);
    }

    @Test
    void whenGetActiveByIdWithInactiveCategory_thenExceptionIsThrown() {
        Category inactiveCategory = Category.builder()
                .id(testCategoryId)
                .name("Inactive Category")
                .isActive(false)
                .build();

        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(inactiveCategory));

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.getActiveById(testCategoryId));

        assertEquals("Не можеш да използваш неактивна категория", exception.getMessage());
        verify(categoryRepository).findById(testCategoryId);
    }

    @Test
    void whenCreateWithValidName_thenCategoryIsCreated() {
        String categoryName = "New Category";
        when(categoryRepository.findByNameIgnoreCase(categoryName)).thenReturn(Optional.empty());
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        Category result = categoryService.create(categoryName);

        assertNotNull(result);
        verify(categoryRepository).findByNameIgnoreCase(categoryName);
        verify(categoryRepository).save(any(Category.class));
    }

    @Test
    void whenCreateWithNullName_thenExceptionIsThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.create(null));

        assertEquals("Името на категорията е задължително", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void whenCreateWithEmptyName_thenExceptionIsThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.create("   "));

        assertEquals("Името на категорията е задължително", exception.getMessage());
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void whenCreateWithExistingName_thenExceptionIsThrown() {
        String existingName = "Existing Category";
        when(categoryRepository.findByNameIgnoreCase(existingName)).thenReturn(Optional.of(testCategory));

        CategoryAlreadyExistsException exception = assertThrows(CategoryAlreadyExistsException.class,
                () -> categoryService.create(existingName));

        assertEquals("Категория с това име вече съществува", exception.getMessage());
        verify(categoryRepository).findByNameIgnoreCase(existingName);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void whenGetCount_thenCountIsReturned() {
        when(categoryRepository.count()).thenReturn(5L);

        Long result = categoryService.getCount();

        assertEquals(5L, result);
        verify(categoryRepository).count();
    }

    @Test
    void whenDeleteByIdWithValidId_thenCategoryIsDeactivated() {
        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(testCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(testCategory);

        categoryService.deleteById(testCategoryId);

        assertFalse(testCategory.getIsActive());
        verify(categoryRepository).findById(testCategoryId);
        verify(categoryRepository).save(testCategory);
    }

    @Test
    void whenDeleteByIdWithNullId_thenExceptionIsThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.deleteById(null));

        assertEquals("Липсва идентификатор на категория", exception.getMessage());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void whenDeleteByIdWithNonExistentId_thenExceptionIsThrown() {
        UUID nonExistentId = UUID.randomUUID();
        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.deleteById(nonExistentId));

        assertEquals("Категорията не е намерена", exception.getMessage());
        verify(categoryRepository).findById(nonExistentId);
        verify(categoryRepository, never()).save(any());
    }

    @Test
    void whenActivateByIdWithValidId_thenCategoryIsActivated() {
        Category inactiveCategory = Category.builder()
                .id(testCategoryId)
                .name("Inactive Category")
                .isActive(false)
                .build();

        when(categoryRepository.findById(testCategoryId)).thenReturn(Optional.of(inactiveCategory));
        when(categoryRepository.save(any(Category.class))).thenReturn(inactiveCategory);

        categoryService.activateById(testCategoryId);

        assertTrue(inactiveCategory.getIsActive());
        verify(categoryRepository).findById(testCategoryId);
        verify(categoryRepository).save(inactiveCategory);
    }

    @Test
    void whenActivateByIdWithNullId_thenExceptionIsThrown() {
        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.activateById(null));

        assertEquals("Липсва идентификатор на категория", exception.getMessage());
        verify(categoryRepository, never()).findById(any());
    }

    @Test
    void whenActivateByIdWithNonExistentId_thenExceptionIsThrown() {
        UUID nonExistentId = UUID.randomUUID();
        when(categoryRepository.findById(nonExistentId)).thenReturn(Optional.empty());

        IllegalArgumentException exception = assertThrows(IllegalArgumentException.class,
                () -> categoryService.activateById(nonExistentId));

        assertEquals("Категорията не е намерена", exception.getMessage());
        verify(categoryRepository).findById(nonExistentId);
        verify(categoryRepository, never()).save(any());
    }
}

