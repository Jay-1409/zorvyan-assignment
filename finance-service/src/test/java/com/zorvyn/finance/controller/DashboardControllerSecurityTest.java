package com.zorvyn.finance.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zorvyn.finance.dto.response.DashboardSummaryResponse;
import com.zorvyn.finance.security.JwtAuthenticationFilter;
import com.zorvyn.finance.security.RestAccessDeniedHandler;
import com.zorvyn.finance.security.RestAuthenticationEntryPoint;
import com.zorvyn.finance.security.SecurityConfig;
import com.zorvyn.finance.service.DashboardService;
import jakarta.servlet.FilterChain;
import java.math.BigDecimal;
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

@WebMvcTest(DashboardController.class)
@AutoConfigureMockMvc
@Import(
    {
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
    }
)
class DashboardControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private DashboardService dashboardService;

    @MockBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

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
    }

    @Test
    @WithMockUser(roles = "VIEWER")
    void summary_shouldReturnOkForViewer() throws Exception {
        DashboardSummaryResponse response = new DashboardSummaryResponse(
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            BigDecimal.ZERO,
            List.of(),
            List.of(),
            List.of()
        );
        when(dashboardService.getSummary(any(), any(), anyInt())).thenReturn(
            response
        );

        mockMvc
            .perform(get("/api/dashboard/summary"))
            .andExpect(status().isOk());
    }

    @Test
    void summary_shouldReturnUnauthorizedWithoutAuthentication()
        throws Exception {
        mockMvc
            .perform(get("/api/dashboard/summary"))
            .andExpect(status().isUnauthorized());
    }
}
