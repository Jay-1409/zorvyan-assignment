package com.zorvyn.user.service.impl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zorvyn.user.entity.Role;
import com.zorvyn.user.entity.User;
import com.zorvyn.user.entity.UserStatus;
import com.zorvyn.user.exception.BadRequestException;
import com.zorvyn.user.repository.UserRepository;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
class UserServiceImplTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private PasswordEncoder passwordEncoder;

    @InjectMocks
    private UserServiceImpl userService;

    @Test
    void updateRole_shouldRejectDemotingLastActiveAdmin() {
        User admin = buildAdmin(1L, "admin@zorvyn.local");
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndStatusAndDeletedFalseAndIdNot(Role.ADMIN, UserStatus.ACTIVE, 1L))
            .thenReturn(0L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.updateRole(1L, Role.ANALYST));

        assertTrue(ex.getMessage().contains("active admin must remain"));
        verify(userRepository, never()).save(admin);
    }

    @Test
    void updateRole_shouldAllowDemotionWhenAnotherAdminExists() {
        User admin = buildAdmin(1L, "admin@zorvyn.local");
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndStatusAndDeletedFalseAndIdNot(Role.ADMIN, UserStatus.ACTIVE, 1L))
            .thenReturn(1L);
        when(userRepository.save(admin)).thenReturn(admin);

        assertEquals(Role.ANALYST, userService.updateRole(1L, Role.ANALYST).role());
        verify(userRepository).save(admin);
    }

    @Test
    void updateStatus_shouldRejectInactivatingLastActiveAdmin() {
        User admin = buildAdmin(1L, "admin@zorvyn.local");
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndStatusAndDeletedFalseAndIdNot(Role.ADMIN, UserStatus.ACTIVE, 1L))
            .thenReturn(0L);

        BadRequestException ex = assertThrows(BadRequestException.class, () -> userService.updateStatus(1L, UserStatus.INACTIVE));

        assertTrue(ex.getMessage().contains("active admin must remain"));
        verify(userRepository, never()).save(admin);
    }

    @Test
    void deleteUser_shouldRejectSelfDeletion() {
        User admin = buildAdmin(1L, "admin@zorvyn.local");
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(admin));

        BadRequestException ex = assertThrows(
            BadRequestException.class,
            () -> userService.deleteUser(1L, "admin@zorvyn.local")
        );

        assertEquals("Admin cannot delete their own account", ex.getMessage());
        verify(userRepository, never()).save(admin);
    }

    @Test
    void deleteUser_shouldRejectDeletingLastActiveAdmin() {
        User admin = buildAdmin(1L, "admin@zorvyn.local");
        when(userRepository.findByIdAndDeletedFalse(1L)).thenReturn(Optional.of(admin));
        when(userRepository.countByRoleAndStatusAndDeletedFalseAndIdNot(Role.ADMIN, UserStatus.ACTIVE, 1L))
            .thenReturn(0L);

        BadRequestException ex = assertThrows(
            BadRequestException.class,
            () -> userService.deleteUser(1L, "other-admin@zorvyn.local")
        );

        assertTrue(ex.getMessage().contains("active admin must remain"));
        verify(userRepository, never()).save(admin);
    }

    private User buildAdmin(Long id, String email) {
        User user = new User();
        user.setId(id);
        user.setUsername("admin");
        user.setEmail(email);
        user.setPasswordHash("hashed");
        user.setRole(Role.ADMIN);
        user.setStatus(UserStatus.ACTIVE);
        user.setDeleted(false);
        return user;
    }
}
