package com.cedric.Eventra.service;

import com.cedric.Eventra.exception.FileStorageException;
import com.cedric.Eventra.exception.ResourceNotFoundException;
import com.cedric.Eventra.service.FileStorageService;
import jakarta.annotation.PostConstruct; // Import PostConstruct
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.MalformedURLException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.UUID;

@Service
public class FileStorageServiceImpl implements FileStorageService {

    private final Path baseStorageLocation;

    @Value("${file.upload-dir.service-images}")
    private String serviceImagesSubDir; // Renamed for clarity (was serviceImagesDirName)

    @Value("${file.upload-dir.profile-pictures}")
    private String profilePicturesSubDir; // Renamed for clarity

    @Value("${file.upload-dir.cover-photos}")
    private String coverPhotosSubDir; // Renamed for clarity

    // Constructor only initializes baseStorageLocation
    public FileStorageServiceImpl(@Value("${file.upload-dir.base}") String baseUploadDir) {
        this.baseStorageLocation = Paths.get(baseUploadDir).toAbsolutePath().normalize();
    }

    @PostConstruct // This method will run after dependency injection is done
    public void init() {
        try {
            Files.createDirectories(this.baseStorageLocation);
            // Now these @Value injected fields will be populated
            Files.createDirectories(this.baseStorageLocation.resolve(serviceImagesSubDir));
            Files.createDirectories(this.baseStorageLocation.resolve(profilePicturesSubDir));
            Files.createDirectories(this.baseStorageLocation.resolve(coverPhotosSubDir));
        } catch (Exception ex) {
            System.err.println("Failed to create storage directories. Base: " + this.baseStorageLocation +
                    ", Service: " + serviceImagesSubDir +
                    ", Profile: " + profilePicturesSubDir +
                    ", Cover: " + coverPhotosSubDir);
            throw new FileStorageException("Could not create base or sub storage directories.", ex);
        }
    }

    private Path getFullStoragePath(String subDirectory) {
        // Ensure subDirectory itself is not null if it comes from an unvalidated source
        if (subDirectory == null) {
            throw new FileStorageException("Subdirectory cannot be null.");
        }
        return this.baseStorageLocation.resolve(subDirectory).normalize();
    }

    @Override
    public String storeFile(MultipartFile file, String subDirectory, String filePrefix, String oldFilename) {
        if (file.isEmpty()) {
            throw new FileStorageException("Failed to store empty file.");
        }

        Path targetDirectory = getFullStoragePath(subDirectory);
        // No need to call Files.createDirectories(targetDirectory) here again,
        // as @PostConstruct should have already created it.

        String originalFilename = StringUtils.cleanPath(file.getOriginalFilename());
        String fileExtension = "";
        try {
            fileExtension = originalFilename.substring(originalFilename.lastIndexOf("."));
            if (!isValidImageExtension(fileExtension) &&
                    (subDirectory.equals(profilePicturesSubDir) ||
                            subDirectory.equals(coverPhotosSubDir) ||
                            subDirectory.equals(serviceImagesSubDir)) ) {
                throw new FileStorageException("Invalid image file extension: " + fileExtension + " for " + subDirectory);
            }
        } catch (IndexOutOfBoundsException e) { // Catch if originalFilename has no extension
            if (subDirectory.equals(profilePicturesSubDir) || subDirectory.equals(coverPhotosSubDir) || subDirectory.equals(serviceImagesSubDir)) {
                throw new FileStorageException("File extension is missing or filename is invalid for image in " + subDirectory + ".");
            }
            // For other non-image file types, you might allow no extension or handle differently
            fileExtension = ""; // Or some default if absolutely necessary for non-images
        }


        String uniqueFileName = (StringUtils.hasText(filePrefix) ? filePrefix : "") + UUID.randomUUID().toString() + fileExtension;

        try {
            if (uniqueFileName.contains("..")) {
                throw new FileStorageException("Filename contains invalid path sequence: " + uniqueFileName);
            }

            if (StringUtils.hasText(oldFilename)) {
                deleteFile(subDirectory, oldFilename);
            }

            Path targetLocation = targetDirectory.resolve(uniqueFileName);
            Files.copy(file.getInputStream(), targetLocation, StandardCopyOption.REPLACE_EXISTING);

            return uniqueFileName;
        } catch (IOException ex) {
            throw new FileStorageException("Could not store file " + uniqueFileName + ". Please try again!", ex);
        }
    }

    private boolean isValidImageExtension(String extension) {
        if (!StringUtils.hasText(extension)) return false;
        String lowerExtension = extension.toLowerCase();
        return lowerExtension.equals(".png") || lowerExtension.equals(".jpg") || lowerExtension.equals(".jpeg") || lowerExtension.equals(".gif");
    }

    @Override
    public Resource loadFileAsResource(String subDirectory, String filename) {
        try {
            Path filePath = getFullStoragePath(subDirectory).resolve(filename).normalize();
            Resource resource = new UrlResource(filePath.toUri());
            if (resource.exists() && resource.isReadable()) {
                return resource;
            } else {
                throw new ResourceNotFoundException("File not found or not readable: " + subDirectory + "/" + filename);
            }
        } catch (MalformedURLException ex) {
            throw new ResourceNotFoundException("File not found (malformed URL): " + subDirectory + "/" + filename, ex);
        }
    }

    @Override
    public void deleteFile(String subDirectory, String filename) {
        if (!StringUtils.hasText(filename)) {
            return;
        }
        try {
            Path filePath = getFullStoragePath(subDirectory).resolve(filename).normalize();
            Files.deleteIfExists(filePath);
        } catch (IOException ex) {
            System.err.println("Could not delete file " + subDirectory + "/" + filename + ": " + ex.getMessage());
        }
    }

    @Override
    public String getFileUrl(String urlPathSegment, String filename) {
        if (!StringUtils.hasText(filename)) {
            return null;
        }
        return ServletUriComponentsBuilder.fromCurrentContextPath()
                .pathSegment("uploads", urlPathSegment)
                .path(filename)
                .toUriString();
    }
}