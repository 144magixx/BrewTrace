package com.minyuwei.xhs.coffeeagent.shared.infrastructure.storage;

import java.nio.file.Path;

public class LocalFileStorageService {
    private final Path root;

    public LocalFileStorageService(Path root) {
        this.root = root;
    }

    public Path resolveSessionPath(String sessionId, String resourceType, String fileName) {
        return root.resolve(sessionId).resolve(resourceType).resolve(fileName).normalize();
    }
}
