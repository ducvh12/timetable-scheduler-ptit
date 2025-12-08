package com.ptit.schedule.entity;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Entity
@Table(name = "tkb_templates",
        indexes = {
                @Index(name = "idx_semester_id", columnList = "semester_id"),
                @Index(name = "idx_row_order", columnList = "row_order"),
                @Index(name = "idx_total_periods", columnList = "total_periods")
        },
        uniqueConstraints = {
                @UniqueConstraint(name = "uk_template_semester",
                        columnNames = {"template_id", "semester_id"})
        })
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class TKBTemplate {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "template_id", nullable = false, length = 50)
    private String templateId;

    @Column(name = "total_periods", nullable = false)
    private Integer totalPeriods;

    @Column(name = "day_of_week")
    private Integer dayOfWeek;

    @Column(nullable = false)
    private Integer kip;

    @Column(name = "start_period", nullable = false)
    private Integer startPeriod;

    @Column(name = "period_length", nullable = false)
    private Integer periodLength;

    @Column(name = "week_schedule", columnDefinition = "JSON", nullable = false)
    private String weekSchedule; // JSON string của List<Integer> (18 phần tử)

    @Column(name = "total_used")
    private Integer totalUsed = 0;

    @com.fasterxml.jackson.annotation.JsonIgnore // Hoàn toàn bỏ qua semester khi serialize
    @ManyToOne(fetch = FetchType.EAGER)
    @JoinColumn(name = "semester_id", nullable = false)
    private Semester semester; // Học kỳ và năm học

    @Column(name = "row_order", nullable = false)
    private Integer rowOrder; // Thứ tự từ Excel (0-58 cho 59 templates)
}
