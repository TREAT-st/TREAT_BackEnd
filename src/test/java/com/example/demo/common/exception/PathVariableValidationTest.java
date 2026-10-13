package com.example.demo.common.exception;

import com.example.demo.domain.user.entity.User;
import org.junit.jupiter.api.Test;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * @Validated 컨트롤러의 파라미터 검증 실패가 400으로 나가는지 본다.
 *
 * ConstraintViolationException 핸들러가 없던 때는 catch-all로 빠져 500이 났다.
 * 실제 빈(@Validated 프록시)과 ExceptionAdvice를 지나야 재현되므로 컨텍스트를 띄운다.
 * 보안 필터는 끈다. 여기서 보려는 건 인증이 아니라 검증과 예외 변환이다.
 */
@SpringBootTest
@AutoConfigureMockMvc(addFilters = false)
@ActiveProfiles("test")
class PathVariableValidationTest {

    @Autowired
    private MockMvc mockMvc;

    @Test
    void 종목코드가_6자리_숫자가_아니면_stock_조회는_400() throws Exception {
        mockMvc.perform(get("/api/v1/stocks/12345"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.isSuccess").value(false))
                .andExpect(jsonPath("$.result.stockCode").value("종목코드는 6자리 숫자입니다."));
    }

    @Test
    void 종목코드가_6자리_숫자가_아니면_volatility_조회는_400() throws Exception {
        mockMvc.perform(get("/api/v1/volatility/ABC123"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.result.stockCode").value("종목코드는 6자리 숫자입니다."));
    }

    /** 형식은 맞지만 기록이 없는 종목은 검증을 통과해 404로 간다. 두 경우가 섞이면 안 된다. */
    @Test
    void 형식이_맞고_기록이_없으면_volatility_조회는_404_4309() throws Exception {
        mockMvc.perform(get("/api/v1/volatility/999999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value(4309));
    }

    /**
     * 같은 핸들러를 타는 @RequestParam 범위 검증도 500이 아니라 400이어야 한다.
     * 이 API는 @AuthUser가 검증보다 먼저 사용자를 요구하므로 인증된 사용자를 넣어둔다.
     * 보안 필터를 껐으므로 스레드의 SecurityContext가 그대로 리졸버에 전달된다.
     */
    @Test
    void 페이지_크기가_범위를_벗어나면_400() throws Exception {
        User user = User.builder().username("validation-test").build();
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(user, null, List.of()));
        try {
            mockMvc.perform(get("/api/v1/stocks/all-stock").param("pageSize", "500"))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.result.pageSize").value("pageSize는 200 이하여야 합니다."));
        } finally {
            SecurityContextHolder.clearContext();
        }
    }
}
