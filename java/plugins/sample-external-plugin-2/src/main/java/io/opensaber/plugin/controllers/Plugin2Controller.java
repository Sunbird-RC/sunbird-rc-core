package io.opensaber.plugin.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletResponse;

@RestController
public class Plugin2Controller {
    @RequestMapping(value = "/plugin2/login", method = RequestMethod.GET)
    public void registryHealth(HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader("Location", "https://google.com");
        httpServletResponse.setStatus(302);
    }
}
