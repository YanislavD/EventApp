package main.controller;

import jakarta.validation.Valid;
import main.service.CategoryService;
import main.web.dto.CategoryCreateRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.web.servlet.ModelAndView;

import java.util.UUID;

@Controller
@RequestMapping("/admin/categories")
public class AdminCategoryController {

    private final CategoryService categoryService;

    public AdminCategoryController(CategoryService categoryService) {
        this.categoryService = categoryService;
    }

    @GetMapping
    public ModelAndView showCategories(@ModelAttribute("categoryCreateRequest") CategoryCreateRequest categoryCreateRequest) {
        return buildModelAndView(new ModelAndView("admin-categories"));
    }

    @PostMapping
    @SuppressWarnings("null")
    public ModelAndView createCategory(@Valid @ModelAttribute("categoryCreateRequest") CategoryCreateRequest categoryCreateRequest,
                                       BindingResult bindingResult,
                                       RedirectAttributes redirectAttributes) {

        if (!bindingResult.hasErrors()) {
            categoryService.create(categoryCreateRequest.getName());
            redirectAttributes.addFlashAttribute("successMessage", "✓ Успех! Категорията беше добавена успешно.");
            return new ModelAndView("redirect:/admin/categories");
        }

        ModelAndView modelAndView = new ModelAndView("admin-categories");
        modelAndView.addObject("categoryCreateRequest", categoryCreateRequest);
        return buildModelAndView(modelAndView);
    }

    @DeleteMapping("/{id}")
    public ModelAndView deleteCategory(@PathVariable UUID id, RedirectAttributes redirectAttributes) {
        categoryService.deleteById(id);
        redirectAttributes.addFlashAttribute("successMessage", "✓ Успех! Категорията беше изтрита успешно.");
        return new ModelAndView("redirect:/admin/categories");
    }

    private ModelAndView buildModelAndView(ModelAndView modelAndView) {
        modelAndView.addObject("categories", categoryService.getAll());
        return modelAndView;
    }
}

