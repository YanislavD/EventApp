package main.exception;

import main.web.dto.RegisterRequest;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ModelAndView handleUserAlreadyExistsException(UserAlreadyExistsException ex, RedirectAttributes redirectAttributes) {

        redirectAttributes.addFlashAttribute("message", ex.getMessage());

        return new ModelAndView("redirect:/register");
    }

    @ExceptionHandler(CategoryAlreadyExistsException.class)
    public ModelAndView handleCategoryAlreadyExistsException(CategoryAlreadyExistsException ex, RedirectAttributes redirectAttributes) {
        redirectAttributes.addFlashAttribute("message", ex.getMessage());
        return new ModelAndView("redirect:/admin/categories");
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException ex) {
        ModelAndView modelAndView = new ModelAndView("error/404");
        modelAndView.setStatus(HttpStatus.NOT_FOUND);
        modelAndView.addObject("message", ex.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(IllegalStateException.class)
    public ModelAndView handleIllegalStateException(IllegalStateException ex) {
        ModelAndView modelAndView = new ModelAndView("error/oops");
        modelAndView.setStatus(HttpStatus.FORBIDDEN);
        modelAndView.addObject("message", ex.getMessage());
        return modelAndView;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ModelAndView modelAndView = new ModelAndView("redirect:/home?error=validationFailed");
        return modelAndView;
    }   
}
