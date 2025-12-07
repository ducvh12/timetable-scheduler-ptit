package com.ptit.schedule.dto;

import com.ptit.schedule.entity.RoomType;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomRequest {

    @NotBlank(message = "Số phòng không được để trống")
    @Size(max = 10, message = "Số phòng không được vượt quá 10 ký tự")
    private String name; // Renamed from phong

    @NotNull(message = "Sức chứa không được để trống")
    @Min(value = 1, message = "Sức chứa phải lớn hơn 0")
    @Max(value = 500, message = "Sức chứa không được vượt quá 500")
    private Integer capacity;

    @NotBlank(message = "Tòa nhà không được để trống")
    @Size(max = 10, message = "Tòa nhà không được vượt quá 10 ký tự")
    private String building; // Renamed from day

    @NotNull(message = "Loại phòng không được để trống")
    private RoomType type;

    @Size(max = 1000, message = "Ghi chú không được vượt quá 1000 ký tự")
    private String note;

    // Backward compatibility getters/setters
    public String getPhong() {
        return this.name;
    }

    public void setPhong(String phong) {
        this.name = phong;
    }

    public String getDay() {
        return this.building;
    }

    public void setDay(String day) {
        this.building = day;
    }
}
