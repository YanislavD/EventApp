package main.exception;

import main.web.dto.RegisterRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ModelAndView handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        ModelAndView modelAndView = new ModelAndView("register"); // връща register.html
        modelAndView.addObject("errorMessage", ex.getMessage());
        modelAndView.addObject("registerRequest", new RegisterRequest()); // празна форма (или можеш да върнеш старата)
        return modelAndView;
    }
}
