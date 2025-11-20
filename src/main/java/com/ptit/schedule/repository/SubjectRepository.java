package com.ptit.schedule.repository;

import com.ptit.schedule.dto.SubjectMajorDTO;
import com.ptit.schedule.entity.Subject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

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
    WHERE m.classYear = :classYear
      AND s.programType = :programType
      AND s.subjectCode NOT IN (
          'BAS1160',
          'BAS1153',
          'SKD1102',
          'BAS1152',
          'SKD1103',
          'MUL13118',
          'BAS1158',
          'SKD1101',
          'SKD1102'
      )
    """)
    List<SubjectMajorDTO> findSubjectsWithMajorInfoByProgramType(
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
    WHERE m.classYear = :classYear
      AND s.programType = :programType
      AND m.majorCode IN :majorCodes
      AND s.subjectCode NOT IN (
          'BAS1160',
          'BAS1153',
          'SKD1102',
          'BAS1152',
          'SKD1103',
          'MUL13118',
          'BAS1158',
          'SKD1101'
      )
""")
    List<SubjectMajorDTO> findSubjectsWithMajorInfoByMajorCodes(
            @Param("classYear") String classYear,
            @Param("programType") String programType,
            @Param("majorCodes") List<String> majorCodes);



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
    WHERE s.subjectCode IN (
          'BAS1160',
          'BAS1153',
          'SKD1102',
          'BAS1152',
          'SKD1103',
          'MUL13118',
          'BAS1158',
          'SKD1101'
      )
    GROUP BY s.subjectCode, s.subjectName, m.majorCode, m.classYear,
             s.theoryHours, s.exerciseHours, s.labHours,
             s.projectHours, s.selfStudyHours, s.studentsPerClass
""")
    List<SubjectMajorDTO> findCommonSubjects();

    /**
     * Lấy tất cả subjects với pagination
     */
    @Query("""
    SELECT s
    FROM Subject s
    JOIN s.major m
    """)
    Page<Subject> findAllWithMajorAndFaculty(Pageable pageable);


}


