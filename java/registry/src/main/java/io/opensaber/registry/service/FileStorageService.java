package io.opensaber.registry.service;

import io.minio.BucketExistsArgs;
import io.minio.MakeBucketArgs;
import io.minio.MinioClient;
import io.minio.PutObjectArgs;
import io.minio.errors.*;
import io.opensaber.registry.model.dto.DocumentsResponse;
import org.jetbrains.annotations.NotNull;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.UUID;

@Service
public class FileStorageService {

    private final MinioClient minioClient;
    private final String bucketName;
    private static final String CONTENT_TYPE_TEXT = "text/plain";

    public FileStorageService(MinioClient minioClient, @Value("${filestorage.bucketname}") String bucketName) {
        this.bucketName = bucketName;
        this.minioClient = minioClient;
    }

    public void save(InputStream inputStream, String objectName) throws Exception {
        if (!isBucketExists()) {
            createNewBucket();
            save(inputStream, objectName);
        }
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(inputStream, -1, 10485760)
                .contentType(CONTENT_TYPE_TEXT)
                .build());
    }

    private void createNewBucket() throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        minioClient.makeBucket(MakeBucketArgs
                .builder()
                .bucket(bucketName)
                .build());
    }

    private boolean isBucketExists() throws ErrorResponseException, InsufficientDataException, InternalException, InvalidKeyException, InvalidResponseException, IOException, NoSuchAlgorithmException, ServerException, XmlParserException {
        return minioClient.bucketExists(BucketExistsArgs
                .builder()
                .bucket(bucketName)
                .build());
    }

    public DocumentsResponse saveAndFetchFileNames(MultipartFile[] files, String requestedURI) {
        String versionDelimiter = "/v1/";
        String[] split = requestedURI.split(versionDelimiter);
        String objectPath = split[1];

        DocumentsResponse documentsResponse = new DocumentsResponse();
        for (MultipartFile file : files) {
            try {
                String objectName = getFileName(objectPath, file.getOriginalFilename());
                save(file.getInputStream(), objectName);
                documentsResponse.addDocumentLocation(objectName);
            } catch (Exception e) {
                documentsResponse.addFileName(file.getOriginalFilename());
                e.printStackTrace();
            }
        }
        return documentsResponse;
    }

    @NotNull
    private String getFileName(String objectPath, String file) {
        String uuid = UUID.randomUUID().toString();
        return objectPath + "/" + uuid + "-" + file;
    }
}

