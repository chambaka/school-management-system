package tz.co.chambaka.school.management.service;

import java.nio.file.Path;

public record StoredPhoto(Path path, String contentType) {
}
