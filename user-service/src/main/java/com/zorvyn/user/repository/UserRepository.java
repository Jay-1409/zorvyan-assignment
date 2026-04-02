package com.zorvyn.user.repository;

import com.zorvyn.user.entity.Role;
import com.zorvyn.user.entity.User;
import com.zorvyn.user.entity.UserStatus;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

public interface UserRepository
    extends JpaRepository<User, Long>, JpaSpecificationExecutor<User>
{
    Optional<User> findByEmailIgnoreCaseAndDeletedFalse(String email);

    Optional<User> findByIdAndDeletedFalse(Long id);

    boolean existsByEmailIgnoreCaseAndDeletedFalse(String email);

    boolean existsByUsernameIgnoreCaseAndDeletedFalse(String username);

    boolean existsByEmailIgnoreCaseAndDeletedFalseAndIdNot(
        String email,
        Long id
    );

    boolean existsByUsernameIgnoreCaseAndDeletedFalseAndIdNot(
        String username,
        Long id
    );

    long countByRoleAndStatusAndDeletedFalse(Role role, UserStatus status);

    long countByRoleAndStatusAndDeletedFalseAndIdNot(
        Role role,
        UserStatus status,
        Long id
    );
}
