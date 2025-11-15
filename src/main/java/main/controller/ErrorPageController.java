package main.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.servlet.ModelAndView;

@Controller
@RequestMapping("/error")
public class ErrorPageController {

    @GetMapping("/oops")
    public ModelAndView forbiddenPage() {
        ModelAndView modelAndView = new ModelAndView("error/oops");
        modelAndView.addObject("title", "Нещо се обърка");
        modelAndView.addObject("message", "Изглежда се опитваш да направиш нещо, което не е позволено.");
        return modelAndView;
    }
}

