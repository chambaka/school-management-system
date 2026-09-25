package tz.co.chambaka.school.management.service;

import tz.co.chambaka.school.management.dto.academic.BuildingRequest;
import tz.co.chambaka.school.management.dto.academic.BuildingResponse;
import tz.co.chambaka.school.management.exception.BusinessException;
import tz.co.chambaka.school.management.exception.DuplicateResourceException;
import tz.co.chambaka.school.management.exception.ResourceNotFoundException;
import tz.co.chambaka.school.management.model.Building;
import tz.co.chambaka.school.management.model.Classroom;
import tz.co.chambaka.school.management.repository.BuildingRepository;
import tz.co.chambaka.school.management.repository.ClassroomRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
public class BuildingService {

    private final BuildingRepository buildingRepository;
    private final ClassroomRepository classroomRepository;

    public BuildingService(BuildingRepository buildingRepository, ClassroomRepository classroomRepository) {
        this.buildingRepository = buildingRepository;
        this.classroomRepository = classroomRepository;
    }

    @Transactional(readOnly = true)
    public List<BuildingResponse> list(Long schoolId) {
        return buildingRepository.findBySchoolIdOrderByNameAsc(schoolId).stream()
                .map(BuildingService::toResponse)
                .toList();
    }

    @Transactional
    public BuildingResponse create(Long schoolId, BuildingRequest request) {
        String name = request.name().trim();
        if (buildingRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, name)) {
            throw new DuplicateResourceException("Building name already exists");
        }
        Building building = new Building();
        building.setSchoolId(schoolId);
        building.setName(name);
        building.setNotes(blankToNull(request.notes()));
        return toResponse(buildingRepository.save(building));
    }

    @Transactional
    public BuildingResponse update(Long schoolId, Long id, BuildingRequest request) {
        Building building = require(schoolId, id);
        String name = request.name().trim();
        if (!building.getName().equalsIgnoreCase(name)
                && buildingRepository.existsBySchoolIdAndNameIgnoreCase(schoolId, name)) {
            throw new DuplicateResourceException("Building name already exists");
        }
        building.setName(name);
        building.setNotes(blankToNull(request.notes()));
        for (Classroom classroom : classroomRepository.findBySiteId(id)) {
            classroom.setBuilding(name);
        }
        return toResponse(building);
    }

    @Transactional
    public void delete(Long schoolId, Long id) {
        Building building = require(schoolId, id);
        if (classroomRepository.existsBySiteId(id)) {
            throw new BusinessException("Remove rooms from this building first");
        }
        buildingRepository.delete(building);
    }

    public Building require(Long schoolId, Long id) {
        return buildingRepository.findByIdAndSchoolId(id, schoolId)
                .orElseThrow(() -> ResourceNotFoundException.of("Building", id));
    }

    private static BuildingResponse toResponse(Building building) {
        return new BuildingResponse(building.getId(), building.getName(), building.getNotes());
    }

    private static String blankToNull(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }
}
