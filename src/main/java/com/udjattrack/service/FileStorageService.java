package com.udjattrack.service;

import org.springframework.web.multipart.MultipartFile;

/**
 * FileStorageService — contract for handling multipart file uploads.
 * The local implementation saves files to the /uploads directory on the server.
 * Can be swapped for a cloud implementation (e.g., S3, GCS) without changing
 * any controller or service code.
 */
public interface FileStorageService {

    /**
     * Stores the given file and returns the publicly accessible URL string.
     *
     * @param file      the multipart file received from the HTTP request
     * @param subfolder a logical subdirectory (e.g., "drivers", "vehicles")
     * @return the URL string that can be persisted in the database
     */
    String storeFile(MultipartFile file, String subfolder);

    /**
     * Deletes a previously stored file by its URL.
     * Used when replacing an existing photo.
     *
     * @param fileUrl the URL previously returned by storeFile
     */
    void deleteFile(String fileUrl);
}
