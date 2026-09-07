package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Campus;
import tz.co.chambaka.school.management.repository.CampusRepository;
import org.springframework.stereotype.Service;

@Service
public class CampusService {

    private final CampusRepository campusRepository;

    public CampusService(CampusRepository campusRepository) {
        this.campusRepository = campusRepository;
    }

    public Campus requirePrimary(Long schoolId) {
        return campusRepository.findFirstBySchoolIdAndPrimaryCampusTrue(schoolId)
                .orElseThrow(() -> new ResourceNotFoundException("School workspace is not ready"));
    }

    public Campus requireInSchool(Long campusId, Long schoolId) {
        return campusRepository.findByIdAndSchoolId(campusId, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("School", campusId));
    }
}
