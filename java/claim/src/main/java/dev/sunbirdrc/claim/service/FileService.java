package dev.sunbirdrc.claim.service;

import dev.sunbirdrc.claim.dto.FileDto;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

public interface FileService {

    ByteArrayResource downloadFile(String fileName);

    FileDto uploadFile(MultipartFile file) throws IOException;
}