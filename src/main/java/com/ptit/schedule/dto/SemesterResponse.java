package com.ptit.schedule.dto;

import com.ptit.schedule.entity.Semester;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SemesterResponse {
    private Long id;
    private String semesterName;
    private String academicYear;
    private LocalDate startDate;
    private LocalDate endDate;
    private Boolean isActive;
    private String description;
    private Integer subjectCount;  // Số lượng môn học trong học kỳ

    public static SemesterResponse fromEntity(Semester semester) {
        return SemesterResponse.builder()
                .id(semester.getId())
                .semesterName(semester.getSemesterName())
                .academicYear(semester.getAcademicYear())
                .startDate(semester.getStartDate())
                .endDate(semester.getEndDate())
                .isActive(semester.getIsActive())
                .description(semester.getDescription())
                .subjectCount(semester.getSubjects() != null ? semester.getSubjects().size() : 0)
                .build();
    }
}
