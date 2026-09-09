package com.vocabmaster.config;

import com.vocabmaster.security.JwtAuthenticationEntryPoint;
import com.vocabmaster.security.JwtAuthenticationFilter;
import com.vocabmaster.word.controller.WordController;
import com.vocabmaster.word.service.LevelService;
import com.vocabmaster.word.service.WordImportService;
import com.vocabmaster.word.service.WordService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Answers;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.boot.web.client.RestTemplateBuilder;
import org.springframework.context.annotation.Import;
import org.springframework.test.web.servlet.MockMvc;

import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletRequest;
import jakarta.servlet.ServletResponse;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.authentication;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.security.authentication.TestingAuthenticationToken;
import org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors;

/**
 * 游客（无 token）可访问只读词库内容端点；用户功能端点仍被拦截。
 * jwtFilter 用 mock（直通匿名），entryPoint 用真实实现（写 401）。
 */
@WebMvcTest(controllers = WordController.class)
@Import({SecurityConfig.class, JwtAuthenticationEntryPoint.class})
class GuestWordAccessTest {

    @Autowired MockMvc mvc;
    @MockBean LevelService levelService;
    @MockBean WordService wordService;
    @MockBean WordImportService wordImportService;
    @MockBean JwtAuthenticationFilter jwtFilter;

    /** @WebMvcTest 会扫 WebMvcConfigurer → WebMvcConfig.restTemplate 需要 builder；deep stubs 供 fluent 链 */
    @MockBean(answer = Answers.RETURNS_DEEP_STUBS)
    RestTemplateBuilder restTemplateBuilder;

    /**
     * mock 的 filter 默认不续链，请求会被黑洞成假 200。
     * doFilterInternal 是 protected 引用不到，改 stub 公共入口 doFilter 继续链，
     * 安全链才能真正拦截（未放行路径 → 401）。
     */
    @BeforeEach
    void letMockedFilterContinueChain() throws Exception {
        doAnswer(inv -> {
            inv.<FilterChain>getArgument(2)
               .doFilter(inv.getArgument(0, ServletRequest.class),
                         inv.getArgument(1, ServletResponse.class));
            return null;
        }).when(jwtFilter).doFilter(any(ServletRequest.class),
                                    any(ServletResponse.class),
                                    any(FilterChain.class));
    }

    @Test
    @DisplayName("无 token GET /words/search → 200")
    void searchWithoutToken() throws Exception {
        mvc.perform(get("/words/search").param("q", "cat"))
                .andExpect(status().isOk());
    }

    @Test
    @DisplayName("无 token GET /words/{id} → 200")
    void detailWithoutToken() throws Exception {
        mvc.perform(get("/words/1")).andExpect(status().isOk());
    }

    @Test
    @DisplayName("无 token GET /words/download → 401（不放行）")
    void downloadStillProtected() throws Exception {
        mvc.perform(get("/words/download").param("level", "CET4"))
                .andExpect(status().isUnauthorized());
    }

    @Test
    @DisplayName("带认证 token GET /words/download → 200（登录用户不受影响）")
    void downloadWithTokenStillWorks() throws Exception {
        mvc.perform(get("/words/download").param("level", "CET4")
                        .with(SecurityMockMvcRequestPostProcessors.authentication(
                                new TestingAuthenticationToken("u1", "n/a", "ROLE_USER"))))
                .andExpect(status().isOk());
    }
}
