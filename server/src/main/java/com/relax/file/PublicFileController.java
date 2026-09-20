package com.relax.file;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public file serving endpoint - no authentication required.
 * Used for serving images that need to be publicly accessible:
 * - Technician photos
 * - Project covers
 * - Banner images
 * - Avatars
 */
@RestController
@RequestMapping("/api/v1/public/files")
public class PublicFileController {

    private final FileMapper fileMapper;
    private final DatabaseObjectStorage storage;

    PublicFileController(FileMapper fileMapper, DatabaseObjectStorage storage) {
        this.fileMapper = fileMapper;
        this.storage = storage;
    }

    @GetMapping("/{id}")
    ResponseEntity<Resource> getFile(@PathVariable long id) {
        FileAsset asset = fileMapper.findById(id)
                .orElse(null);
        if (asset == null || !"READY".equals(asset.status())) {
            return ResponseEntity.notFound().build();
        }
        Resource resource = storage.localResource(asset);
        String encodedName = URLEncoder.encode(asset.originalName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(asset.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "inline; filename*=UTF-8''" + encodedName)
                .cacheControl(org.springframework.http.CacheControl.maxAge(java.time.Duration.ofHours(24)))
                .body(resource);
    }
}
