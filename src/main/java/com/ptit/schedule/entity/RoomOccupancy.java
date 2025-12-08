package com.ptit.schedule.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "room_occupancies", uniqueConstraints = @UniqueConstraint(columnNames = { "room_id", "semester_id",
        "day_of_week", "period" }, name = "uk_room_semester_time"), indexes = {
                @Index(name = "idx_semester_id", columnList = "semester_id"),
                @Index(name = "idx_room_id", columnList = "room_id"),
                @Index(name = "idx_unique_key", columnList = "unique_key")
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RoomOccupancy {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "room_id", nullable = false)
    @NotNull(message = "Phòng không được để trống")
    private Room room;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "semester_id", nullable = false)
    @NotNull(message = "Học kỳ không được để trống")
    private Semester semester;

    @Column(name = "day_of_week", nullable = false)
    @NotNull(message = "Thứ không được để trống")
    @Min(value = 2, message = "Thứ phải từ 2 đến 7")
    @Max(value = 7, message = "Thứ phải từ 2 đến 7")
    private Integer dayOfWeek; // 2-7 (Thứ 2 đến Thứ 7)

    @Column(name = "period", nullable = false)
    @NotNull(message = "Tiết học không được để trống")
    @Min(value = 1, message = "Tiết học phải từ 1 đến 6")
    @Max(value = 6, message = "Tiết học phải từ 1 đến 6")
    private Integer period; // 1-6

    @Column(name = "unique_key", nullable = false, length = 50)
    @NotBlank(message = "Unique key không được để trống")
    @Size(max = 50, message = "Unique key không được vượt quá 50 ký tự")
    private String uniqueKey; // Format: "404-A2|5|1" or "404|5|1"

    @Column(name = "note", length = 500)
    @Size(max = 500, message = "Ghi chú không được vượt quá 500 ký tự")
    private String note; // Optional note about this occupation
}
