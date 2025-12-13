package com.ptit.schedule.entity;

public enum OccupancyStatus {
    AVAILABLE("Còn trống"),
    UNAVAILABLE("Không khả dụng"),
    USED("Đã sử dụng");

    private final String displayName;

    OccupancyStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
