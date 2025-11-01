package main.controller;

import jakarta.validation.Valid;
import main.service.CategoryService;
import main.web.dto.CategoryCreateRequest;
import org.springframework.stereotype.Controller;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

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
                                       BindingResult bindingResult) {

        if (!bindingResult.hasErrors()) {
            String name = categoryCreateRequest.getName();
            if (name == null) {
                bindingResult.rejectValue("name", "category.name", "Въведи име на категорията");
            } else {
                try {
                    categoryService.create(name);
                    return new ModelAndView("redirect:/admin/categories?created");
                } catch (IllegalArgumentException ex) {
                    String message = ex.getMessage() != null ? ex.getMessage() : "Невалидна категория";
                    bindingResult.rejectValue("name", "category.name", message);
                }
            }
        }

        ModelAndView modelAndView = new ModelAndView("admin-categories");
        modelAndView.addObject("categoryCreateRequest", categoryCreateRequest);
        return buildModelAndView(modelAndView);
    }

    private ModelAndView buildModelAndView(ModelAndView modelAndView) {
        modelAndView.addObject("categories", categoryService.getAll());
        return modelAndView;
    }
}

