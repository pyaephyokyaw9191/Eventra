package com.cedric.Eventra.service;

import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;

import java.nio.file.Path;

public interface FileStorageService {

    /**
     * Stores a file in a specified subdirectory with a unique name.
     * Deletes the old file if oldFilename is provided.
     *
     * @param file         The file to store.
     * @param subDirectory The subdirectory within the base upload directory (e.g., "profile-pictures").
     * @param filePrefix   A prefix for the generated filename (e.g., "user_" + userId + "_profile_").
     * @param oldFilename  The filename of the existing file to be replaced (can be null).
     * @return The uniquely generated filename of the stored file.
     */
    String storeFile(MultipartFile file, String subDirectory, String filePrefix, String oldFilename);

    /**
     * Loads a file as a Spring Resource from a specific subdirectory.
     * @param subDirectory The subdirectory.
     * @param filename The name of the file to load.
     * @return The Resource.
     */
    Resource loadFileAsResource(String subDirectory, String filename);

    /**
     * Deletes a file from a specific subdirectory.
     * @param subDirectory The subdirectory.
     * @param filename The name of the file to delete.
     */
    void deleteFile(String subDirectory, String filename);

    String getFileUrl(String urlPathSegment, String filename);
}