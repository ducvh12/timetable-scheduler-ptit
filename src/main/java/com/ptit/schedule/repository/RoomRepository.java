package com.ptit.schedule.repository;

import com.ptit.schedule.entity.Room;
import com.ptit.schedule.entity.RoomStatus;
import com.ptit.schedule.entity.RoomType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface RoomRepository extends JpaRepository<Room, Long> {

    // Tìm phòng theo tòa nhà
    @Query("SELECT r FROM Room r WHERE r.building = :building")
    List<Room> findByBuilding(@Param("building") String building);

    // Tìm phòng theo tòa nhà (backward compatible method name)
    @Query("SELECT r FROM Room r WHERE r.building = :day")
    List<Room> findByDay(@Param("day") String day);

    // Tìm phòng theo loại
    List<Room> findByType(RoomType type);

    // Tìm phòng theo trạng thái
    List<Room> findByStatus(RoomStatus status);

    // Tìm phòng theo tòa nhà và trạng thái
    @Query("SELECT r FROM Room r WHERE r.building = :building AND r.status = :status")
    List<Room> findByBuildingAndStatus(@Param("building") String building, @Param("status") RoomStatus status);

    // Tìm phòng theo tòa nhà và trạng thái (backward compatible method name)
    @Query("SELECT r FROM Room r WHERE r.building = :day AND r.status = :status")
    List<Room> findByDayAndStatus(@Param("day") String day, @Param("status") RoomStatus status);

    // Tìm phòng theo loại và trạng thái
    List<Room> findByTypeAndStatus(RoomType type, RoomStatus status);

    // Tìm phòng theo name (room number)
    Optional<Room> findByName(String name);

    // Tìm phòng theo sức chứa tối thiểu
    List<Room> findByCapacityGreaterThanEqual(Integer minCapacity);

    // Tìm phòng trống có sức chứa đủ cho số sinh viên
    @Query("SELECT r FROM Room r WHERE r.status = 'AVAILABLE' AND r.capacity >= :requiredCapacity ORDER BY r.capacity ASC")
    List<Room> findAvailableRoomsWithCapacity(@Param("requiredCapacity") Integer requiredCapacity);

    // Tìm phòng theo tòa nhà và sức chứa
    @Query("SELECT r FROM Room r WHERE r.building = :building AND r.capacity >= :minCapacity")
    List<Room> findByBuildingAndCapacityGreaterThanEqual(@Param("building") String building,
            @Param("minCapacity") Integer minCapacity);

    // Tìm phòng theo tòa nhà và sức chứa (backward compatible method name)
    @Query("SELECT r FROM Room r WHERE r.building = :day AND r.capacity >= :minCapacity")
    List<Room> findByDayAndCapacityGreaterThanEqual(@Param("day") String day,
            @Param("minCapacity") Integer minCapacity);

    // Tìm phòng theo số phòng và tòa nhà
    @Query("SELECT r FROM Room r WHERE r.name = :name AND r.building = :building")
    Optional<Room> findByNameAndBuilding(@Param("name") String name, @Param("building") String building);

    // Tìm phòng theo số phòng và tòa nhà (backward compatible method name)
    @Query("SELECT r FROM Room r WHERE r.name = :phong AND r.building = :day")
    Optional<Room> findByPhongAndDay(@Param("phong") String phong, @Param("day") String day);
}
