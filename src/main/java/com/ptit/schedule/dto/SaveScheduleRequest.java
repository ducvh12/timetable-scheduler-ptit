package com.ptit.schedule.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

/**
 * DTO for saving schedule with template and subject reference
 */
@Data
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class SaveScheduleRequest {
    @JsonProperty("subject_id")
    private Long subjectId; // ID của Subject trong database
    
    @JsonProperty("class_number")
    private Integer classNumber; // Lớp thứ mấy
    
    @JsonProperty("student_year")
    private String studentYear; // Khóa
    
    @JsonProperty("major")
    private String major; // Mã ngành
    
    @JsonProperty("special_system")
    private String specialSystem; // Hệ đặc thù
    
    @JsonProperty("si_so_mot_lop")
    private Integer siSoMotLop; // Sĩ số một lớp
    
    @JsonProperty("room_number")
    private String roomNumber; // Mã phòng
    
    @JsonProperty("template_database_id")
    private Long templateDatabaseId; // ID của TKBTemplate trong database
}
