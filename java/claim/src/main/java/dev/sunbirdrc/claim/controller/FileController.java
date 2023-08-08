package dev.sunbirdrc.claim.controller;

import dev.sunbirdrc.claim.dto.FileDto;
import dev.sunbirdrc.claim.service.FileService;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@RestController
@RequestMapping("/api/v1/files/")
@RequiredArgsConstructor
public class FileController {

    @Autowired
    FileService fileService;


    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RequestMapping("upload")
    public ResponseEntity<String> uploadFile(
            @RequestParam MultipartFile file) throws IOException {
        String fileUrl = null;
        FileDto fileDto = fileService.uploadFile(file);
        if (fileDto != null) {
            fileUrl = fileDto.getFileUrl();
            return ResponseEntity.ok(fileUrl);
        }
        return (ResponseEntity<String>) ResponseEntity.status(HttpStatus.EXPECTATION_FAILED);
    }

    @RequestMapping("download")
    @PostMapping(produces = {MediaType.APPLICATION_PDF_VALUE})
    public ResponseEntity<Resource> downloadFile(
            @RequestParam(value = "fileName", required = false) String fileName) {
        ByteArrayResource resource = fileService.downloadFile(fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "filename=\"" + fileName + "\"");
        return ResponseEntity.ok().
                contentType(MediaType.APPLICATION_PDF).
                headers(headers).body(resource);
    }
}

