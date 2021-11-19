package io.opensaber.registry.controller;


import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.util.ResourceUtils;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.nio.file.Files;

@RestController
public class RegistryTemplateController{

    @Value("${certificate.templateFolderPath}")
    private String templatesFolderPath;

    @RequestMapping(value = "/api/v1/templates/{fileName}.html", method = RequestMethod.GET)
    public String getTemplate(@PathVariable String fileName) {
        String content = "";
        try {
            File file = new ClassPathResource(templatesFolderPath + fileName + ".html").getFile();
            content = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            e.printStackTrace();
        }
        return content;
    }

}
