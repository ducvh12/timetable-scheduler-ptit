package com.ptit.schedule.repository;

import com.ptit.schedule.dto.SubjectMajorDTO;
import com.ptit.schedule.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Repository
public interface SubjectRepository extends JpaRepository<Subject, Long> {
    
    // Tìm tất cả subject theo major
    List<Subject> findByMajorId(Integer majorId);

    @Query("SELECT s FROM Subject s WHERE s.subjectCode = :subjectCode AND s.major.majorCode = :majorCode")
    Optional<Subject> findBySubjectCodeAndMajorCode(@Param("subjectCode") String subjectCode,
                                                    @Param("majorCode") String majorCode);

    @Query("""
    SELECT new com.ptit.schedule.dto.SubjectMajorDTO(
        s.subjectCode,
        s.subjectName,
        m.majorCode,
        m.classYear,
        s.theoryHours,
        s.exerciseHours,
        s.labHours,
        s.projectHours,
        s.selfStudyHours,
        m.numberOfStudents,
        s.studentsPerClass
    )
    FROM Subject s
    JOIN s.major m
    """)
    List<SubjectMajorDTO> getAllSubjectsWithMajorInfo();

    // Lấy danh sách subject kèm thông tin ngành
    @Query("""
    SELECT new com.ptit.schedule.dto.SubjectMajorDTO(
        s.subjectCode,
        s.subjectName,
        m.majorCode,
        m.classYear,
        s.theoryHours,
        s.exerciseHours,
        s.labHours,
        s.projectHours,
        s.selfStudyHours,
        m.numberOfStudents,
        s.studentsPerClass
    )
    FROM Subject s
    JOIN s.major m
    WHERE s.semester.semesterName = :semesterName
      AND s.semester.academicYear = :academicYear
      AND m.classYear = :classYear
      AND s.programType = :programType
      AND s.isCommon = false
    """)
    List<SubjectMajorDTO> findSubjectsWithMajorInfoByProgramType(
            @Param("semesterName") String semesterName,
            @Param("academicYear") String academicYear,
            @Param("classYear") String classYear,
            @Param("programType") String programType);


    @Query("""
    SELECT new com.ptit.schedule.dto.SubjectMajorDTO(
        s.subjectCode,
        s.subjectName,
        m.majorCode,
        m.classYear,
        s.theoryHours,
        s.exerciseHours,
        s.labHours,
        s.projectHours,
        s.selfStudyHours,
        m.numberOfStudents,
        s.studentsPerClass
    )
    FROM Subject s
    JOIN s.major m
    WHERE s.semester.semesterName = :semesterName
      AND s.semester.academicYear = :academicYear
      AND m.classYear = :classYear
      AND s.programType = :programType
      AND m.majorCode IN :majorCodes
      AND s.isCommon = false
""")
    List<SubjectMajorDTO> findSubjectsWithMajorInfoByMajorCodes(
            @Param("semesterName") String semesterName,
            @Param("academicYear") String academicYear,
            @Param("classYear") String classYear,
            @Param("programType") String programType,
            @Param("majorCodes") List<String> majorCodes);


    /**
     * Lấy tất cả subjects chung theo semester và academic year
     */
    @Query("""
    SELECT new com.ptit.schedule.dto.SubjectMajorDTO(
         s.subjectCode,
         s.subjectName,
         m.majorCode,
         m.classYear,
         s.theoryHours,
         s.exerciseHours,
         s.labHours,
         s.projectHours,
         s.selfStudyHours,
         CAST(SUM(m.numberOfStudents) AS integer),
         s.studentsPerClass
     )
    FROM Subject s
    JOIN s.major m
    WHERE s.semester.semesterName = :semesterName
      AND s.semester.academicYear = :academicYear
      AND s.isCommon = true
    GROUP BY s.subjectCode, s.subjectName, m.majorCode, m.classYear,
             s.theoryHours, s.exerciseHours, s.labHours,
             s.projectHours, s.selfStudyHours, s.studentsPerClass
""")
    List<SubjectMajorDTO> findCommonSubjects(
        @Param("semesterName") String semesterName,
        @Param("academicYear") String academicYear
    );

