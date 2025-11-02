package com.ptit.schedule.config;

import com.ptit.schedule.repository.FacultyRepository;
import com.ptit.schedule.repository.RoomRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.stereotype.Component;

import javax.sql.DataSource;
import java.sql.Connection;

@Slf4j
@Component
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {
    
    private final DataSource dataSource;
    private final FacultyRepository facultyRepository;
    private final RoomRepository roomRepository;
    
    @Override
    public void run(String... args) {
        try {
            // Chỉ chạy migration nếu database chưa có data
            if (isDatabaseEmpty()) {
                log.info("Database is empty. Starting data initialization from data.sql...");
                
                Connection connection = dataSource.getConnection();
                ClassPathResource resource = new ClassPathResource("data.sql");
                
                if (resource.exists()) {
                    ScriptUtils.executeSqlScript(connection, resource);
                    log.info("Data initialization completed successfully.");
                    log.info("Faculties count: {}", facultyRepository.count());
                    log.info("Rooms count: {}", roomRepository.count());
                } else {
                    log.warn("data.sql file not found, skipping data initialization.");
                }
                
                connection.close();
            } else {
                log.info("Database already contains data. Skipping data initialization.");
                log.info("Current faculties count: {}", facultyRepository.count());
                log.info("Current rooms count: {}", roomRepository.count());
            }
            
        } catch (Exception e) {
            log.error("Error during data initialization: {}", e.getMessage(), e);
            // Không throw exception để app vẫn start được
        }
    }
    
    /**
     * Kiểm tra xem database có rỗng không
     */
    private boolean isDatabaseEmpty() {
        return facultyRepository.count() == 0 && roomRepository.count() == 0;
    }
    
    /**
     * Force reload data (có thể gọi từ endpoint admin nếu cần)
     */
    public void forceReloadData() {
        try {
            log.info("Force reloading data from data.sql...");
            
            Connection connection = dataSource.getConnection();
            ClassPathResource resource = new ClassPathResource("data.sql");
            
            if (resource.exists()) {
                ScriptUtils.executeSqlScript(connection, resource);
                log.info("Force data reload completed successfully.");
            }
            
            connection.close();
            
        } catch (Exception e) {
            log.error("Error during force data reload: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to reload data", e);
        }
    }
}