package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.SubjectRequest;
import com.ptit.schedule.dto.SubjectResponse;
import com.ptit.schedule.entity.Faculty;
import com.ptit.schedule.entity.Major;
import com.ptit.schedule.entity.Subject;
import com.ptit.schedule.repository.FacultyRepository;
import com.ptit.schedule.repository.MajorRepository;
import com.ptit.schedule.repository.SubjectRepository;
import com.ptit.schedule.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SubjectServiceImpl implements SubjectService {
    
    private final SubjectRepository subjectRepository;
    private final MajorRepository majorRepository;
    private final FacultyRepository facultyRepository;
    
    /**
     * Lấy tất cả subjects
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getAllSubjects() {
        return subjectRepository.findAll()
                .stream()
                .map(SubjectResponse::fromEntity)
                .collect(Collectors.toList());
    }
    

    
    /**
     * Lấy subjects theo major ID
     */
    @Override
    @Transactional(readOnly = true)
    public List<SubjectResponse> getSubjectsByMajorId(Integer majorId) {
        return subjectRepository.findByMajorId(majorId)
                .stream()
                .map(SubjectResponse::fromEntity)
                .collect(Collectors.toList());
    }

    /**
     * Tạo subject mới
     */
    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        // Kiểm tra major có tồn tại không, nếu không thì tạo mới
        Major major = getOrCreateMajor(request);

        Optional<Subject> exitsingSubject = subjectRepository.findBySubjectCodeAndMajorCode(request.getSubjectCode(),
                major.getMajorCode());
        if (exitsingSubject.isPresent()) {
//            System.out.println("Subject with id " + request.getSubjectId() + " already exists. Updating instead.");
            return updateSubject(exitsingSubject.get().getId(), request);

        }
        // Tạo subject mới
        Subject subject = Subject.builder()
                .subjectCode(request.getSubjectCode())
                .subjectName(request.getSubjectName())
                .theoryHours(request.getTheoryHours())
                .exerciseHours(request.getExerciseHours())
                .projectHours(request.getProjectHours())
                .labHours(request.getLabHours())
                .selfStudyHours(request.getSelfStudyHours())
                .credits(request.getCredits())
                .department(request.getDepartment())
                .examFormat(request.getExamFormat())
                .numberOfClasses(request.getNumberOfClasses())
                .studentsPerClass(request.getStudentsPerClass())
                .major(major)
                .build();

        Subject savedSubject = subjectRepository.save(subject);
        return SubjectResponse.fromEntity(savedSubject);
    }
    
    /**
     * Lấy major nếu tồn tại, nếu không thì tạo mới
     */
    private Major getOrCreateMajor(SubjectRequest request) {
        // Tìm major theo major code và class year


        Optional<Major> existingMajor = majorRepository.findByMajorCodeAndClassYear(
                request.getMajorId(), 
                request.getClassYear()
        );
        
        if (existingMajor.isPresent()) {
//            System.out.println("Major with id " + request.getMajorId() + " and class year " + request.getClassYear() + " already exists.");
            return existingMajor.get();
        }
        
        // Nếu không tìm thấy major với ID và class year cụ thể, tạo major mới
        if (request.getMajorId() == null || request.getMajorId().trim().isEmpty()) {
            throw new RuntimeException("Major ID is required to create new major");
        }
        
        if (request.getFacultyId() == null || request.getFacultyId().trim().isEmpty()) {
            throw new RuntimeException("Faculty ID is required to create new major");
        }
        
        // Kiểm tra faculty có tồn tại không
        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new RuntimeException("Faculty not found with id: " + request.getFacultyId()));
        
        Major newMajor = Major.builder()
                .majorCode(request.getMajorId())
                .majorName(request.getMajorName())
                .numberOfStudents(request.getNumberOfStudents() != null ? request.getNumberOfStudents() : 50)
                .classYear(request.getClassYear())
                .faculty(faculty)
                .build();
        
        return majorRepository.save(newMajor);
    }
    
    /**
     * Cập nhật subject
     */
    @Override
    public SubjectResponse updateSubject(Long id, SubjectRequest request) {
        Subject subject = subjectRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Subject not found with id: " + id));
        
        // Kiểm tra major có tồn tại không, nếu không thì tạo mới
        Major major = getOrCreateMajor(request);
        
        // Cập nhật thông tin subject
        subject.setSubjectCode(request.getSubjectCode());
        subject.setSubjectName(request.getSubjectName());
        subject.setStudentsPerClass(request.getStudentsPerClass());
        subject.setNumberOfClasses(request.getNumberOfClasses());
        subject.setCredits(request.getCredits());
        subject.setTheoryHours(request.getTheoryHours());
        subject.setExerciseHours(request.getExerciseHours());
        subject.setProjectHours(request.getProjectHours());
        subject.setLabHours(request.getLabHours());
        subject.setSelfStudyHours(request.getSelfStudyHours());
        subject.setDepartment(request.getDepartment());
        subject.setExamFormat(request.getExamFormat());
        subject.setMajor(major);
        subject.setProgramType(request.getProgramType());

        Subject savedSubject = subjectRepository.save(subject);
        return SubjectResponse.fromEntity(savedSubject);
    }
    
    /**
     * Xóa subject
     */
    @Override
    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new RuntimeException("Subject not found with id: " + id);
        }
        subjectRepository.deleteById(id);
    }
}