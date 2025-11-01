package com.ptit.schedule.service.impl;

import com.ptit.schedule.dto.*;
import com.ptit.schedule.entity.*;
import com.ptit.schedule.repository.*;
import com.ptit.schedule.service.SubjectService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
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
    public List<SubjectMajorDTO> getAllSubjects() {
        return subjectRepository.getAllSubjectsWithMajorInfo();
    }

    /**
     * Lấy tất cả subjects với phân trang
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SubjectFullDTO> getAllSubjectsWithPagination(int page, int size, String sortBy, String sortDir) {
        try {
            // Tạo Sort object
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
            
            // Tạo Pageable object
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Lấy data với pagination
            Page<Subject> subjectPage = subjectRepository.findAllWithMajorAndFaculty(pageable);
            
            // Convert Page<Subject> sang Page<SubjectFullDTO>
            Page<SubjectFullDTO> subjectFullDTOPage = subjectPage.map(SubjectFullDTO::fromEntity);
            
            return subjectFullDTOPage;
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách môn học với phân trang: " + e.getMessage());
        }
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

    @Override
    public List<SubjectMajorDTO> getSubjectAndMajorCodeByClassYear(String classYear, String programType, List<String> majorCodes) {
        return subjectRepository.findSubjectsWithMajorInfoByMajorCodes(classYear, programType, majorCodes);
    }

    @Override
    public List<Set<String>> groupMajorsBySharedSubjects(String classYear, String programType) {
        // Lấy danh sách môn theo năm học (lọc chính quy, loại môn chung)
        List<SubjectMajorDTO> subjects = subjectRepository
                .findSubjectsWithMajorInfoByProgramType(classYear, programType);
        if(programType.equals("Đặc thù")){
            List<SubjectMajorDTO> appendSubjects = subjectRepository
                    .findSubjectsWithMajorInfoByProgramType(classYear, "CLC");
            subjects.addAll(appendSubjects);
        }
        for(SubjectMajorDTO subjectMajorDTO : subjects) {
            System.out.println(subjectMajorDTO);
        }
        // Kiểm tra nếu là năm cuối (khóa 2022) thì trả về separate majors
        if (isLastYear(classYear)) {
            return separateMajorsByClassYear(subjects);
        }

        // Gọi hàm grouping logic bình thường cho các năm khác
        return groupMajorsBySharedSubjects(subjects);
    }

    @Override
    public List<SubjectMajorDTO> getCommonSubjects() {
        return subjectRepository.findCommonSubjects();
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
                .subjectCode(request.getSubjectCode().trim())
                .subjectName(request.getSubjectName().trim())
                .theoryHours(request.getTheoryHours())
                .exerciseHours(request.getExerciseHours())
                .projectHours(request.getProjectHours())
                .labHours(request.getLabHours())
                .selfStudyHours(request.getSelfStudyHours())
                .credits(request.getCredits())
                .department(request.getDepartment().trim())
                .examFormat(request.getExamFormat().trim())
                .numberOfClasses(request.getNumberOfClasses())
                .studentsPerClass(request.getStudentsPerClass())
                .programType(request.getProgramType().trim())
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
        System.out.println("Updating subject with id " + id);
        // Kiểm tra major có tồn tại không, nếu không thì tạo mới
        Major major = getOrCreateMajor(request);
        
        // Cập nhật thông tin subject
        subject.setSubjectCode(request.getSubjectCode().trim());
        subject.setSubjectName(request.getSubjectName().trim());
        subject.setStudentsPerClass(request.getStudentsPerClass());
        subject.setNumberOfClasses(request.getNumberOfClasses());
        subject.setCredits(request.getCredits());
        subject.setTheoryHours(request.getTheoryHours());
        subject.setExerciseHours(request.getExerciseHours());
        subject.setProjectHours(request.getProjectHours());
        subject.setLabHours(request.getLabHours());
        subject.setSelfStudyHours(request.getSelfStudyHours());
        subject.setDepartment(request.getDepartment().trim());
        subject.setExamFormat(request.getExamFormat().trim());
        subject.setMajor(major);
        subject.setProgramType(request.getProgramType().trim());
        subject.setSemester(request.getSemester());

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


    /**
     * Kiểm tra xem có phải năm cuối không (khóa 2022)
     */
    private boolean isLastYear(String classYear) {
        // Logic kiểm tra năm cuối - có thể adjust theo business rule
        return "2022".equals(classYear);
    }

    /**
     * Trả về các major riêng biệt cho năm cuối, không group lại
     */
    private List<Set<String>> separateMajorsByClassYear(List<SubjectMajorDTO> subjects) {
        // Lấy tất cả major codes unique
        Set<String> allMajors = subjects.stream()
                .map(SubjectMajorDTO::getMajorCode)
                .collect(Collectors.toSet());

        // Trả về mỗi major như một group riêng biệt
        return allMajors.stream()
                .map(major -> {
                    Set<String> singleMajorGroup = new HashSet<>();
                    singleMajorGroup.add(major);
                    return singleMajorGroup;
                })
                .collect(Collectors.toList());
    }

    public static List<Set<String>> groupMajorsBySharedSubjects(List<SubjectMajorDTO> list) {
        // subjectCode -> list majorCode học môn đó
        Map<String, List<String>> subjectToMajors = new HashMap<>();
        Set<String> allMajors = new HashSet<>(); // lưu tất cả các major có trong danh sách

        for (SubjectMajorDTO sm : list) {
            subjectToMajors
                    .computeIfAbsent(sm.getSubjectCode(), k -> new ArrayList<>())
                    .add(sm.getMajorCode());
            allMajors.add(sm.getMajorCode());
        }

        // Xây graph: major -> majors học chung
        Map<String, Set<String>> graph = new HashMap<>();
        for (List<String> majors : subjectToMajors.values()) {
            for (String m1 : majors) {
                graph.computeIfAbsent(m1, k -> new HashSet<>()); // đảm bảo có node kể cả khi không có cạnh
                for (String m2 : majors) {
                    if (!m1.equals(m2))
                        graph.get(m1).add(m2);
                }
            }
        }

        // Đảm bảo tất cả các major đều có mặt trong graph (ngành học 1 mình)
        for (String major : allMajors) {
            graph.computeIfAbsent(major, k -> new HashSet<>());
        }

        // Duyệt DFS để tìm nhóm liên thông
        Set<String> visited = new HashSet<>();
        List<Set<String>> groups = new ArrayList<>();

        for (String major : graph.keySet()) {
            if (!visited.contains(major)) {
                Set<String> component = new HashSet<>();
                dfs(major, graph, visited, component);
                groups.add(component);
            }
        }

        // Trả trực tiếp danh sách nhóm
        return groups;
    }

    private static void dfs(String current, Map<String, Set<String>> graph,
                            Set<String> visited, Set<String> component) {
        visited.add(current);
        component.add(current);
        for (String neighbor : graph.getOrDefault(current, Set.of())) {
            if (!visited.contains(neighbor)) {
                dfs(neighbor, graph, visited, component);
            }
        }
    }

}
