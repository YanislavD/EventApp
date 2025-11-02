package main.exception;

import main.web.dto.RegisterRequest;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.servlet.ModelAndView;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ModelAndView handleUserAlreadyExistsException(UserAlreadyExistsException ex) {
        ModelAndView modelAndView = new ModelAndView("register");
        modelAndView.addObject("errorMessage", ex.getMessage());
        modelAndView.addObject("registerRequest", new RegisterRequest());
        return modelAndView;
    }

    @ExceptionHandler(IllegalArgumentException.class)
    public ModelAndView handleIllegalArgumentException(IllegalArgumentException ex) {
        ModelAndView modelAndView = new ModelAndView("redirect:/home?error=invalidArgument");
        return modelAndView;
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ModelAndView handleMethodArgumentNotValidException(MethodArgumentNotValidException ex) {
        ModelAndView modelAndView = new ModelAndView("redirect:/home?error=validationFailed");
        return modelAndView;
    }
}
