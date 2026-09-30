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
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

@Service
public class PhotoStorageService {

    static final long MAX_BYTES = 2 * 1024 * 1024;
    static final long MAX_DOCUMENT_BYTES = 10 * 1024 * 1024;
    private static final Set<String> ALLOWED = Set.of("image/jpeg", "image/png", "image/webp");
    private static final List<String> EXTENSIONS = List.of(".jpg", ".png", ".webp");
    private static final Map<String, String> DOCUMENT_TYPES = documentTypes();
    private static final List<String> DOCUMENT_EXTENSIONS = List.copyOf(DOCUMENT_TYPES.values());

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

    public Path storeAssignmentFile(Long schoolId, Long assignmentId, MultipartFile file) {
        return storeDocument(schoolId, "assignments", String.valueOf(assignmentId), file);
    }

    public Path storeAssignmentFile(Long schoolId, Long assignmentId, Long attachmentId, MultipartFile file) {
        return storeDocument(schoolId, "assignments", assignmentId + "-" + attachmentId, file);
    }

    public Optional<StoredPhoto> findAssignmentFile(Long schoolId, Long assignmentId) {
        return findDocument(schoolId, "assignments", String.valueOf(assignmentId));
    }

    public Optional<StoredPhoto> findAssignmentFile(Long schoolId, Long assignmentId, Long attachmentId) {
        Optional<StoredPhoto> stored = findDocument(schoolId, "assignments", assignmentId + "-" + attachmentId);
        if (stored.isPresent()) {
            return stored;
        }
        return findDocument(schoolId, "assignments", String.valueOf(assignmentId));
    }

    public void deleteAssignmentFile(Long schoolId, Long assignmentId, Long attachmentId) {
        Path dir = personDir(schoolId, "assignments");
        deleteDocumentExisting(dir, assignmentId + "-" + attachmentId);
    }

    public void deleteLegacyAssignmentFile(Long schoolId, Long assignmentId) {
        deleteDocumentExisting(personDir(schoolId, "assignments"), String.valueOf(assignmentId));
    }

    public Path storeSubmissionFile(Long schoolId, Long assignmentId, Long studentId, MultipartFile file) {
        return storeDocument(schoolId, "assignment-submissions", assignmentId + "-" + studentId, file);
    }

    public Optional<StoredPhoto> findSubmissionFile(Long schoolId, Long assignmentId, Long studentId) {
        return findDocument(schoolId, "assignment-submissions", assignmentId + "-" + studentId);
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

    private Path storeDocument(Long schoolId, String kind, String fileKey, MultipartFile file) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("Please choose a document or image");
        }
        if (file.getSize() > MAX_DOCUMENT_BYTES) {
            throw new BusinessException("File must be 10 MB or smaller");
        }
        String contentType = normalizeDocumentType(file.getContentType(), file.getOriginalFilename());
        Path dir = personDir(schoolId, kind);
        Path dest = dir.resolve(fileKey + DOCUMENT_TYPES.get(contentType));
        try {
            Files.createDirectories(dir);
            deleteDocumentExisting(dir, fileKey);
            try (InputStream in = file.getInputStream()) {
                Files.copy(in, dest, StandardCopyOption.REPLACE_EXISTING);
            }
            return dest;
        } catch (IOException ex) {
            throw new BusinessException("Could not save file");
        }
    }

    private Optional<StoredPhoto> findDocument(Long schoolId, String kind, String fileKey) {
        Path dir = personDir(schoolId, kind);
        for (String ext : DOCUMENT_EXTENSIONS) {
            Path path = dir.resolve(fileKey + ext);
            if (Files.isRegularFile(path)) {
                return Optional.of(new StoredPhoto(path, contentTypeForDocument(ext)));
            }
        }
        return Optional.empty();
    }

    private void deleteDocumentExisting(Path dir, String fileKey) {
        for (String ext : DOCUMENT_EXTENSIONS) {
            try {
                Files.deleteIfExists(dir.resolve(fileKey + ext));
            } catch (IOException ex) {
                throw new BusinessException("Could not replace file");
            }
        }
    }

    static String normalizeDocumentType(String contentType, String filename) {
        String type = contentType == null ? "" : contentType.toLowerCase().split(";")[0].trim();
        if ("image/jpg".equals(type)) {
            type = "image/jpeg";
        }
        if ("application/x-pdf".equals(type)) {
            type = "application/pdf";
        }
        if (!type.isBlank()) {
            if (DOCUMENT_TYPES.containsKey(type)) {
                return type;
            }
            if (!"application/octet-stream".equals(type)) {
                throw new BusinessException(documentTypeMessage());
            }
        }
        String name = filename == null ? "" : filename.toLowerCase();
        for (Map.Entry<String, String> entry : DOCUMENT_TYPES.entrySet()) {
            if (name.endsWith(entry.getValue()) || (".jpg".equals(entry.getValue()) && name.endsWith(".jpeg"))) {
                return entry.getKey();
            }
        }
        throw new BusinessException(documentTypeMessage());
    }

    static String contentTypeForDocument(String extension) {
        for (Map.Entry<String, String> entry : DOCUMENT_TYPES.entrySet()) {
            if (entry.getValue().equals(extension)) {
                return entry.getKey();
            }
        }
        return "application/octet-stream";
    }

    private static String documentTypeMessage() {
        return "Attach a document (PDF, Word, Excel, PowerPoint, text) or an image (JPEG, PNG, WebP, GIF)";
    }

    private static Map<String, String> documentTypes() {
        Map<String, String> types = new LinkedHashMap<>();
        types.put("image/jpeg", ".jpg");
        types.put("image/png", ".png");
        types.put("image/webp", ".webp");
        types.put("image/gif", ".gif");
        types.put("application/pdf", ".pdf");
        types.put("application/vnd.openxmlformats-officedocument.wordprocessingml.document", ".docx");
        types.put("application/msword", ".doc");
        types.put("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", ".xlsx");
        types.put("application/vnd.ms-excel", ".xls");
        types.put("application/vnd.openxmlformats-officedocument.presentationml.presentation", ".pptx");
        types.put("application/vnd.ms-powerpoint", ".ppt");
        types.put("text/plain", ".txt");
        types.put("application/rtf", ".rtf");
        types.put("application/vnd.oasis.opendocument.text", ".odt");
        return Map.copyOf(types);
    }
}
