package com.ptit.schedule.config;

import com.ptit.schedule.entity.Role;
import com.ptit.schedule.entity.User;
import com.ptit.schedule.repository.FacultyRepository;
import com.ptit.schedule.repository.RoomRepository;
import com.ptit.schedule.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.core.io.ClassPathResource;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.security.crypto.password.PasswordEncoder;
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
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;
    
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
            
            // Initialize default users (always check)
           initializeDefaultUsers();
            
        } catch (Exception e) {
            log.error("Error during data initialization: {}", e.getMessage(), e);
            // Không throw exception để app vẫn start được
        }
    }
    
    /**
     * Khởi tạo users mặc định
     */
    private void initializeDefaultUsers() {
        // Create default admin user if not exists
        if (!userRepository.existsByUsername("admin")) {
            User admin = User.builder()
                    .username("admin")
                    .email("admin@ptit.edu.vn")
                    .password(passwordEncoder.encode("admin123"))
                    .fullName("Administrator")
                    .role(Role.ADMIN)
                    .enabled(true)
                    .build();
            
            userRepository.save(admin);
            log.info("Default admin user created: username=admin, password=admin123");
        } else {
            log.info("Admin user already exists, skipping creation.");
        }
        
        // Create default regular user if not exists
        if (!userRepository.existsByUsername("user")) {
            User user = User.builder()
                    .username("user")
                    .email("user@ptit.edu.vn")
                    .password(passwordEncoder.encode("user123"))
                    .fullName("Regular User")
                    .role(Role.USER)
                    .enabled(true)
                    .build();
            
            userRepository.save(user);
            log.info("Default user created: username=user, password=user123");
        } else {
            log.info("Regular user already exists, skipping creation.");
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