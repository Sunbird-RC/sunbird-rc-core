package dev.sunbirdrc.registry.controller;

import dev.sunbirdrc.registry.helper.RegistryHelper;
import dev.sunbirdrc.registry.model.dto.DocumentsResponse;
import dev.sunbirdrc.registry.service.FileStorageService;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServletRequest;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.List;

// TODO: Get should be viewed by both attestor and reviewer
@Controller
@ConditionalOnProperty(name = "filestorage.enabled", havingValue = "true", matchIfMissing = true)
public class FileStorageController {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageController.class);
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
            logger.error("Authorizing entity failed: {}", ExceptionUtils.getStackTrace(e));
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        String objectPath = getDirectoryPath(httpServletRequest.getRequestURI());
        DocumentsResponse documentsResponse = fileStorageService.saveAndFetchFileNames(files, objectPath);
        return new ResponseEntity<>(documentsResponse, HttpStatus.OK);
    }

    @PutMapping("/api/v1/{entity}/{entityId}/{property}/documents/{documentId}")
    public ResponseEntity<DocumentsResponse> update(@RequestParam MultipartFile file,
                                                  @PathVariable String entity,
                                                  @PathVariable String entityId,
                                                  HttpServletRequest httpServletRequest) {
        try {
            registryHelper.authorize(entity, entityId, httpServletRequest);
        } catch (Exception e) {
            logger.error("Authorizing entity failed: {}", ExceptionUtils.getStackTrace(e));
            return new ResponseEntity<>(HttpStatus.FORBIDDEN);
        }
        String objectPath = getDirectoryPath(httpServletRequest.getRequestURI());
        try {
            fileStorageService.deleteDocument(objectPath);
        } catch (Exception e) {
            DocumentsResponse documentsResponse = new DocumentsResponse();
            documentsResponse.setErrors(Collections.singletonList(e.getMessage()));
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(documentsResponse);
        }
        objectPath = objectPath.substring(0, objectPath.lastIndexOf("/"));
        DocumentsResponse documentsResponse = fileStorageService.saveAndFetchFileNames(new MultipartFile[] {file}, objectPath);
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
            logger.error("Authorizing entity failed: {}", ExceptionUtils.getStackTrace(e));
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
            logger.error("Authorizing entity failed: {}", ExceptionUtils.getStackTrace(e));
            return ResponseEntity.status(HttpStatus.FORBIDDEN).build();
        }
        String objectName = getDirectoryPath(httpServletRequest.getRequestURI());
        try {
            fileStorageService.deleteDocument(objectName);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed deleting the document");
        }
        return ResponseEntity.ok(HttpStatus.OK);
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
        String objectName = getDirectoryPath(httpServletRequest.getRequestURI());
        byte[] document;
        try {
            document = fileStorageService.getDocument(objectName);
        } catch (Exception e) {
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Failed fetching the document".getBytes(StandardCharsets.UTF_8));
        }
        return ResponseEntity.ok().body(document);
    }

    private String getDirectoryPath(String requestedURI) {
        String versionDelimiter = "/v1/";
        String[] split = requestedURI.split(versionDelimiter);
        return split[1];
    }
}
