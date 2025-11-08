package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.FacultyRequest;
import com.ptit.schedule.dto.FacultyResponse;
import com.ptit.schedule.entity.Faculty;
import com.ptit.schedule.repository.FacultyRepository;
import com.ptit.schedule.service.FacultyService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class FacultyServiceImpl implements FacultyService {
    
    private final FacultyRepository facultyRepository;
    @Override
    @Transactional(readOnly = true)
    public List<FacultyResponse> getAllFaculties() {
        return facultyRepository.findAll()
                .stream()
                .map(FacultyResponse::fromEntity)
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public FacultyResponse getFacultyById(String id) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + id));
        return FacultyResponse.fromEntity(faculty);
    }

    @Override
    public FacultyResponse createFaculty(FacultyRequest request) {
        Faculty faculty = new Faculty();
        faculty.setId(UUID.randomUUID().toString());
        faculty.setFacultyName(request.getFacultyName());
        
        Faculty savedFaculty = facultyRepository.save(faculty);
        return FacultyResponse.fromEntity(savedFaculty);
    }

    @Override
    public FacultyResponse updateFaculty(String id, FacultyRequest request) {
        Faculty faculty = facultyRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + id));
        
        faculty.setFacultyName(request.getFacultyName());
        
        Faculty savedFaculty = facultyRepository.save(faculty);
        return FacultyResponse.fromEntity(savedFaculty);
    }

    @Override
    public void deleteFaculty(String id) {
        if (!facultyRepository.existsById(id)) {
            throw new RuntimeException("Faculty not found with id: " + id);
        }
        facultyRepository.deleteById(id);
    }
}
