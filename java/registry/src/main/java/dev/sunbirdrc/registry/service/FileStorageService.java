package dev.sunbirdrc.registry.service;

import dev.sunbirdrc.pojos.ComponentHealthInfo;
import dev.sunbirdrc.pojos.HealthIndicator;
import dev.sunbirdrc.registry.model.dto.DocumentsResponse;
import io.minio.*;
import io.minio.errors.*;
import io.minio.http.Method;
import io.minio.messages.Bucket;
import io.minio.messages.DeleteError;
import io.minio.messages.DeleteObject;
import org.apache.commons.lang3.exception.ExceptionUtils;
import org.apache.poi.util.IOUtils;
import org.jetbrains.annotations.NotNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_FILE_STORAGE_SERVICE_NAME;

@Service
@ConditionalOnProperty(name = "filestorage.enabled", havingValue = "true", matchIfMissing = true)
public class FileStorageService implements HealthIndicator {
    private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
    private static final String CONTENT_TYPE_TEXT = "text/plain";
    private final MinioClient minioClient;
    private final String bucketName;

    public FileStorageService(MinioClient minioClient, @Value("${filestorage.bucketname}") String bucketName) {
        this.bucketName = bucketName;
        this.minioClient = minioClient;
    }

    public void save(InputStream inputStream, String objectName) throws Exception {
        logger.info("Saving the file in the location {}", objectName);
        minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(inputStream, -1, 10485760).build());
        logger.info("File has successfully saved");
    }

    public DocumentsResponse saveAndFetchFileNames(MultipartFile[] files, String objectPath) {

        DocumentsResponse documentsResponse = new DocumentsResponse();
        for (MultipartFile file : files) {
            String objectName = objectPath + "/" + getFileName(Objects.requireNonNull(file.getOriginalFilename()));
            try {
                save(file.getInputStream(), objectName);
                documentsResponse.addDocumentLocation(objectName);
            } catch (Exception e) {
                documentsResponse.addError(file.getOriginalFilename());
                logger.error("Error has occurred while trying to save the file {}: {}", file.getOriginalFilename(), ExceptionUtils.getStackTrace(e));
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
        return uuid + "-" + file.replaceAll(" ", "_");
    }

    public DocumentsResponse deleteFiles(List<String> files) {
        DocumentsResponse documentsResponse = new DocumentsResponse();
        List<DeleteObject> deleteObjects = files.stream().map(DeleteObject::new).collect(Collectors.toList());
        Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(bucketName).objects(deleteObjects).build());
        for (Result<DeleteError> result : results) {
            try {
                documentsResponse.addError(result.get().bucketName());
            } catch (Exception e) {
                logger.error("Error has occurred while fetching the delete error result {}", ExceptionUtils.getStackTrace(e));
            }
        }
        return documentsResponse;
    }

    public String getSignedUrl(String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
        return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET).bucket(bucketName).object(objectName).expiry(2, TimeUnit.HOURS).build());
    }

    public byte[] getDocument(String objectName) throws Exception {
        try {
            InputStream inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
            return IOUtils.toByteArray(inputStream);
        } catch (Exception e) {
            logger.error("Error has occurred while fetching the document {} {}", objectName, ExceptionUtils.getStackTrace(e));
            throw e;
        }
    }

    public void deleteDocument(String objectName) throws Exception {
        try {
            minioClient.removeObject(RemoveObjectArgs.builder().bucket(bucketName).object(objectName).build());
        } catch (Exception e) {
            logger.error("Error has occurred while deleting the document {}", objectName);
            throw e;
        }
    }

    @Override
    public String getServiceName() {
        return SUNBIRD_FILE_STORAGE_SERVICE_NAME;
    }


    @Override
    public ComponentHealthInfo getHealthInfo() {
        try {
            List<Bucket> buckets = minioClient.listBuckets();
            return new ComponentHealthInfo(getServiceName(), true);
        } catch (Exception e) {
            return new ComponentHealthInfo(getServiceName(), false, CONNECTION_FAILURE, e.getMessage());
        }
    }
}
