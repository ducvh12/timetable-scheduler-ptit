package com.ptit.schedule.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MajorBuildingPreferenceRequest {

    @NotBlank(message = "Ngành không được để trống")
    private String nganh;

    @NotBlank(message = "Tòa nhà không được để trống")
    private String preferredBuilding;

    @NotNull(message = "Mức ưu tiên không được để trống")
    @Min(value = 1, message = "Mức ưu tiên phải từ 1 trở lên")
    private Integer priorityLevel;

    private String notes;
}
