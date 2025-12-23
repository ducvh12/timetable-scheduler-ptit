package com.ptit.schedule.dto;

import com.ptit.schedule.entity.Subject;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SubjectFullDTO {
    
    private Long id;
    private String subjectCode;
    private String subjectName;
    private Integer credits;
    private Integer theoryHours;
    private Integer exerciseHours;
    private Integer projectHours;
    private Integer labHours;
    private Integer selfStudyHours;
    private String examFormat;
    private String classYear;
    private String programType;
    private Integer numberOfStudents;
    private Integer numberOfClasses;
    private String department;
    private Integer studentsPerClass;
    private String semesterName;
    private String academicYear;
    
    // Major information
    private Long majorId;
    private String majorCode;
    private String majorName;
    
    // Faculty information
    private String facultyId;
    private String facultyName;
    
    /**
     * Convert từ Subject entity sang SubjectFullDTO
     */
    public static SubjectFullDTO fromEntity(Subject subject) {
        SubjectFullDTOBuilder builder = SubjectFullDTO.builder()
                .id(subject.getId())
                .subjectCode(subject.getSubjectCode())
                .subjectName(subject.getSubjectName())
                .credits(subject.getCredits())
                .theoryHours(subject.getTheoryHours())
                .exerciseHours(subject.getExerciseHours())
                .projectHours(subject.getProjectHours())
                .labHours(subject.getLabHours())
                .selfStudyHours(subject.getSelfStudyHours())
                .examFormat(subject.getExamFormat())
                .classYear(subject.getMajor().getClassYear())
                .programType(subject.getProgramType())
                .numberOfStudents(subject.getMajor().getNumberOfStudents())
                .numberOfClasses(subject.getNumberOfClasses())
                .department(subject.getDepartment())

                .studentsPerClass(subject.getStudentsPerClass());

        // Set semester name if exists
        if (subject.getSemester() != null) {
            builder.academicYear(subject.getSemester().getAcademicYear());
            builder.semesterName(subject.getSemester().getSemesterName());
        }

        // Set major information if exists
        if (subject.getMajor() != null) {
            builder.majorId(subject.getMajor().getId())
                   .majorCode(subject.getMajor().getMajorCode())
                   .majorName(subject.getMajor().getMajorName());

            // Set faculty information if exists
            if (subject.getMajor().getFaculty() != null) {
                builder.facultyId(subject.getMajor().getFaculty().getId())
                       .facultyName(subject.getMajor().getFaculty().getFacultyName());
            }
        }

        return builder.build();
    }

}