    /**
     * Lấy tất cả subjects với pagination
     */
    @Query("""
    SELECT s
    FROM Subject s
    JOIN s.major m
    """)
    Page<Subject> findAllWithMajorAndFaculty(Pageable pageable);

    /**
     * Lấy subjects với pagination và filter
     */
    @Query("""
    SELECT s
    FROM Subject s
    JOIN s.major m
    LEFT JOIN s.semester sem
    WHERE (:academicYear IS NULL OR sem.academicYear = :academicYear)
      AND (:semesterName IS NULL OR sem.semesterName = :semesterName)
      AND (:classYear IS NULL OR m.classYear = :classYear)
      AND (:majorCode IS NULL OR m.majorCode = :majorCode)
      AND (:programType IS NULL OR s.programType = :programType)
    """)
    Page<Subject> findAllWithFilters(
        @Param("academicYear") String academicYear,
        @Param("semesterName") String semesterName,
        @Param("classYear") String classYear,
        @Param("majorCode") String majorCode,
        @Param("programType") String programType,
        Pageable pageable
    );

    /**
     * Tìm tất cả subjects theo semesterName
     */
    @Query("SELECT s FROM Subject s WHERE s.semester.semesterName = :semesterName")
    List<Subject> findBySemesterName(@Param("semesterName") String semesterName);

    /**
     * Tìm tất cả subjects theo semesterName và academicYear
     */
    @Query("SELECT s FROM Subject s WHERE s.semester.semesterName = :semesterName AND s.semester.academicYear = :academicYear")
    List<Subject> findBySemesterNameAndAcademicYear(
        @Param("semesterName") String semesterName, 
        @Param("academicYear") String academicYear
    );

    /**
     * Xóa tất cả subjects theo semesterName
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Subject s WHERE s.semester.semesterName = :semesterName")
    void deleteBySemesterName(@Param("semesterName") String semesterName);

    /**
     * Xóa tất cả subjects theo semesterName và academicYear
     */
    @Transactional
    @Modifying
    @Query("DELETE FROM Subject s WHERE s.semester.semesterName = :semesterName AND s.semester.academicYear = :academicYear")
    int deleteBySemesterNameAndAcademicYear(
        @Param("semesterName") String semesterName, 
        @Param("academicYear") String academicYear
    );

    /**
     * Lấy tất cả program types (distinct)
     */
    @Query("SELECT DISTINCT s.programType FROM Subject s WHERE s.programType IS NOT NULL ORDER BY s.programType")
    List<String> findAllDistinctProgramTypes();

    /**
     * Lấy program types theo semester và academic year
     */
    @Query("SELECT DISTINCT s.programType FROM Subject s WHERE s.semester.semesterName = :semesterName AND s.semester.academicYear = :academicYear AND s.programType IS NOT NULL ORDER BY s.programType")
    List<String> findDistinctProgramTypesBySemesterNameAndAcademicYear(
        @Param("semesterName") String semesterName,
        @Param("academicYear") String academicYear
    );

    /**
     * Lấy tất cả class years (distinct) từ Major
     */
    @Query("SELECT DISTINCT m.classYear FROM Major m WHERE m.classYear IS NOT NULL ORDER BY m.classYear")
    List<String> findAllDistinctClassYears();

    /**
     * Lấy class years theo semester, academic year và program type
     */
    @Query("SELECT DISTINCT m.classYear FROM Subject s JOIN s.major m WHERE s.semester.semesterName = :semesterName AND s.semester.academicYear = :academicYear AND s.programType = :programType AND m.classYear IS NOT NULL ORDER BY m.classYear DESC")
    List<String> findDistinctClassYearsBySemesterNameAndAcademicYearAndProgramType(
        @Param("semesterName") String semesterName,
        @Param("academicYear") String academicYear,
        @Param("programType") String programType
    );


}








