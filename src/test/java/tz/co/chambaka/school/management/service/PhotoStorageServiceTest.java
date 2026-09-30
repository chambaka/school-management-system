package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class PhotoStorageServiceTest {

    @TempDir
    Path tempDir;

    private PhotoStorageService service;

    @BeforeEach
    void setUp() {
        service = new PhotoStorageService(tempDir.toString());
    }

    @Test
    void storeFindReplaceAndDelete() throws IOException {
        MockMultipartFile jpeg = new MockMultipartFile("file", "face.jpg", "image/jpeg", new byte[]{1, 2, 3});
        Path stored = service.storeStudentPhoto(1L, 9L, jpeg);
        assertThat(stored).exists();
        assertThat(stored.getFileName().toString()).isEqualTo("9.jpg");
        assertThat(service.findStudentPhoto(1L, 9L)).hasValueSatisfying(photo -> {
            assertThat(photo.contentType()).isEqualTo("image/jpeg");
            assertThat(photo.path()).isEqualTo(stored);
        });

        MockMultipartFile png = new MockMultipartFile("file", "face.png", "image/png", new byte[]{4, 5});
        Path replaced = service.storeStudentPhoto(1L, 9L, png);
        assertThat(replaced.getFileName().toString()).isEqualTo("9.png");
        assertThat(Files.exists(stored)).isFalse();
        assertThat(service.findStudentPhoto(1L, 9L)).hasValueSatisfying(photo ->
                assertThat(photo.contentType()).isEqualTo("image/png"));

        service.deleteStudentPhoto(1L, 9L);
        assertThat(service.findStudentPhoto(1L, 9L)).isEmpty();
        assertThat(service.findStudentPhoto(1L, 8L)).isEmpty();

        Path teacherStored = service.storeTeacherPhoto(1L, 4L,
                new MockMultipartFile("file", "staff.webp", "image/webp", new byte[]{7, 8}));
        assertThat(teacherStored.getFileName().toString()).isEqualTo("4.webp");
        assertThat(teacherStored.toString()).contains("teachers");
        assertThat(service.findTeacherPhoto(1L, 4L)).isPresent();
        service.deleteTeacherPhoto(1L, 4L);
        assertThat(service.findTeacherPhoto(1L, 4L)).isEmpty();
    }

    @Test
    void acceptsFilenameFallbackAndAliases() {
        MockMultipartFile jpeg = new MockMultipartFile("file", "face.jpeg", "", new byte[]{1});
        assertThat(service.storeStudentPhoto(2L, 1L, jpeg).getFileName().toString()).isEqualTo("1.jpg");

        MockMultipartFile png = new MockMultipartFile("file", "face.PNG", null, new byte[]{1});
        assertThat(service.storeStudentPhoto(2L, 2L, png).getFileName().toString()).isEqualTo("2.png");

        MockMultipartFile webp = new MockMultipartFile("file", "face.webp", "image/webp", new byte[]{1});
        assertThat(service.storeStudentPhoto(2L, 3L, webp).getFileName().toString()).isEqualTo("3.webp");
        assertThat(service.findStudentPhoto(2L, 3L)).hasValueSatisfying(photo ->
                assertThat(photo.contentType()).isEqualTo("image/webp"));

        MockMultipartFile jpgAlias = new MockMultipartFile("file", "face.jpg", "image/jpg; charset=utf-8", new byte[]{1});
        assertThat(service.storeStudentPhoto(2L, 4L, jpgAlias).getFileName().toString()).isEqualTo("4.jpg");
    }

    @Test
    void rejectsInvalidFiles() {
        assertThatThrownBy(() -> service.storeStudentPhoto(1L, 1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Please choose a photo");
        assertThatThrownBy(() -> service.storeStudentPhoto(1L, 1L,
                new MockMultipartFile("file", "face.jpg", "image/jpeg", new byte[0])))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Please choose a photo");

        byte[] tooBig = new byte[(int) PhotoStorageService.MAX_BYTES + 1];
        Arrays.fill(tooBig, (byte) 1);
        assertThatThrownBy(() -> service.storeStudentPhoto(1L, 1L,
                new MockMultipartFile("file", "face.jpg", "image/jpeg", tooBig)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Photo must be 2 MB or smaller");

        assertThatThrownBy(() -> service.storeStudentPhoto(1L, 1L,
                new MockMultipartFile("file", "face.gif", "image/gif", new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Photo must be a JPEG, PNG, or WebP image");
        assertThatThrownBy(() -> service.storeStudentPhoto(1L, 1L,
                new MockMultipartFile("file", "face.exe", "", new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Photo must be a JPEG, PNG, or WebP image");
        assertThatThrownBy(() -> service.storeStudentPhoto(1L, 1L,
                new MockMultipartFile("file", null, null, new byte[]{1})))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void replaceFailsWhenExistingFileCannotBeRemoved() throws IOException {
        Path blocker = tempDir.resolve("schools").resolve("4").resolve("students").resolve("1.jpg").resolve("nested");
        Files.createDirectories(blocker);
        assertThatThrownBy(() -> service.storeStudentPhoto(4L, 1L,
                new MockMultipartFile("file", "face.png", "image/png", new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Could not replace photo");
    }

    @Test
    void saveFailsWhenPathIsNotADirectory() throws IOException {
        Path blocker = tempDir.resolve("schools").resolve("3").resolve("students");
        Files.createDirectories(blocker.getParent());
        Files.writeString(blocker, "not-a-dir");
        assertThatThrownBy(() -> service.storeStudentPhoto(3L, 1L,
                new MockMultipartFile("file", "face.jpg", "image/jpeg", new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Could not save photo");
    }

    @Test
    void storesDocumentsAndImagesForAssignments() throws IOException {
        MockMultipartFile pdf = new MockMultipartFile("file", "work.pdf", "application/pdf", new byte[]{1, 2});
        Path stored = service.storeAssignmentFile(1L, 8L, pdf);
        assertThat(stored.getFileName().toString()).isEqualTo("8.pdf");
        assertThat(service.findAssignmentFile(1L, 8L)).hasValueSatisfying(file ->
                assertThat(file.contentType()).isEqualTo("application/pdf"));

        MockMultipartFile png = new MockMultipartFile("file", "sheet.png", "image/png", new byte[]{3});
        Path image = service.storeAssignmentFile(1L, 8L, png);
        assertThat(image.getFileName().toString()).isEqualTo("8.png");
        assertThat(Files.exists(stored)).isFalse();

        MockMultipartFile docx = new MockMultipartFile(
                "file", "notes.docx",
                "application/vnd.openxmlformats-officedocument.wordprocessingml.document",
                new byte[]{4});
        assertThat(service.storeSubmissionFile(1L, 8L, 3L, docx).getFileName().toString()).isEqualTo("8-3.docx");
        assertThat(service.findSubmissionFile(1L, 8L, 3L)).isPresent();

        MockMultipartFile byName = new MockMultipartFile("file", "scan.GIF", "application/octet-stream", new byte[]{5});
        assertThat(service.storeAssignmentFile(2L, 1L, byName).getFileName().toString()).isEqualTo("1.gif");

        MockMultipartFile jpegAlias = new MockMultipartFile("file", "pic.jpeg", "", new byte[]{6});
        assertThat(service.storeAssignmentFile(2L, 2L, jpegAlias).getFileName().toString()).isEqualTo("2.jpg");

        assertThatThrownBy(() -> service.storeAssignmentFile(1L, 1L, null))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Please choose a document or image");
        assertThatThrownBy(() -> service.storeAssignmentFile(1L, 1L,
                new MockMultipartFile("file", "x.exe", "application/x-msdownload", new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessageContaining("document");
        byte[] tooBig = new byte[(int) PhotoStorageService.MAX_DOCUMENT_BYTES + 1];
        Arrays.fill(tooBig, (byte) 1);
        assertThatThrownBy(() -> service.storeAssignmentFile(1L, 1L,
                new MockMultipartFile("file", "big.pdf", "application/pdf", tooBig)))
                .isInstanceOf(BusinessException.class)
                .hasMessage("File must be 10 MB or smaller");
        assertThat(service.findAssignmentFile(9L, 9L)).isEmpty();
        Path perFile = service.storeAssignmentFile(1L, 7L, 22L, pdf);
        assertThat(perFile.getFileName().toString()).isEqualTo("7-22.pdf");
        assertThat(service.findAssignmentFile(1L, 7L, 22L)).hasValueSatisfying(file ->
                assertThat(file.path().getFileName().toString()).isEqualTo("7-22.pdf"));
        service.deleteAssignmentFile(1L, 7L, 22L);
        assertThat(service.findAssignmentFile(1L, 7L, 22L)).isEmpty();
        Path legacy = service.storeAssignmentFile(3L, 4L, pdf);
        assertThat(service.findAssignmentFile(3L, 4L, 99L)).hasValueSatisfying(file ->
                assertThat(file.path().getFileName().toString()).isEqualTo("4.pdf"));
        service.deleteLegacyAssignmentFile(3L, 4L);
        assertThat(service.findAssignmentFile(3L, 4L)).isEmpty();
        assertThat(PhotoStorageService.normalizeDocumentType("application/x-pdf", null)).isEqualTo("application/pdf");
        assertThat(PhotoStorageService.normalizeDocumentType("image/jpg", "x")).isEqualTo("image/jpeg");
        assertThat(PhotoStorageService.contentTypeForDocument(".pdf")).isEqualTo("application/pdf");
        assertThat(PhotoStorageService.contentTypeForDocument(".unknown")).isEqualTo("application/octet-stream");
    }

    @Test
    void assignmentReplaceFailsWhenExistingFileCannotBeRemoved() throws IOException {
        Path blocker = tempDir.resolve("schools").resolve("5").resolve("assignments").resolve("1.pdf").resolve("nested");
        Files.createDirectories(blocker);
        assertThatThrownBy(() -> service.storeAssignmentFile(5L, 1L,
                new MockMultipartFile("file", "work.pdf", "application/pdf", new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Could not replace file");
    }

    @Test
    void assignmentSaveFailsWhenPathIsNotADirectory() throws IOException {
        Path blocker = tempDir.resolve("schools").resolve("6").resolve("assignments");
        Files.createDirectories(blocker.getParent());
        Files.writeString(blocker, "not-a-dir");
        assertThatThrownBy(() -> service.storeAssignmentFile(6L, 1L,
                new MockMultipartFile("file", "work.pdf", "application/pdf", new byte[]{1})))
                .isInstanceOf(BusinessException.class)
                .hasMessage("Could not save file");
    }

    @Test
    void typeHelpers() {
        assertThat(PhotoStorageService.normalizeType("image/png", null)).isEqualTo("image/png");
        assertThat(PhotoStorageService.extension("image/png")).isEqualTo(".png");
        assertThat(PhotoStorageService.extension("image/webp")).isEqualTo(".webp");
        assertThat(PhotoStorageService.extension("image/jpeg")).isEqualTo(".jpg");
        assertThat(PhotoStorageService.contentTypeFor(".png")).isEqualTo("image/png");
        assertThat(PhotoStorageService.contentTypeFor(".webp")).isEqualTo("image/webp");
        assertThat(PhotoStorageService.contentTypeFor(".jpg")).isEqualTo("image/jpeg");
    }
}
