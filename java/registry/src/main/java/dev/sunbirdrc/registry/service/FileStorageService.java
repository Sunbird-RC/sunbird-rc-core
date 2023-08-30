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
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

import static dev.sunbirdrc.registry.middleware.util.Constants.CONNECTION_FAILURE;
import static dev.sunbirdrc.registry.middleware.util.Constants.SUNBIRD_FILE_STORAGE_SERVICE_NAME;

@Service
public class FileStorageService implements HealthIndicator {
	private static final Logger logger = LoggerFactory.getLogger(FileStorageService.class);
	private final MinioClient minioClient;
	private final String bucketName;
	private static final String CONTENT_TYPE_TEXT = "text/plain";

	public FileStorageService(MinioClient minioClient, @Value("${filestorage.bucketname}") String bucketName) {
		this.bucketName = bucketName;
		this.minioClient = minioClient;
	}

	public void save(InputStream inputStream, String objectName) throws Exception {
		logger.info("Saving the file in the location {}", objectName);
		minioClient.putObject(PutObjectArgs.builder().bucket(bucketName).object(objectName).stream(inputStream, -1, 10485760).build());
		logger.info("File has successfully saved");
	}

	public DocumentsResponse saveAndFetchFileNames(MultipartFile[] files, String requestedURI) {
		String objectPath = getDirectoryPath(requestedURI);

		DocumentsResponse documentsResponse = new DocumentsResponse();
		for (MultipartFile file : files) {
			String fileName = getFileName(file.getOriginalFilename());
			try {
				String objectName = "/"+objectPath + "/" + fileName;
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
		Iterable<Result<DeleteError>> results = minioClient.removeObjects(RemoveObjectsArgs.builder().bucket(bucketName).objects(deleteObjects).build());
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

	public String getSignedUrl(String objectName) throws ServerException, InsufficientDataException, ErrorResponseException, IOException, NoSuchAlgorithmException, InvalidKeyException, InvalidResponseException, XmlParserException, InternalException {
		return minioClient.getPresignedObjectUrl(GetPresignedObjectUrlArgs.builder().method(Method.GET).bucket(bucketName).object(objectName).expiry(2, TimeUnit.HOURS).build());
	}

	public byte[] getDocument(String requestedURI) {
		String objectName = getDirectoryPath(requestedURI);
		byte[] bytes = new byte[0];
		try {
			InputStream inputStream = minioClient.getObject(GetObjectArgs.builder().bucket(bucketName).object(objectName).build());
			bytes = IOUtils.toByteArray(inputStream);
		} catch (Exception e) {
			logger.error("Error has occurred while fetching the document {} {}", objectName, e.getMessage());
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
