package com.zorvyn.user.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zorvyn.common.dto.PagedResponse;
import com.zorvyn.user.dto.response.UserResponse;
import com.zorvyn.user.security.JwtAuthenticationFilter;
import com.zorvyn.user.security.LoginRateLimitFilter;
import com.zorvyn.user.security.RestAccessDeniedHandler;
import com.zorvyn.user.security.RestAuthenticationEntryPoint;
import com.zorvyn.user.security.SecurityConfig;
import com.zorvyn.user.service.UserService;
import jakarta.servlet.FilterChain;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.context.annotation.Import;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@Import(
    {
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
    }
)
class UserControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserService userService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private LoginRateLimitFilter loginRateLimitFilter;

    @BeforeEach
    void setUp() throws Exception {
        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                invocation.getArgument(0),
                invocation.getArgument(1)
            );
            return null;
        })
            .when(jwtAuthenticationFilter)
            .doFilter(any(), any(), any());

        doAnswer(invocation -> {
            FilterChain chain = invocation.getArgument(2);
            chain.doFilter(
                invocation.getArgument(0),
                invocation.getArgument(1)
            );
            return null;
        })
            .when(loginRateLimitFilter)
            .doFilter(any(), any(), any());
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void listUsers_shouldReturnForbiddenForViewer() throws Exception {
        mockMvc.perform(get("/api/users")).andExpect(status().isForbidden());
        verifyNoInteractions(userService);
    }

    @Test
    @WithMockUser(roles = "ADMIN")
    void listUsers_shouldReturnOkForAdmin() throws Exception {
        PagedResponse<UserResponse> response = new PagedResponse<>(
            List.of(),
            0,
            10,
            0,
            0,
            true,
            true
        );
        when(
            userService.listUsers(
                any(),
                any(),
                nullable(String.class),
                anyInt(),
                anyInt(),
                nullable(String.class),
                nullable(String.class)
            )
        ).thenReturn(response);

        mockMvc.perform(get("/api/users")).andExpect(status().isOk());
    }
}
