package com.zorvyn.user.config;

import com.zorvyn.user.entity.Role;
import com.zorvyn.user.entity.User;
import com.zorvyn.user.entity.UserStatus;
import com.zorvyn.user.repository.UserRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Component
@Profile("!test")
public class DataSeeder implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public DataSeeder(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    @Transactional
    public void run(String... args) {
        if (userRepository.count() > 0) {
            return;
        }

        createSeedUser("admin", "admin@zorvyn.local", "Admin@123", Role.ADMIN);
        createSeedUser("analyst", "analyst@zorvyn.local", "Analyst@123", Role.ANALYST);
        createSeedUser("viewer", "viewer@zorvyn.local", "Viewer@123", Role.VIEWER);
    }

    private void createSeedUser(String username, String email, String rawPassword, Role role) {
        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(rawPassword));
        user.setRole(role);
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);
        userRepository.save(user);
    }
}
