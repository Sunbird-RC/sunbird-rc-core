package io.opensaber.registry.controller;

import io.opensaber.registry.model.dto.DocumentsResponse;
import io.opensaber.registry.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;

@Controller
public class FileStorageController {
    private final FileStorageService fileStorageService;

    FileStorageController(FileStorageService fileStorageService) {
        this.fileStorageService = fileStorageService;
    }

    @PostMapping("/api/v1/{entity}/{entityId}/{property}/documents")
    public ResponseEntity<DocumentsResponse> save(@RequestParam MultipartFile[] multipartFiles,
                                                  @PathVariable String entity,
                                                  @PathVariable String entityId,
                                                  @PathVariable String property,
                                                  HttpServletRequest httpServletRequest) {

        // TODO: authorize user
        DocumentsResponse fileNames = fileStorageService.saveAndFetchFileNames(multipartFiles, httpServletRequest.getRequestURI());
        return new ResponseEntity<>(fileNames, HttpStatus.OK);
    }

}
