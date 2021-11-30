package io.opensaber.registry.controller;


import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
public class RegistryTemplateController{
    @RequestMapping(value = "/api/v1/templates/{fileName}.html", method = RequestMethod.GET)
    public String getTemplate(@PathVariable String fileName) {
        String content = "";
        try {
            String templatesFolderPath = "public/_schemas/templates/";
            File file = new ClassPathResource(templatesFolderPath + fileName + ".html").getFile();
            content = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }
}
