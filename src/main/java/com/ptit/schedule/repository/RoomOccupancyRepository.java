package com.ptit.schedule.repository;

import com.ptit.schedule.entity.RoomOccupancy;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;
import java.util.Set;

@Repository
public interface RoomOccupancyRepository extends JpaRepository<RoomOccupancy, Long> {

        /**
         * Find all room occupancies for a specific semester
         * 
         * @param semesterId Semester ID
         * @return List of room occupancies
         */
        @Query("SELECT ro FROM RoomOccupancy ro WHERE ro.semester.id = :semesterId")
        List<RoomOccupancy> findBySemesterId(@Param("semesterId") Long semesterId);

        /**
         * Find all room occupancies for a specific room and semester
         * 
         * @param roomId     Room ID
         * @param semesterId Semester ID
         * @return List of room occupancies
         */
        @Query("SELECT ro FROM RoomOccupancy ro WHERE ro.room.id = :roomId AND ro.semester.id = :semesterId")
        List<RoomOccupancy> findByRoomIdAndSemesterId(@Param("roomId") Long roomId,
                        @Param("semesterId") Long semesterId);

        /**
         * Find all room occupancies for a specific room (all semesters)
         * 
         * @param roomId Room ID
         * @return List of room occupancies
         */
        @Query("SELECT ro FROM RoomOccupancy ro WHERE ro.room.id = :roomId")
        List<RoomOccupancy> findByRoomId(@Param("roomId") Long roomId);

        /**
         * Find a specific room occupancy by room, semester, day and period
         * 
         * @param roomId     Room ID
         * @param semesterId Semester ID
         * @param dayOfWeek  Day of week (2-7)
         * @param period     Period (1-6)
         * @return Optional RoomOccupancy
         */
        @Query("SELECT ro FROM RoomOccupancy ro WHERE ro.room.id = :roomId " +
                        "AND ro.semester.id = :semesterId AND ro.dayOfWeek = :dayOfWeek AND ro.period = :period")
        Optional<RoomOccupancy> findByRoomIdAndSemesterIdAndDayOfWeekAndPeriod(
                        @Param("roomId") Long roomId,
                        @Param("semesterId") Long semesterId,
                        @Param("dayOfWeek") Integer dayOfWeek,
                        @Param("period") Integer period);

        /**
         * Get all occupied room keys for a specific semester
         * Returns Set<String> with format: "404-A2|5|1" (compatible with old JSON
         * format)
         * 
         * @param semesterId Semester ID
         * @return Set of unique keys
         */
        @Query("SELECT ro.uniqueKey FROM RoomOccupancy ro WHERE ro.semester.id = :semesterId")
        Set<String> findOccupiedKeysBySemesterId(@Param("semesterId") Long semesterId);

        /**
         * Delete all occupancies for a specific semester
         * 
         * @param semesterId Semester ID
         */
        @Modifying
        @Query("DELETE FROM RoomOccupancy ro WHERE ro.semester.id = :semesterId")
        void deleteBySemesterId(@Param("semesterId") Long semesterId);

        /**
         * Check if a specific time slot is occupied for a room in a semester
         * 
         * @param roomId     Room ID
         * @param semesterId Semester ID
         * @param dayOfWeek  Day of week (2-7)
         * @param period     Period (1-6)
         * @return true if occupied, false otherwise
         */
        @Query("SELECT COUNT(ro) > 0 FROM RoomOccupancy ro WHERE ro.room.id = :roomId " +
                        "AND ro.semester.id = :semesterId AND ro.dayOfWeek = :dayOfWeek AND ro.period = :period")
        boolean existsByRoomAndSemesterAndTime(@Param("roomId") Long roomId,
                        @Param("semesterId") Long semesterId,
                        @Param("dayOfWeek") Integer dayOfWeek,
                        @Param("period") Integer period);

        /**
         * Find room occupancy by unique key and semester
         * 
         * @param uniqueKey  Unique key (format: "404-A2|5|1")
         * @param semesterId Semester ID
         * @return RoomOccupancy if found
         */
        @Query("SELECT ro FROM RoomOccupancy ro WHERE ro.uniqueKey = :uniqueKey AND ro.semester.id = :semesterId")
        RoomOccupancy findByUniqueKeyAndSemesterId(@Param("uniqueKey") String uniqueKey,
                        @Param("semesterId") Long semesterId);

        /**
         * Delete occupancies by unique keys for a specific semester
         * Useful for batch deletion
         * 
         * @param uniqueKeys Set of unique keys
         * @param semesterId Semester ID
         */
        @Modifying
        @Query("DELETE FROM RoomOccupancy ro WHERE ro.uniqueKey IN :uniqueKeys AND ro.semester.id = :semesterId")
        void deleteByUniqueKeysAndSemesterId(@Param("uniqueKeys") Set<String> uniqueKeys,
                        @Param("semesterId") Long semesterId);

        /**
         * Find all room occupancies for a specific semester, day and period
         * 
         * @param semesterId Semester ID
         * @param dayOfWeek  Day of week (2-7)
         * @param period     Period (1-6)
         * @return List of room occupancies
         */
        @Query("SELECT ro FROM RoomOccupancy ro WHERE ro.semester.id = :semesterId " +
                        "AND ro.dayOfWeek = :dayOfWeek AND ro.period = :period")
        List<RoomOccupancy> findBySemesterIdAndDayOfWeekAndPeriod(
                        @Param("semesterId") Long semesterId,
                        @Param("dayOfWeek") Integer dayOfWeek,
                        @Param("period") Integer period);

        /**
         * Check if room occupancy exists
         * 
         * @param roomId     Room ID
         * @param semesterId Semester ID
         * @param dayOfWeek  Day of week (2-7)
         * @param period     Period (1-6)
         * @return true if exists
         */
        boolean existsByRoomIdAndSemesterIdAndDayOfWeekAndPeriod(
                        Long roomId, Long semesterId, Integer dayOfWeek, Integer period);
}
