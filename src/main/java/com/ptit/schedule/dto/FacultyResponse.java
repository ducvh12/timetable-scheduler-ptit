package com.ptit.schedule.dto;

import com.ptit.schedule.entity.Faculty;
import lombok.Data;

import java.util.List;
import java.util.stream.Collectors;

@Data
public class FacultyResponse {
    
    private String id;
    private String facultyName;
    private List<Integer> majorIds;
    private List<String> majorNames;
    
    public static FacultyResponse fromEntity(Faculty faculty) {
        FacultyResponse response = new FacultyResponse();
        response.setId(faculty.getId());
        response.setFacultyName(faculty.getFacultyName());
        return response;
    }
}
