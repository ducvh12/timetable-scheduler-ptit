package com.ptit.schedule.service;

import com.ptit.schedule.dto.RoomRequest;
import com.ptit.schedule.dto.RoomResponse;
import com.ptit.schedule.dto.RoomStatusUpdateRequest;
import com.ptit.schedule.dto.RoomBulkStatusUpdateRequest;
import com.ptit.schedule.dto.TKBBatchResponse;
import com.ptit.schedule.entity.Room;
import com.ptit.schedule.entity.RoomStatus;
import com.ptit.schedule.entity.RoomType;
import com.ptit.schedule.dto.RoomPickResult;

import java.util.List;
import java.util.Set;

public interface RoomService {

        // CRUD operations
        List<RoomResponse> getAllRooms();

        RoomResponse getRoomById(Long id);

        RoomResponse createRoom(RoomRequest roomRequest);

        RoomResponse updateRoom(Long id, RoomRequest roomRequest);

        void deleteRoom(Long id);

        // Status management
        RoomResponse updateRoomStatus(Long id, RoomStatusUpdateRequest statusRequest);

        List<RoomResponse> bulkUpdateRoomStatus(RoomBulkStatusUpdateRequest request);

        // Query methods
        List<RoomResponse> getRoomsByBuilding(String day);

        List<RoomResponse> getRoomsByStatus(RoomStatus status);

        List<RoomResponse> getRoomsByType(RoomType type);

        List<RoomResponse> getAvailableRoomsWithCapacity(Integer requiredCapacity);

        List<RoomResponse> getRoomsByBuildingAndStatus(String day, RoomStatus status);

        List<RoomResponse> getRoomsByTypeAndStatus(RoomType type, RoomStatus status);

        // Methods for TimetableSchedulingService
        RoomPickResult pickRoom(List<Room> rooms, Integer sisoPerClass, Set<Object> occupied,
                        Integer thu, Integer kip, String subjectType, String studentYear,
                        String heDacThu, List<String> weekSchedule, String nganh, String maMon);

        // Assign rooms to existing TKB
        TKBBatchResponse assignRoomsToSchedule(
                        TKBBatchResponse existingSchedule,
                        String academicYear,
                        String semester);
}
