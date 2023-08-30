package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.dto.FileDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.MediaType;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.List;

public interface FileService {

    ByteArrayResource downloadFile(String fileName);

    FileDto uploadFile(MultipartFile file) throws IOException;

    List<FileDto> uploadMultipleFile(MultipartFile[] files, String entityName, String entityId);

    MediaType getFileMediaType(String fileName) throws Exception;
}