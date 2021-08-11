package io.opensaber.registry.controller;

import io.opensaber.registry.helper.RegistryHelper;
import io.opensaber.registry.model.dto.DocumentsResponse;
import io.opensaber.registry.service.FileStorageService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.util.List;

// TODO: Get should be viewed by both attestor and reviewer
@Controller
public class FileStorageController {
    private final FileStorageService fileStorageService;
    private final RegistryHelper registryHelper;

    FileStorageController(FileStorageService fileStorageService, RegistryHelper registryHelper) {
        this.fileStorageService = fileStorageService;
        this.registryHelper = registryHelper;
    }

    @PostMapping("/api/v1/{entity}/{entityId}/{property}/documents")
    public ResponseEntity<DocumentsResponse> save(@RequestParam MultipartFile[] files,
                                                  @PathVariable String entity,
                                                  @PathVariable String entityId,
                                                  @PathVariable String property,
                                                  HttpServletRequest httpServletRequest) {
        try {
            registryHelper.authorize(entity, entityId, httpServletRequest);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        DocumentsResponse documentsResponse = fileStorageService.saveAndFetchFileNames(files, httpServletRequest.getRequestURI());
        return new ResponseEntity<>(documentsResponse, HttpStatus.OK);
    }

    @DeleteMapping("/api/v1/{entity}/{entityId}/{property}/documents")
    public ResponseEntity<DocumentsResponse> deleteMultipleFiles(@PathVariable String entity,
                                                    @PathVariable String entityId,
                                                    @PathVariable String property,
                                                    @RequestBody List<String> files,
                                                    HttpServletRequest httpServletRequest) {
        try {
            registryHelper.authorize(entity, entityId, httpServletRequest);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        DocumentsResponse documentsResponse = fileStorageService.deleteFiles(files);
        return new ResponseEntity<>(documentsResponse, HttpStatus.OK);
    }

    @DeleteMapping(value = "/api/v1/{entity}/{entityId}/{property}/documents/{documentId}")
    public ResponseEntity deleteAFile(@PathVariable String entity,
                              @PathVariable String entityId,
                              @PathVariable String property,
                              @PathVariable String documentId,
                              HttpServletRequest httpServletRequest) {
        try {
            registryHelper.authorize(entity, entityId, httpServletRequest);
        } catch (Exception e) {
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.FORBIDDEN);
        }
        return fileStorageService.deleteDocument(httpServletRequest.getRequestURI());
    }

    @GetMapping(value = "/api/v1/{entity}/{entityId}/{property}/documents/{documentId}", produces = MediaType.APPLICATION_OCTET_STREAM_VALUE)
    public ResponseEntity<byte[]> get(@PathVariable String entity,
                                      @PathVariable String entityId,
                                      @PathVariable String property,
                                      @PathVariable String documentId,
                                      HttpServletRequest httpServletRequest) {
        try {
            registryHelper.authorize(entity, entityId, httpServletRequest);
        } catch (Exception e) {
            try {
                registryHelper.authorizeAttestor(entity, httpServletRequest);
            } catch (Exception exceptionFromAuthorizeAttestor) {
                return new ResponseEntity<>(HttpStatus.FORBIDDEN);
            }
        }
        byte[] document = fileStorageService.getDocument(httpServletRequest.getRequestURI());
        return ResponseEntity.ok().body(document);
    }
}
