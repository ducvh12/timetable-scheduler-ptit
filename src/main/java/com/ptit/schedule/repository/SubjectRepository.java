package com.ptit.schedule.repository;

import com.ptit.schedule.entity.Subject;
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
}
