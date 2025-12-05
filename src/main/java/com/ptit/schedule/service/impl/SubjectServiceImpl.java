package com.ptit.schedule.service.impl;


import com.ptit.schedule.dto.*;
import com.ptit.schedule.entity.*;
import com.ptit.schedule.exception.InvalidDataException;
import com.ptit.schedule.exception.ResourceNotFoundException;
import com.ptit.schedule.repository.*;
import com.ptit.schedule.service.SubjectService;
import com.ptit.schedule.specification.SubjectSpecification;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class SubjectServiceImpl implements SubjectService {
    
    private final SubjectRepository subjectRepository;
    private final MajorRepository majorRepository;
    private final FacultyRepository facultyRepository;
    private final SemesterRepository semesterRepository;
    
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

    @Override
    public Page<SubjectFullDTO> getSubjects(String search, String semester, String classYear, String majorCode,
                                             String programType, String academicYear, Pageable pageable) {
        log.info("Searching subjects - search: '{}', semester: {}, classYear: {}, majorCode: {}, programType: {}, academicYear: {}",
                search, semester, classYear, majorCode, programType, academicYear);

        // Build specification với tất cả filters
        Specification<Subject> spec = SubjectSpecification.hasSearch(search)
                .and(SubjectSpecification.hasSemester(semester))
                .and(SubjectSpecification.hasClassYear(classYear))
                .and(SubjectSpecification.hasMajor(majorCode))
                .and(SubjectSpecification.hasProgramType(programType))
                .and(SubjectSpecification.hasAcademicYear(academicYear));

        // Execute query với specification và pageable
        Page<Subject> subjects = subjectRepository.findAll(spec, pageable);

        log.info("Found {} subjects matching criteria", subjects.getTotalElements());

        // Convert to DTOs
        return subjects.map(SubjectFullDTO::fromEntity);
    }

    /**
     * Lấy tất cả subjects với phân trang và filter
     */
    @Override
    @Transactional(readOnly = true)
    public Page<SubjectFullDTO> getAllSubjectsWithPaginationAndFilters(
            int page, 
            int size, 
            String sortBy, 
            String sortDir,
            String academicYear,
            String semester,
            String classYear,
            String majorCode,
            String programType) {
        try {
            // Tạo Sort object
            Sort sort = sortDir.equalsIgnoreCase("desc") ? 
                Sort.by(sortBy).descending() : 
                Sort.by(sortBy).ascending();
            
            // Tạo Pageable object
            Pageable pageable = PageRequest.of(page, size, sort);
            
            // Lấy data với pagination và filter
            Page<Subject> subjectPage = subjectRepository.findAllWithFilters(
                academicYear != null && !academicYear.isEmpty() ? academicYear : null,
                semester != null && !semester.trim().isEmpty() ? semester : null,
                classYear != null && !classYear.trim().isEmpty() ? classYear : null,
                majorCode != null && !majorCode.trim().isEmpty() ? majorCode : null,
                programType != null && !programType.trim().isEmpty() ? programType : null,
                pageable
            );
            
            // Convert Page<Subject> sang Page<SubjectFullDTO>
            return subjectPage.map(SubjectFullDTO::fromEntity);
            
        } catch (Exception e) {
            throw new RuntimeException("Lỗi khi lấy danh sách môn học với filter: " + e.getMessage());
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
    public List<SubjectMajorDTO> getSubjectAndMajorCodeByClassYear(
            String semesterName,
            String academicYear,
            String classYear,
            String programType,
            List<String> majorCodes) {
        for (String majorCode : majorCodes) {
            System.out.println("majorCode : " + majorCode);
        }
        List<SubjectMajorDTO> subjectMajorDTOList = subjectRepository.findSubjectsWithMajorInfoByMajorCodes(
            semesterName, academicYear, classYear, programType, majorCodes
        );
        if(programType.equals("Đặc thù")){
            subjectMajorDTOList.addAll(subjectRepository.findSubjectsWithMajorInfoByMajorCodes(
                semesterName, academicYear, classYear, "CLC", majorCodes
            ));
        }
        return subjectMajorDTOList;
    }


    @Override
    public List<Set<String>> groupMajorsBySharedSubjects(
            String semesterName,
            String academicYear,
            String classYear,
            String programType) {
        // Lấy danh sách môn theo semester, academic year, class year và program type
        List<SubjectMajorDTO> subjects = subjectRepository
                .findSubjectsWithMajorInfoByProgramType(semesterName, academicYear, classYear, programType);
        if(programType.equals("Đặc thù")){

            List<SubjectMajorDTO> appendSubjects = subjectRepository
                    .findSubjectsWithMajorInfoByProgramType(semesterName, academicYear, classYear, "CLC");
            subjects.addAll(appendSubjects);
            return separateMajorsByClassYear(subjects);
        }

//        if(programType.equals("Chung")){
//            List<SubjectMajorDTO> appendSubjects = subjectRepository
//                    .findSubjectsWithMajorInfoByProgramType(classYear, "CLC");
//            subjects.addAll(appendSubjects);
//            return separateMajorsByClassYear(subjects);
//        }

//        for(SubjectMajorDTO subjectMajorDTO : subjects) {
//            System.out.println(subjectMajorDTO);
//        }
        // Kiểm tra nếu là năm cuối (khóa 2022) thì trả về separate majors
        if (isLastYear(classYear)) {
            return separateMajorsByClassYear(subjects);
        }

        // Gọi hàm grouping logic bình thường cho các năm khác
        return groupMajorsBySharedSubjects(subjects);
    }

    @Override
    public List<SubjectMajorDTO> getCommonSubjects(String semesterName, String academicYear) {
        List<SubjectMajorDTO> subjectMajorDTOs = subjectRepository.findCommonSubjects(semesterName, academicYear);
        if(subjectMajorDTOs.isEmpty()){
            System.out.println("Không tìm thấy môn học chung nào!");
            return subjectMajorDTOs;
        }
        
        // Group các môn học theo mã môn và tính tổng số sinh viên
        Map<String, SubjectMajorDTO> groupedSubjects = new HashMap<>();
        
        for (SubjectMajorDTO subject : subjectMajorDTOs) {
            String key = subject.getSubjectCode() + "-" + subject.getClassYear();

            if (groupedSubjects.containsKey(key)) {
                // Nếu đã có môn học này, cộng thêm số sinh viên
                SubjectMajorDTO existing = groupedSubjects.get(key);
                int totalStudents = existing.getNumberOfStudents() + subject.getNumberOfStudents();
                
                // Tạo SubjectMajorDTO mới với tổng số sinh viên và majorCode = "Chung"
                SubjectMajorDTO updated = new SubjectMajorDTO(
                    existing.getSubjectCode(),
                    existing.getSubjectName(),
                    "Chung", // Set majorCode thành "Chung"
                    existing.getClassYear(),
                    existing.getTheoryHours(),
                    existing.getExerciseHours(),
                    existing.getLabHours(),
                    existing.getProjectHours(),
                    existing.getSelfStudyHours(),
                    totalStudents, // Tổng số sinh viên
                    existing.getStudentPerClass()
                );
                
                groupedSubjects.put(key, updated);
            } else {
                // Nếu chưa có, thêm mới với majorCode = "Chung"
                SubjectMajorDTO newSubject = new SubjectMajorDTO(
                    subject.getSubjectCode(),
                    subject.getSubjectName(),
                    "Chung", // Set majorCode thành "Chung"
                    subject.getClassYear(),
                    subject.getTheoryHours(),
                    subject.getExerciseHours(),
                    subject.getLabHours(),
                    subject.getProjectHours(),
                    subject.getSelfStudyHours(),
                    subject.getNumberOfStudents(),
                    subject.getStudentPerClass()
                );
                
                groupedSubjects.put(key, newSubject);
            }
        }
        
        return new ArrayList<>(groupedSubjects.values());
    }

    /**
     * Tạo subject mới
     */
    @Override
    @Transactional
    public SubjectResponse createSubject(SubjectRequest request) {
        if (request.getSemesterName() == null || request.getSemesterName().trim().isEmpty()) {
            throw new InvalidDataException("Tên học kỳ không được để trống");
        }

        // Tìm semester theo tên và năm học
        Semester semester = semesterRepository.findBySemesterNameAndAcademicYear(request.getSemesterName(), request.getAcademicYear())
                .orElseThrow(() -> new RuntimeException("Không tìm thấy học kỳ với tên: " + request.getSemesterName()));

        // Kiểm tra major có tồn tại không, nếu không thì tạo mới
        Major major = getOrCreateMajor(request);

        // Kiểm tra xem subject đã tồn tại với các tiêu chí: subjectCode, majorCode, semesterName, academicYear, classYear
        Optional<Subject> existingSubject = subjectRepository.findBySubjectCodeAndMajorCodeAndSemesterAndClassYear(
                request.getSubjectCode(),
                major.getMajorCode(),
                request.getSemesterName(),
                request.getAcademicYear(),
                request.getClassYear()
        );
        
        if (existingSubject.isPresent()) {
            System.out.println("Subject '" + request.getSubjectCode() + 
                "' (Major: " + major.getMajorCode() + 
                ", Semester: " + request.getSemesterName() + 
                ", Year: " + request.getAcademicYear() + 
                ", Class: " + request.getClassYear() + 
                ") already exists. Updating instead.");
            return updateSubject(existingSubject.get().getId(), request);
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
                .semester(semester)
                .isCommon(request.getIsCommon())
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
            throw new InvalidDataException("Mã ngành không được để trống");
        }
        
        if (request.getFacultyId() == null || request.getFacultyId().trim().isEmpty()) {
            throw new InvalidDataException("Mã khoa không được để trống");
        }
        
        // Kiểm tra faculty có tồn tại không
        Faculty faculty = facultyRepository.findById(request.getFacultyId())
                .orElseThrow(() -> new ResourceNotFoundException("Không tìm thấy mã khoa: " + request.getFacultyId()));
        
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
                .orElseThrow(() -> new ResourceNotFoundException("môn học", "mã", id));
//        System.out.println("Updating subject with id " + id);
        // Kiểm tra major có tồn tại không, nếu không thì tạo mới
        Major major = getOrCreateMajor(request);

        // Validate semester
        if (request.getSemesterName() != null && !request.getSemesterName().trim().isEmpty()) {
            Semester semester = semesterRepository.findBySemesterName(request.getSemesterName())
                    .orElseThrow(() -> new ResourceNotFoundException("học kỳ", "tên", request.getSemesterName()));
            subject.setSemester(semester);
        }
        
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
        subject.setIsCommon(request.getIsCommon());
        Subject savedSubject = subjectRepository.save(subject);

        return SubjectResponse.fromEntity(savedSubject);
    }
    
    /**
     * Xóa subject
     */
    @Override
    public void deleteSubject(Long id) {
        if (!subjectRepository.existsById(id)) {
            throw new ResourceNotFoundException("môn học", "mã", id);
        }
        subjectRepository.deleteById(id);
    }

    /**
     * Xóa subjects theo semester (trả về số lượng đã xóa)
     */
    @Override
    @Transactional
    public int deleteSubjectsBySemesterName(String semesterName) {
        if (semesterName == null || semesterName.trim().isEmpty()) {
            throw new InvalidDataException("Tên học kỳ không được để trống");
        }

        List<Subject> subjects = subjectRepository.findBySemesterName(semesterName);
        if (subjects.isEmpty()) {
            throw new ResourceNotFoundException("môn học nào trong học kỳ", "tên", semesterName);
        }
        
        int count = subjects.size();
        subjectRepository.deleteBySemesterName(semesterName);
        return count;
    }

    /**
     * Xóa subjects theo semesterName và academicYear
     */
    @Override
    @Transactional
    public int deleteSubjectsBySemesterNameAndAcademicYear(String semesterName, String academicYear) {
        if (semesterName == null || semesterName.trim().isEmpty()) {
            throw new InvalidDataException("Tên học kỳ không được để trống");
        }
        if (academicYear == null || academicYear.trim().isEmpty()) {
            throw new InvalidDataException("Năm học không được để trống");
        }

        List<Subject> subjects = subjectRepository.findBySemesterNameAndAcademicYear(semesterName, academicYear);
        if (subjects.isEmpty()) {
            throw new ResourceNotFoundException(
                    "môn học nào trong học kỳ năm học", semesterName, academicYear
            );
        }
        
        int count = subjectRepository.deleteBySemesterNameAndAcademicYear(semesterName, academicYear);
        return count;
    }


    /**
     * Kiểm tra xem có phải năm cuối không (khóa 2022)
     */
    private boolean isLastYear(String classYear) {
        // Logic kiểm tra năm cuối - có thể adjust theo business rule
        String finalYear = subjectRepository.findAllDistinctClassYears().get(0);
        return finalYear.equals(classYear);
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

    /**
     * Lấy tất cả program types (distinct)
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllProgramTypes() {
        return subjectRepository.findAllDistinctProgramTypes();
    }

    /**
     * Lấy program types theo semester và academic year
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getProgramTypesBySemesterAndAcademicYear(String semesterName, String academicYear) {
        return subjectRepository.findDistinctProgramTypesBySemesterNameAndAcademicYear(semesterName, academicYear);
    }

    /**
     * Lấy tất cả class years (distinct)
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getAllClassYears() {
        return subjectRepository.findAllDistinctClassYears();
    }

    /**
     * Lấy class years theo semester, academic year và program type
     */
    @Override
    @Transactional(readOnly = true)
    public List<String> getClassYearsBySemesterAndAcademicYearAndProgramType(String semesterName, String academicYear, String programType) {
        return subjectRepository.findDistinctClassYearsBySemesterNameAndAcademicYearAndProgramType(semesterName, academicYear, programType);
    }

}
