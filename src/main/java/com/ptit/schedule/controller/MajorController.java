package com.ptit.schedule.controller;

import com.ptit.schedule.dto.ApiResponse;
import com.ptit.schedule.dto.MajorResponse;
import com.ptit.schedule.service.MajorService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("api/majors")
@RequiredArgsConstructor
@Tag(name = "Major Management", description = "API quản lý ngành học")
public class MajorController {
    private final MajorService majorService;

    @Operation(summary = "Lấy tất cả ngành học", description = "Trả về danh sách tất cả các ngành học")
    @io.swagger.v3.oas.annotations.responses.ApiResponse(responseCode = "200", description = "Thành công")
    @GetMapping
    public ResponseEntity<ApiResponse<List<MajorResponse>>> getAllMajors() {
        List<MajorResponse> majors = majorService.getAllMajors();
        ApiResponse<List<MajorResponse>> response = ApiResponse.success(majors, "Lấy danh sách ngành thành công");
        return ResponseEntity.ok(response);
    }
}
