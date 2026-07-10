package com.flowmova.backend.shared.storage;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.Locale;
import java.util.Set;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.web.server.ResponseStatusException;

@Service
class LocalFileStorageService implements FileStorageService {

    private static final Set<String> SUPPORTED_IMAGE_TYPES = Set.of(
            "image/jpeg",
            "image/png",
            "image/webp");

    private final StorageProperties properties;

    LocalFileStorageService(StorageProperties properties) {
        this.properties = properties;
    }

    @Override
    public StoredFile storeImage(
            String folder,
            String originalFilename,
            String contentType,
            long size,
            InputStream inputStream) throws IOException {
        String normalizedContentType = contentType == null ? "" : contentType.toLowerCase(Locale.ROOT);
        if (!SUPPORTED_IMAGE_TYPES.contains(normalizedContentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image type must be JPEG, PNG or WEBP");
        }
        if (size <= 0) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is required");
        }
        if (size > properties.maxImageBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Image file is too large");
        }

        String extension = extensionFor(normalizedContentType);
        Path relativePath = Path.of(folder, "cover" + extension);
        Path target = properties.localRoot().resolve(relativePath).normalize();
        Path normalizedRoot = properties.localRoot().toAbsolutePath().normalize();
        Path absoluteTarget = target.toAbsolutePath().normalize();
        if (!absoluteTarget.startsWith(normalizedRoot)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Invalid storage path");
        }

        Files.createDirectories(absoluteTarget.getParent());
        Files.copy(inputStream, absoluteTarget, StandardCopyOption.REPLACE_EXISTING);

        String publicUrl = properties.publicUrlPrefix().replaceAll("/+$", "")
                + "/"
                + relativePath.toString().replace('\\', '/');
        return new StoredFile(publicUrl);
    }

    private String extensionFor(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }
}
