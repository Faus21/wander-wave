package com.dama.wanderwave.azure;

import com.azure.storage.blob.BlobServiceClient;
import com.azure.storage.blob.BlobServiceClientBuilder;
import com.azure.storage.blob.BlobContainerClient;
import com.azure.storage.blob.BlobClient;
import com.azure.core.util.BinaryData;
import com.dama.wanderwave.handler.azure.FileTypeException;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.util.Arrays;
import java.util.List;

@Getter
@Service
@Slf4j
public class AzureService {

    private final BlobServiceClient blobServiceClient;

    private static final List<String> ALLOWED_CONTENT_TYPES = Arrays.asList(
            "image/jpeg",
            "image/png",
            "image/gif",
            "image/webp"
    );

    public AzureService(@Value("${azure.storage.connection-string}") String azureConnectionString) {
        this.blobServiceClient = new BlobServiceClientBuilder()
                .connectionString(azureConnectionString)
                .buildClient();
    }

    public String uploadAvatar(String containerName, String blobName, byte[] fileContent, String contentType, long fileSize) {
        log.info("Starting upload for file: {}, size: {}, contentType: {}", blobName, fileSize, contentType);

        if (!ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new FileTypeException("Invalid file type. Only JPEG, PNG, GIF, and WebP are allowed.");
        }

        BlobContainerClient blobContainerClient = blobServiceClient.getBlobContainerClient(containerName);
        log.debug("Retrieved container client for container: {}", containerName);

        if (!blobContainerClient.exists()) {
            log.info("Container '{}' does not exist. Creating a new container.", containerName);
            blobContainerClient.create();
        }

        BlobClient blobClient = blobContainerClient.getBlobClient(blobName);
        log.debug("Uploading file '{}' to container '{}'.", blobName, containerName);
        blobClient.upload(BinaryData.fromBytes(fileContent), true);

        String blobUrl = blobClient.getBlobUrl();
        log.info("File '{}' uploaded successfully. Blob URL: {}", blobName, blobUrl);

        return blobUrl;
    }
}



