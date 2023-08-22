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
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

@RestController
@RequestMapping("/api/v1/files")
@RequiredArgsConstructor
public class FileController {

    @Autowired
    FileService fileService;


    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RequestMapping("/upload")
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

    @RequestMapping("/download")
    @PostMapping(produces = {MediaType.APPLICATION_PDF_VALUE})
    public ResponseEntity<Resource> downloadFile(
            @RequestParam(value = "fileName", required = false) String fileName) {
        ByteArrayResource resource = fileService.downloadFile(fileName);
        HttpHeaders headers = new HttpHeaders();
        headers.add(HttpHeaders.CONTENT_DISPOSITION, "filename=\"" + fileName + "\"");

        try {
            MediaType mediaType = fileService.getFileMediaType(fileName);

            return ResponseEntity.ok()
                    .contentType(mediaType)
                    .headers(headers).body(resource);
        } catch (Exception e) {
            return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
        }
    }

    /**
     * @param entityName
     * @param entityId
     * @param files
     * @return
     */
    @PostMapping(consumes = {MediaType.MULTIPART_FORM_DATA_VALUE})
    @RequestMapping("/upload/multiple/{entityName}/{entityId}")
    public ResponseEntity<List<FileDto>> uploadMultipleFiles(
            @PathVariable String entityName,
            @PathVariable String entityId,
            @RequestParam MultipartFile[] files) {

        List<FileDto> fileDtoList = fileService.uploadMultipleFile(files, entityName, entityId);

        if (fileDtoList != null) {
            return ResponseEntity.ok(fileDtoList);
        }

        return new ResponseEntity<>(HttpStatus.EXPECTATION_FAILED);
    }
}

