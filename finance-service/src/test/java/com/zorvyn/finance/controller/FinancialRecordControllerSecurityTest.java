package com.zorvyn.finance.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.zorvyn.finance.dto.response.FinancialRecordResponse;
import com.zorvyn.common.dto.PagedResponse;
import com.zorvyn.finance.security.JwtAuthenticationFilter;
import com.zorvyn.finance.security.RestAccessDeniedHandler;
import com.zorvyn.finance.security.RestAuthenticationEntryPoint;
import com.zorvyn.finance.security.SecurityConfig;
import com.zorvyn.finance.service.FinancialRecordService;
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

@WebMvcTest(FinancialRecordController.class)
@AutoConfigureMockMvc
@Import(
    {
        SecurityConfig.class,
        RestAuthenticationEntryPoint.class,
        RestAccessDeniedHandler.class,
    }
)
class FinancialRecordControllerSecurityTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private FinancialRecordService financialRecordService;

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
    void listRecords_shouldReturnForbiddenForViewer() throws Exception {
        mockMvc.perform(get("/api/records")).andExpect(status().isForbidden());
        verifyNoInteractions(financialRecordService);
    }

    @Test
    @WithMockUser(roles = "ANALYST")
    void listRecords_shouldReturnOkForAnalyst() throws Exception {
        PagedResponse<FinancialRecordResponse> response = new PagedResponse<>(
            List.of(),
            0,
            10,
            0,
            0,
            true,
            true
        );
        when(
            financialRecordService.listRecords(
                any(),
                nullable(String.class),
                nullable(java.time.LocalDate.class),
                nullable(java.time.LocalDate.class),
                nullable(java.math.BigDecimal.class),
                nullable(java.math.BigDecimal.class),
                nullable(String.class),
                anyInt(),
                anyInt(),
                nullable(String.class),
                nullable(String.class)
            )
        ).thenReturn(response);

        mockMvc.perform(get("/api/records")).andExpect(status().isOk());
    }
}
