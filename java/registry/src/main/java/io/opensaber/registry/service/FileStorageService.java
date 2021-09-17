package io.opensaber.registry.service;

import io.minio.*;
import io.minio.errors.*;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import io.opensaber.registry.model.dto.DocumentsResponse;
import org.apache.poi.util.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
public class FileStorageService {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private final MinioClient minioClient;
    private final String bucketName;
    private static final String CONTENT_TYPE_TEXT = "text/plain";

    public FileStorageService(MinioClient minioClient, @Value("${filestorage.bucketname}") String bucketName) {
        this.bucketName = bucketName;
        this.minioClient = minioClient;
    }

    public void save(InputStream inputStream, String objectName) throws Exception {
        if (!isBucketExists()) {
            logger.info("Bucket {} doesn't exist creating new bucket", bucketName);
            createNewBucket();
            save(inputStream, objectName);
        }
        logger.info("Saving the file in the location {}", objectName);
        minioClient.putObject(PutObjectArgs.builder()
                .bucket(bucketName)
                .object(objectName)
                .stream(inputStream, -1, 10485760)
                .build());
        logger.info("File has successfully saved");
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
        String objectPath = getDirectoryPath(requestedURI);

        DocumentsResponse documentsResponse = new DocumentsResponse();
        for (MultipartFile file : files) {
            String fileName = getFileName(file.getOriginalFilename());
            try {
                String objectName = objectPath + "/" + fileName;
                save(file.getInputStream(), objectName);
                documentsResponse.addDocumentLocation(objectName);
            } catch (Exception e) {
                documentsResponse.addError(file.getOriginalFilename());
                logger.error("Error has occurred while trying to save the file {}", fileName);
                e.printStackTrace();
            }
        }
        return documentsResponse;
    }

    private String getDirectoryPath(String requestedURI) {
        String versionDelimiter = "/v1/";
        String[] split = requestedURI.split(versionDelimiter);
        return split[1];
    }

    @NotNull
    private String getFileName(String file) {
        String uuid = UUID.randomUUID().toString();
        return uuid + "-" + file;
    }

    public DocumentsResponse deleteFiles(List<String> files) {
        DocumentsResponse documentsResponse = new DocumentsResponse();
        List<DeleteObject> deleteObjects = files.stream().map(DeleteObject::new).collect(Collectors.toList());
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder()
                        .bucket(bucketName)
                        .objects(deleteObjects)
                        .build());
        for (Result<DeleteError> result : results) {
            try {
                documentsResponse.addError(result.get().bucketName());
            } catch (Exception e) {
                logger.error("Error has occurred while fetching the delete error result {}", e.getMessage());
                e.printStackTrace();
            }
        }
        return documentsResponse;
    }

    public byte[] getDocument(String requestedURI) {
        String objectName = getDirectoryPath(requestedURI);
        byte[] bytes = new byte[0];
        try {
            InputStream inputStream = minioClient.getObject(
                    GetObjectArgs.builder()
                            .bucket(bucketName)
                            .object(objectName)
                            .build());
            bytes = IOUtils.toByteArray(inputStream);
        } catch (Exception e) {
            logger.error("Error has occurred while fetching the document {} {}", objectName,  e.getMessage());
            e.printStackTrace();
        }
        return bytes;
    }

    public ResponseEntity deleteDocument(String requestedURI) {
        String objectName = getDirectoryPath(requestedURI);
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            logger.error("Error has occurred while deleting the document {}", objectName);
            e.printStackTrace();
            return new ResponseEntity(HttpStatus.BAD_REQUEST);
        }
        return new ResponseEntity(HttpStatus.OK);
    }
}
