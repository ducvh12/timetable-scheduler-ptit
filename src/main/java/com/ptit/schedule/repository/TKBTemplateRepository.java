package com.ptit.schedule.repository;

import com.ptit.schedule.entity.Semester;
import com.ptit.schedule.entity.TKBTemplate;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface TKBTemplateRepository extends JpaRepository<TKBTemplate, Long> {

    /**
     * Tìm tất cả templates theo semester, sắp xếp theo rowOrder
     */
    List<TKBTemplate> findBySemesterOrderByRowOrderAsc(Semester semester);

    /**
     * Tìm templates theo số tiết và semester, sắp xếp theo rowOrder
     */
    List<TKBTemplate> findByTotalPeriodsAndSemesterOrderByRowOrderAsc(Integer totalPeriods, Semester semester);

    /**
     * Xóa tất cả templates của một semester
     */
    void deleteBySemester(Semester semester);
}
