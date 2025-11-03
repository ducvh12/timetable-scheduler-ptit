package com.ptit.schedule.controller;

import com.ptit.schedule.dto.ApiResponse;
import com.ptit.schedule.dto.MajorBuildingPreferenceRequest;
import com.ptit.schedule.entity.MajorBuildingPreference;
import com.ptit.schedule.service.MajorBuildingPreferenceService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/major-building-preferences")
@RequiredArgsConstructor

public class MajorBuildingPreferenceController {

    private final MajorBuildingPreferenceService preferenceService;

    @GetMapping
    public ResponseEntity<ApiResponse<List<MajorBuildingPreference>>> getAllPreferences() {
        List<MajorBuildingPreference> prefs = preferenceService.getAllActivePreferences();
        return ResponseEntity.ok(ApiResponse.<List<MajorBuildingPreference>>builder()
                .success(true)
                .message("Lấy danh sách ưu tiên thành công")
                .data(prefs)
                .build());
    }

    @GetMapping("/major/{nganh}")
    public ResponseEntity<ApiResponse<List<String>>> getPreferredBuildingsForMajor(
            @PathVariable String nganh) {
        List<String> buildings = preferenceService.getPreferredBuildingsForMajor(nganh);
        return ResponseEntity.ok(ApiResponse.<List<String>>builder()
                .success(true)
                .message("Lấy danh sách tòa ưu tiên thành công")
                .data(buildings)
                .build());
    }

    @PostMapping
    public ResponseEntity<ApiResponse<MajorBuildingPreference>> createOrUpdatePreference(
            @Valid @RequestBody MajorBuildingPreferenceRequest request) {
        MajorBuildingPreference pref = preferenceService.createOrUpdatePreference(
                request.getNganh(),
                request.getPreferredBuilding(),
                request.getPriorityLevel(),
                request.getNotes());
        return ResponseEntity.ok(ApiResponse.<MajorBuildingPreference>builder()
                .success(true)
                .message("Lưu ưu tiên thành công")
                .data(pref)
                .build());
    }

    @DeleteMapping("/major/{nganh}/building/{building}")
    public ResponseEntity<ApiResponse<Void>> deactivatePreference(
            @PathVariable String nganh,
            @PathVariable String building) {
        preferenceService.deactivatePreference(nganh, building);
        return ResponseEntity.ok(ApiResponse.<Void>builder()
                .success(true)
                .message("Vô hiệu hóa ưu tiên thành công")
                .build());
    }

    @PostMapping("/bulk")
    public ResponseEntity<ApiResponse<String>> bulkCreatePreferences(
            @Valid @RequestBody List<MajorBuildingPreferenceRequest> requests) {
        for (MajorBuildingPreferenceRequest req : requests) {
            preferenceService.createOrUpdatePreference(
                    req.getNganh(),
                    req.getPreferredBuilding(),
                    req.getPriorityLevel(),
                    req.getNotes());
        }
        return ResponseEntity.ok(ApiResponse.<String>builder()
                .success(true)
                .message("Import ưu tiên thành công")
                .data("Đã import " + requests.size() + " ưu tiên")
                .build());
    }
}
