package dev.sunbirdrc.registry.controller;


import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.web.bind.annotation.*;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;

@RestController
public class RegistryTemplateController{
    private static final Logger logger = LoggerFactory.getLogger(RegistryTemplateController.class);

    @Value("${template.folder-path}")
    private String templatesFolderPath;

    @RequestMapping(value = "/api/v1/templates/{fileName}", method = RequestMethod.GET)
    public String getTemplate(@PathVariable String fileName) {
        String content = "";
        try {
            File file = new ClassPathResource(templatesFolderPath + fileName).getFile();
            content = new String(Files.readAllBytes(file.toPath()));
        } catch (IOException e) {
            logger.info("Fetching template failed: {}", ExceptionUtils.getStackTrace(e));
        }
        return content;
    }
}
