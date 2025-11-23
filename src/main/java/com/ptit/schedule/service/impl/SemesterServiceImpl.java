package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.SemesterRequest;
import com.ptit.schedule.dto.SemesterResponse;
import com.ptit.schedule.entity.Semester;
import com.ptit.schedule.repository.SemesterRepository;
import com.ptit.schedule.service.SemesterService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SemesterServiceImpl implements SemesterService {
    
    private final SemesterRepository semesterRepository;
    
    @Override
    @Transactional(readOnly = true)
    public List<SemesterResponse> getAllSemesters() {
        return semesterRepository.findAllOrderByYearAndSemesterDesc()
                .stream()
                .map(SemesterResponse::fromEntity)
                .collect(Collectors.toList());
    }
    
    @Override
    @Transactional(readOnly = true)
    public SemesterResponse getSemesterById(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ với ID: " + id));
        return SemesterResponse.fromEntity(semester);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SemesterResponse getSemesterByName(String semesterName) {
        Semester semester = semesterRepository.findBySemesterName(semesterName)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ với tên: " + semesterName));
        return SemesterResponse.fromEntity(semester);
    }
    
    @Override
    @Transactional(readOnly = true)
    public SemesterResponse getActiveSemester() {
        Semester semester = semesterRepository.findByIsActiveTrue()
                .orElseThrow(() -> new RuntimeException("Không có học kỳ nào đang hoạt động"));
        return SemesterResponse.fromEntity(semester);
    }
    
    @Override
    public SemesterResponse createSemester(SemesterRequest request) {
        // Kiểm tra tên học kỳ và năm học đã tồn tại chưa
        if (semesterRepository.existsBySemesterNameAndAcademicYear(
                request.getSemesterName(), request.getAcademicYear())) {
            throw new RuntimeException(
                String.format("Đã tồn tại học kỳ '%s' trong năm học '%s'", 
                    request.getSemesterName(), request.getAcademicYear())
            );
        }
        
        // Nếu semester mới được set là active, deactivate tất cả các semester khác
        if (request.getIsActive() != null && request.getIsActive()) {
            deactivateAllSemesters();
        }
        
        Semester semester = Semester.builder()
                .semesterName(request.getSemesterName())
                .academicYear(request.getAcademicYear())
                .startDate(request.getStartDate())
                .endDate(request.getEndDate())
                .isActive(request.getIsActive() != null ? request.getIsActive() : false)
                .description(request.getDescription())
                .build();
        
        Semester savedSemester = semesterRepository.save(semester);
        return SemesterResponse.fromEntity(savedSemester);
    }
    
    @Override
    public SemesterResponse updateSemester(Long id, SemesterRequest request) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ với ID: " + id));
        
        // Kiểm tra tên học kỳ và năm học có bị trùng với semester khác không
        boolean isDifferentSemester = !semester.getSemesterName().equals(request.getSemesterName()) 
                || !semester.getAcademicYear().equals(request.getAcademicYear());
        
        if (isDifferentSemester 
                && semesterRepository.existsBySemesterNameAndAcademicYear(
                    request.getSemesterName(), request.getAcademicYear())) {
            throw new RuntimeException(
                String.format("Đã tồn tại học kỳ '%s' trong năm học '%s'", 
                    request.getSemesterName(), request.getAcademicYear())
            );
        }
        
        // Nếu semester được set là active, deactivate tất cả các semester khác
        if (request.getIsActive() != null && request.getIsActive() && !semester.getIsActive()) {
            deactivateAllSemesters();
        }
        
        semester.setSemesterName(request.getSemesterName());
        semester.setAcademicYear(request.getAcademicYear());
        semester.setStartDate(request.getStartDate());
        semester.setEndDate(request.getEndDate());
        semester.setIsActive(request.getIsActive() != null ? request.getIsActive() : semester.getIsActive());
        semester.setDescription(request.getDescription());
        
        Semester updatedSemester = semesterRepository.save(semester);
        return SemesterResponse.fromEntity(updatedSemester);
    }
    
    @Override
    public void deleteSemester(Long id) {
        if (!semesterRepository.existsById(id)) {
            throw new RuntimeException("Không tìm thấy học kỳ với ID: " + id);
        }
        semesterRepository.deleteById(id);
    }
    
    @Override
    public SemesterResponse activateSemester(Long id) {
        Semester semester = semesterRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ với ID: " + id));
        
        // Deactivate tất cả các semester khác
        deactivateAllSemesters();
        
        // Activate semester này
        semester.setIsActive(true);
        Semester activatedSemester = semesterRepository.save(semester);
        
        return SemesterResponse.fromEntity(activatedSemester);
    }
    
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllSemesterNames() {
        return semesterRepository.findAllOrderByYearAndSemesterDesc()
                .stream()
                .map(Semester::getSemesterName)
                .collect(Collectors.toList());
    }
    
    /**
     * Helper method: Deactivate tất cả các semester
     */
    private void deactivateAllSemesters() {
        List<Semester> allSemesters = semesterRepository.findAll();
        allSemesters.forEach(s -> s.setIsActive(false));
        semesterRepository.saveAll(allSemesters);
    }
}
