package dev.sunbirdrc.claim.service;

import com.google.cloud.storage.Blob;
import com.google.cloud.storage.Storage;
import dev.sunbirdrc.claim.dto.FileDto;
import dev.sunbirdrc.claim.exception.GCPFileUploadException;
import dev.sunbirdrc.claim.utils.GCPBucketUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;


@Service
@RequiredArgsConstructor
public class FileServiceImpl implements FileService {

    @Value("dev-public-upsmf")
    private String bucketName;
    @Value("${gcp.dir.name}")
    private String gcpDirectoryName;
    @Autowired
    Storage storage;
    private final GCPBucketUtil dataBucketUtil;

    @Override
    public ByteArrayResource downloadFile(String fileName) {
        ByteArrayResource resource = null;
        Blob blob = dataBucketUtil.DownloadFile(fileName);
        if(blob!=null){
            resource = new ByteArrayResource(blob.getContent());
        }
        return resource;
    }


    @Override
    public FileDto uploadFile(MultipartFile file) throws IOException {
        String originalFileName = file.getOriginalFilename();
        FileDto fileDto = null;
        Path path = new File(originalFileName).toPath();
        try {
            String contentType = Files.probeContentType(path);
            fileDto = dataBucketUtil.uploadFile(file, originalFileName, contentType);
        } catch (Exception e) {
            throw new GCPFileUploadException("Error occurred while uploading");
        }
        return fileDto;
    }
}