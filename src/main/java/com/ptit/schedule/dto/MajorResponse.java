package com.ptit.schedule.dto;

import com.ptit.schedule.entity.Major;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class MajorResponse {
    
    private Long id;
    private String majorCode;
    private String majorName;
    private Integer numberOfStudents;
    private String classYear;
    private String facultyId;
    private String facultyName;
    private List<String> subjectIds;
    private List<String> subjectNames;
    
    public static MajorResponse fromEntity(Major major) {
        MajorResponse response = new MajorResponse();
        response.setId(major.getId());
        response.setMajorCode(major.getMajorCode());
        response.setMajorName(major.getMajorName());
        response.setNumberOfStudents(major.getNumberOfStudents());
        response.setClassYear(major.getClassYear());
        response.setFacultyId(major.getFaculty().getId());
        response.setFacultyName(major.getFaculty().getFacultyName());
        
        if (major.getSubjects() != null) {
            response.setSubjectIds(major.getSubjects().stream()
                    .map(subject -> subject.getSubjectCode())
                    .collect(Collectors.toList()));
            response.setSubjectNames(major.getSubjects().stream()
                    .map(subject -> subject.getSubjectName())
                    .collect(Collectors.toList()));
        }
        
        return response;
    }
}
