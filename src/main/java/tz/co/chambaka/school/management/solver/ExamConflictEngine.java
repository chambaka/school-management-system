package tz.co.chambaka.school.management.solver;

import tz.co.chambaka.school.management.model.Classroom;
import tz.co.chambaka.school.management.model.ExamSubject;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public final class ExamConflictEngine {

    private ExamConflictEngine() {
    }

    public static boolean overlaps(LocalDate dateA, LocalTime startA, LocalTime endA,
                                  LocalDate dateB, LocalTime startB, LocalTime endB) {
        if (dateA == null || dateB == null || startA == null || endA == null || startB == null || endB == null) {
            return false;
        }
        if (!dateA.equals(dateB)) {
            return false;
        }
        return startA.isBefore(endB) && startB.isBefore(endA);
    }

    public static boolean invigilatorClash(ExamSubject candidate, List<ExamSubject> scheduled) {
        if (candidate.getInvigilator() == null) {
            return false;
        }
        Long invigilatorId = candidate.getInvigilator().getId();
        return scheduled.stream()
                .filter(paper -> paper.getInvigilator() != null && invigilatorId.equals(paper.getInvigilator().getId()))
                .anyMatch(paper -> overlaps(
                        candidate.getExamDate(), candidate.getStartTime(), candidate.getEndTime(),
                        paper.getExamDate(), paper.getStartTime(), paper.getEndTime()));
    }

    public static boolean venueClash(ExamSubject candidate, List<ExamSubject> scheduled) {
        if (candidate.getVenue() == null || candidate.getVenue().isBlank()) {
            return false;
        }
        return scheduled.stream()
                .filter(paper -> candidate.getVenue().equalsIgnoreCase(paper.getVenue()))
                .anyMatch(paper -> overlaps(
                        candidate.getExamDate(), candidate.getStartTime(), candidate.getEndTime(),
                        paper.getExamDate(), paper.getStartTime(), paper.getEndTime()));
    }

    public static boolean roomTooSmall(Classroom room, int studentCount) {
        return room != null && room.getCapacity() != null && room.getCapacity() < studentCount;
    }
}
