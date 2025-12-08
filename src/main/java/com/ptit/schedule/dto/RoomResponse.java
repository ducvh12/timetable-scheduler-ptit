package com.ptit.schedule.dto;

import com.fasterxml.jackson.annotation.JsonIgnore;
import com.ptit.schedule.entity.RoomStatus;
import com.ptit.schedule.entity.RoomType;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomResponse {

    private Long id;
    private String name; // Renamed from phong
    private Integer capacity;
    private String building; // Renamed from day
    private RoomType type;
    private String typeDisplayName;
    private RoomStatus status;
    private String statusDisplayName;
    private String note;

    // Backward compatibility getters/setters - Hidden from JSON output
    @JsonIgnore
    public String getPhong() {
        return this.name;
    }

    public void setPhong(String phong) {
        this.name = phong;
    }

    @JsonIgnore
    public String getDay() {
        return this.building;
    }

    public void setDay(String day) {
        this.building = day;
    }
}
