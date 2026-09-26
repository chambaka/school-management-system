package tz.co.chambaka.school.management.dto.academic;

import java.util.List;

public record GradeImportResponse(
        int saved,
        int skipped,
        List<String> problems
) {
}
