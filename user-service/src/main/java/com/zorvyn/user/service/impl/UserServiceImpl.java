package com.zorvyn.user.service.impl;

import com.zorvyn.user.dto.request.CreateUserRequest;
import com.zorvyn.user.dto.request.UpdateUserRequest;
import com.zorvyn.common.dto.PagedResponse;
import com.zorvyn.user.dto.response.UserResponse;
import com.zorvyn.user.entity.Role;
import com.zorvyn.user.entity.User;
import com.zorvyn.user.entity.UserStatus;
import com.zorvyn.user.exception.BadRequestException;
import com.zorvyn.user.exception.ConflictException;
import com.zorvyn.user.exception.ResourceNotFoundException;
import com.zorvyn.user.mapper.UserMapper;
import com.zorvyn.user.repository.UserRepository;
import com.zorvyn.user.service.UserService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class UserServiceImpl implements UserService {

    private static final Set<String> ALLOWED_SORT_FIELDS = Set.of(
        "id",
        "username",
        "email",
        "role",
        "status",
        "createdAt",
        "updatedAt"
    );
    private static final String LAST_ACTIVE_ADMIN_MSG =
        "Operation blocked: at least one active admin must remain";

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(
        UserRepository userRepository,
        PasswordEncoder passwordEncoder
    ) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public UserResponse createUser(CreateUserRequest request) {
        String username = normalizeUsername(request.username());
        String email = normalizeEmail(request.email());

        if (
            userRepository.existsByUsernameIgnoreCaseAndDeletedFalse(username)
        ) {
            throw new ConflictException("Username already exists");
        }
        if (userRepository.existsByEmailIgnoreCaseAndDeletedFalse(email)) {
            throw new ConflictException("Email already exists");
        }

        User user = new User();
        user.setUsername(username);
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(request.password()));
        user.setRole(request.role());
        user.setStatus(
            request.status() == null ? UserStatus.ACTIVE : request.status()
        );
        user.setDeleted(false);

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getUserById(Long id) {
        return UserMapper.toResponse(findActiveUserById(id));
    }

    @Override
    @Transactional(readOnly = true)
    public PagedResponse<UserResponse> listUsers(
        Role role,
        UserStatus status,
        String search,
        int page,
        int size,
        String sortBy,
        String sortDir
    ) {
        String resolvedSortBy = resolveSortBy(sortBy);
        Sort.Direction direction = resolveSortDirection(sortDir);

        Pageable pageable = PageRequest.of(
            page,
            size,
            Sort.by(direction, resolvedSortBy)
        );
        Specification<User> specification = buildSpecification(
            role,
            status,
            search
        );

        Page<UserResponse> responsePage = userRepository
            .findAll(specification, pageable)
            .map(UserMapper::toResponse);

        return PagedResponse.from(responsePage);
    }

    @Override
    public UserResponse updateUser(Long id, UpdateUserRequest request) {
        User user = findActiveUserById(id);

        if (!hasAnyUpdate(request)) {
            throw new BadRequestException(
                "At least one field must be provided to update"
            );
        }

        if (hasText(request.username())) {
            String normalizedUsername = normalizeUsername(request.username());
            if (
                !normalizedUsername.equalsIgnoreCase(user.getUsername()) &&
                userRepository.existsByUsernameIgnoreCaseAndDeletedFalseAndIdNot(
                    normalizedUsername,
                    id
                )
            ) {
                throw new ConflictException("Username already exists");
            }
            user.setUsername(normalizedUsername);
        }

        if (hasText(request.email())) {
            String normalizedEmail = normalizeEmail(request.email());
            if (
                !normalizedEmail.equalsIgnoreCase(user.getEmail()) &&
                userRepository.existsByEmailIgnoreCaseAndDeletedFalseAndIdNot(
                    normalizedEmail,
                    id
                )
            ) {
                throw new ConflictException("Email already exists");
            }
            user.setEmail(normalizedEmail);
        }

        if (hasText(request.password())) {
            user.setPasswordHash(passwordEncoder.encode(request.password()));
        }

        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateRole(Long id, Role role) {
        User user = findActiveUserById(id);
        if (
            user.getRole() == Role.ADMIN &&
            role != Role.ADMIN &&
            isLastActiveAdmin(user.getId())
        ) {
            throw new BadRequestException(LAST_ACTIVE_ADMIN_MSG);
        }
        user.setRole(role);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public UserResponse updateStatus(Long id, UserStatus status) {
        User user = findActiveUserById(id);
        if (
            user.getRole() == Role.ADMIN &&
            user.getStatus() == UserStatus.ACTIVE &&
            status == UserStatus.INACTIVE &&
            isLastActiveAdmin(user.getId())
        ) {
            throw new BadRequestException(LAST_ACTIVE_ADMIN_MSG);
        }
        user.setStatus(status);
        return UserMapper.toResponse(userRepository.save(user));
    }

    @Override
    public void deleteUser(Long id, String actorEmail) {
        User user = findActiveUserById(id);
        String normalizedActorEmail = normalizeEmail(actorEmail);
        if (user.getEmail().equalsIgnoreCase(normalizedActorEmail)) {
            throw new BadRequestException(
                "Admin cannot delete their own account"
            );
        }
        if (
            user.getRole() == Role.ADMIN &&
            user.getStatus() == UserStatus.ACTIVE &&
            isLastActiveAdmin(user.getId())
        ) {
            throw new BadRequestException(LAST_ACTIVE_ADMIN_MSG);
        }
        user.setDeleted(true);
        user.setDeletedAt(LocalDateTime.now());
        userRepository.save(user);
    }

    @Override
    @Transactional(readOnly = true)
    public UserResponse getCurrentUser(String email) {
        return userRepository
            .findByEmailIgnoreCaseAndDeletedFalse(normalizeEmail(email))
            .map(UserMapper::toResponse)
            .orElseThrow(() ->
                new ResourceNotFoundException("Current user not found")
            );
    }

    private User findActiveUserById(Long id) {
        return userRepository
            .findByIdAndDeletedFalse(id)
            .orElseThrow(() ->
                new ResourceNotFoundException("User not found with id=" + id)
            );
    }

    private Specification<User> buildSpecification(
        Role role,
        UserStatus status,
        String search
    ) {
        Specification<User> specification = (root, query, cb) ->
            cb.isFalse(root.get("deleted"));

        if (role != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("role"), role)
            );
        }

        if (status != null) {
            specification = specification.and((root, query, cb) ->
                cb.equal(root.get("status"), status)
            );
        }

        if (hasText(search)) {
            String likeTerm =
                "%" + search.trim().toLowerCase(Locale.ROOT) + "%";
            specification = specification.and((root, query, cb) ->
                cb.or(
                    cb.like(cb.lower(root.get("username")), likeTerm),
                    cb.like(cb.lower(root.get("email")), likeTerm)
                )
            );
        }

        return specification;
    }

    private String resolveSortBy(String sortBy) {
        String resolved = hasText(sortBy) ? sortBy.trim() : "createdAt";
        if (!ALLOWED_SORT_FIELDS.contains(resolved)) {
            throw new BadRequestException(
                "Invalid sortBy field. Allowed values: " +
                    List.copyOf(ALLOWED_SORT_FIELDS)
            );
        }
        return resolved;
    }

    private Sort.Direction resolveSortDirection(String sortDir) {
        try {
            return hasText(sortDir)
                ? Sort.Direction.fromString(sortDir.trim())
                : Sort.Direction.DESC;
        } catch (IllegalArgumentException ex) {
            throw new BadRequestException(
                "Invalid sortDir. Allowed values: ASC or DESC"
            );
        }
    }

    private boolean hasAnyUpdate(UpdateUserRequest request) {
        return (
            hasText(request.username()) ||
            hasText(request.email()) ||
            hasText(request.password())
        );
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    private String normalizeUsername(String username) {
        return username.trim();
    }

    private boolean isLastActiveAdmin(Long excludedUserId) {
        long remainingActiveAdmins =
            userRepository.countByRoleAndStatusAndDeletedFalseAndIdNot(
                Role.ADMIN,
                UserStatus.ACTIVE,
                excludedUserId
            );
        return remainingActiveAdmins == 0;
    }
}
