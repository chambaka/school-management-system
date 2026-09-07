package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.BusinessException;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.List;
import java.util.Optional;
import java.util.Set;

@Service
public class PhotoStorageService {

    static final long MAX_BYTES = 2 * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private static final List<String> EXTENSIONS = List.of(".jpg", ".png", ".webp");

    private final Path root;

    public PhotoStorageService(@Value("${sms.uploads.dir:uploads}") String uploadDir) {
        this.root = Path.of(uploadDir).toAbsolutePath().normalize();
    }

    public Path storeStudentPhoto(Long schoolId, Long studentId, MultipartFile file) {
        return storePhoto(schoolId, "students", studentId, file);
    }

    public Path storeTeacherPhoto(Long schoolId, Long teacherId, MultipartFile file) {
        return storePhoto(schoolId, "teachers", teacherId, file);
    }

    public Optional<StoredPhoto> findStudentPhoto(Long schoolId, Long studentId) {
        return findPhoto(schoolId, "students", studentId);
    }

    public Optional<StoredPhoto> findTeacherPhoto(Long schoolId, Long teacherId) {
        return findPhoto(schoolId, "teachers", teacherId);
    }

    public void deleteStudentPhoto(Long schoolId, Long studentId) {
        deleteExisting(schoolId, "students", studentId);
    }

    public void deleteTeacherPhoto(Long schoolId, Long teacherId) {
        deleteExisting(schoolId, "teachers", teacherId);
    }

    private Path storePhoto(Long schoolId, String kind, Long personId, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please choose a photo");
        }
        if (file.getSize() > MAX_BYTES) {
            throw new BusinessException("Photo must be 2 MB or smaller");
        }
        String contentType = normalizeType(file.getContentType(), file.getOriginalFilename());
        Path dir = personDir(schoolId, kind);
        Path dest = dir.resolve(personId + extension(contentType));
        try {
            Files.createDirectories(dir);
            deleteExisting(schoolId, kind, personId);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            return dest;
        } catch (IOException ex) {
            throw new BusinessException("Could not save photo");
        }
    }

    private Optional<StoredPhoto> findPhoto(Long schoolId, String kind, Long personId) {
        Path dir = personDir(schoolId, kind);
        for (String ext : EXTENSIONS) {
            Path path = dir.resolve(personId + ext);
            if (Files.isRegularFile(path)) {
                return Optional.of(new StoredPhoto(path, contentTypeFor(ext)));
            }
        }
        return Optional.empty();
    }

    private void deleteExisting(Long schoolId, String kind, Long personId) {
        Path dir = personDir(schoolId, kind);
        for (String ext : EXTENSIONS) {
            try {
                Files.deleteIfExists(dir.resolve(personId + ext));
            } catch (IOException ex) {
                throw new BusinessException("Could not replace photo");
            }
        }
    }

    private Path personDir(Long schoolId, String kind) {
        return root.resolve("schools").resolve(String.valueOf(schoolId)).resolve(kind);
    }

    static String normalizeType(String contentType, String filename) {
        String type = contentType == null ? "" : contentType.toLowerCase().split(";")[0].trim();
        if ("image/jpg".equals(type)) {
            type = "image/jpeg";
        }
        if (!type.isBlank()) {
            if (ALLOWED.contains(type)) {
                return type;
            }
            throw new BusinessException("Photo must be a JPEG, PNG, or WebP image");
        }
        String name = filename == null ? "" : filename.toLowerCase();
        if (name.endsWith(".jpg") || name.endsWith(".jpeg")) {
            return "image/jpeg";
        }
        if (name.endsWith(".png")) {
            return "image/png";
        }
        if (name.endsWith(".webp")) {
            return "image/webp";
        }
        throw new BusinessException("Photo must be a JPEG, PNG, or WebP image");
    }

    static String extension(String contentType) {
        return switch (contentType) {
            case "image/png" -> ".png";
            case "image/webp" -> ".webp";
            default -> ".jpg";
        };
    }

    static String contentTypeFor(String extension) {
        return switch (extension) {
            case ".png" -> "image/png";
            case ".webp" -> "image/webp";
            default -> "image/jpeg";
        };
    }
}
