package com.ptit.schedule.repository;

import com.ptit.schedule.entity.Major;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MajorRepository extends JpaRepository<Major, Long> {
    
    // Tìm majors theo faculty ID
    List<Major> findByFacultyId(String facultyId);

    // Tìm major theo major code và khóa học
    Optional<Major> findByMajorCodeAndClassYear(String majorCode, String classYear);
}
