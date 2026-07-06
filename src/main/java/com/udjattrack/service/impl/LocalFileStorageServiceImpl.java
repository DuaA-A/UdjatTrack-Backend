package com.udjattrack.service.impl;

import com.udjattrack.exception.BusinessException;
import com.udjattrack.service.FileStorageService;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Set;
import java.util.UUID;

@Service
@Slf4j
public class LocalFileStorageServiceImpl implements FileStorageService {

    private static final Set<String> ALLOWED_CONTENT_TYPES = Set.of(
            "image/jpeg", "image/png", "image/webp"
    );
    private static final long MAX_FILE_SIZE_BYTES = 5 * 1024 * 1024; // 5 MB

    @Value("${application.storage.upload-dir:uploads}")
    private String uploadDir;

    @Value("${application.storage.base-url:http://localhost:8080/api/v1}")
    private String baseUrl;

    @PostConstruct
    public void init() {
        try {
            Files.createDirectories(Paths.get(uploadDir));
            log.info("FileStorageService: Upload directory ready at '{}'", Paths.get(uploadDir).toAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException("Could not create upload directory: " + uploadDir, e);
        }
    }

    @Override
    public String storeFile(MultipartFile file, String subfolder) {
        validateFile(file);

        try {
            Path targetDir = Paths.get(uploadDir, subfolder);
            Files.createDirectories(targetDir);

            String originalFilename = file.getOriginalFilename();
            String extension = getExtension(originalFilename);
            String storedFilename = UUID.randomUUID() + "." + extension;

            Path targetPath = targetDir.resolve(storedFilename);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            String fileUrl = baseUrl + "/files/" + subfolder + "/" + storedFilename;
            log.info("FileStorageService: File stored at '{}', accessible via '{}'", targetPath, fileUrl);
            return fileUrl;

        } catch (IOException e) {
            throw new BusinessException("Could not store file. Please try again: " + e.getMessage());
        }
    }

    @Override
    public void deleteFile(String fileUrl) {
        if (fileUrl == null || fileUrl.isBlank()) return;

        try {
            String relativePath = fileUrl.replace(baseUrl + "/files/", "");
            Path filePath = Paths.get(uploadDir, relativePath);
            Files.deleteIfExists(filePath);
            log.info("FileStorageService: Deleted file at '{}'", filePath);
        } catch (IOException e) {
            log.warn("FileStorageService: Could not delete old file '{}': {}", fileUrl, e.getMessage());
        }
    }
    private void validateFile(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("File cannot be empty.");
        }
        if (file.getSize() > MAX_FILE_SIZE_BYTES) {
            throw new BusinessException("File size exceeds the 5 MB limit.");
        }
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_CONTENT_TYPES.contains(contentType)) {
            throw new BusinessException("Invalid file type. Only JPEG, PNG, and WebP images are allowed.");
        }
    }

    private String getExtension(String filename) {
        if (filename == null || !filename.contains(".")) return "bin";
        return filename.substring(filename.lastIndexOf('.') + 1).toLowerCase();
    }
}
