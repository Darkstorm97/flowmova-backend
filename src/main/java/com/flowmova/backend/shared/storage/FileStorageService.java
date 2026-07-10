package com.flowmova.backend.shared.storage;

import java.io.IOException;
import java.io.InputStream;

public interface FileStorageService {

    StoredFile storeImage(String folder, String originalFilename, String contentType, long size, InputStream inputStream)
            throws IOException;
}
