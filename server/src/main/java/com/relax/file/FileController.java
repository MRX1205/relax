package com.relax.file;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.relax.auth.CurrentUser;
import com.relax.common.api.ApiResponse;

@RestController
@RequestMapping("/api/v1/files")
public class FileController {

    private final FileService fileService;

    FileController(FileService fileService) {
        this.fileService = fileService;
    }

    @PostMapping("/upload-policies")
    ApiResponse<FileService.UploadPolicy> createUploadPolicy(@AuthenticationPrincipal CurrentUser currentUser,
            @Valid @RequestBody UploadPolicyRequest request) {
        return ApiResponse.success(fileService.createUploadPolicy(currentUser.id(), request.toServiceRequest()));
    }

    @PutMapping(value = "/{id}/content", consumes = MediaType.ALL_VALUE)
    ApiResponse<Void> uploadLocal(@PathVariable long id,
            @RequestHeader("X-Upload-Token") String uploadToken,
            @RequestHeader(HttpHeaders.CONTENT_TYPE) String contentType,
            @RequestBody byte[] content) {
        fileService.uploadLocal(id, uploadToken, contentType, content);
        return ApiResponse.success(null);
    }

    @PostMapping("/{id}/complete")
    ApiResponse<FileService.FileView> complete(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable long id) {
        return ApiResponse.success(fileService.complete(currentUser.id(), id));
    }

    @GetMapping
    ApiResponse<List<FileService.FileView>> files(@AuthenticationPrincipal CurrentUser currentUser) {
        return ApiResponse.success(fileService.files(currentUser.id()));
    }

    @GetMapping("/{id}/download")
    ApiResponse<ObjectStorage.DownloadGrant> download(@AuthenticationPrincipal CurrentUser currentUser,
            @PathVariable long id, HttpServletRequest servletRequest) {
        return ApiResponse.success(fileService.createDownloadGrant(currentUser, id, servletRequest.getRemoteAddr()));
    }

    @GetMapping("/{id}/content")
    ResponseEntity<Resource> localContent(@AuthenticationPrincipal CurrentUser currentUser, @PathVariable long id) {
        FileService.LocalFile file = fileService.localFile(currentUser, id);
        String encodedName = URLEncoder.encode(file.fileName(), StandardCharsets.UTF_8).replace("+", "%20");
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(file.mimeType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + encodedName)
                .body(file.resource());
    }

    public record UploadPolicyRequest(
            @NotBlank String purpose,
            @NotBlank @Size(max = 180) String fileName,
            @NotBlank @Size(max = 100) String mimeType,
            @Min(1) @Max(10485760) long size) {

        FileService.UploadPolicyRequest toServiceRequest() {
            return new FileService.UploadPolicyRequest(purpose, fileName, mimeType, size);
        }
    }
}
