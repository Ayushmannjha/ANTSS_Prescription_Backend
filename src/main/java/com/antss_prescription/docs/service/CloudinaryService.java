package com.antss_prescription.docs.service;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.util.Map;
import java.net.URI;

@Service
@RequiredArgsConstructor
public class CloudinaryService {

    private final Cloudinary cloudinary;

    public UploadResult uploadFile(MultipartFile file) throws IOException {

        Map<?, ?> uploadResult = cloudinary.uploader().upload(
                file.getBytes(),
                ObjectUtils.asMap("resource_type", "auto", "folder", "antss/documents"));

        return new UploadResult(
                uploadResult.get("secure_url").toString(),
                uploadResult.get("public_id").toString(),
                uploadResult.get("resource_type").toString());
    }

    public void deleteFile(String publicId, String resourceType) throws IOException {
        if (publicId == null || publicId.isBlank()) return;
        Map<?, ?> result = cloudinary.uploader().destroy(publicId,
                ObjectUtils.asMap("resource_type", resourceType == null ? "image" : resourceType));
        String status = String.valueOf(result.get("result"));
        if (!"ok".equals(status) && !"not found".equals(status)) {
            throw new IOException("Cloudinary deletion failed");
        }
    }

    public String extractPublicId(String url) {
        if (url == null || url.isBlank()) return null;
        try {
            String path = URI.create(url).getPath();
            int uploadIndex = path.indexOf("/upload/");
            if (uploadIndex < 0) return null;
            String value = path.substring(uploadIndex + "/upload/".length());
            value = value.replaceFirst("^v[0-9]+/", "");
            int extension = value.lastIndexOf('.');
            return extension > 0 ? value.substring(0, extension) : value;
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }

    public record UploadResult(String url, String publicId, String resourceType) {}
}
