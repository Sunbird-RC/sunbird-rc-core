package dev.sunbirdrc.plugin.controllers;

import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletResponse;

@RestController
public class PluginController {
    @RequestMapping(value = "/plugin/login", method = RequestMethod.GET)
    public void registryHealth(HttpServletResponse httpServletResponse) {
        httpServletResponse.setHeader("Location", "https://ndear.xiv.in/login");
        httpServletResponse.setStatus(302);
    }
}
