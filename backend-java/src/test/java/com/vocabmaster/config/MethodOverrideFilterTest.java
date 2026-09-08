package com.vocabmaster.config;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

class MethodOverrideFilterTest {

    @RestController
    static class DummyController {
        @PatchMapping("/ping")
        String patch() {
            return "patched";
        }

        @GetMapping("/ping")
        String get() {
            return "got";
        }
    }

    private MockMvc mockMvc;

    @BeforeEach
    void setUp() {
        mockMvc = MockMvcBuilders.standaloneSetup(new DummyController())
                .addFilters(new MethodOverrideFilter())
                .build();
    }

    @Test
    @DisplayName("POST + X-HTTP-Method-Override: PATCH 路由到 @PatchMapping")
    void postWithOverrideHeaderRoutesToPatchMapping() throws Exception {
        mockMvc.perform(post("/ping")
                        .header("X-HTTP-Method-Override", "PATCH")
                        .contentType("application/json")
                        .content("{}"))
                .andExpect(status().isOk())
                .andExpect(content().string("patched"));
    }

    @Test
    @DisplayName("无 override 头的 POST 不改写，仍 405")
    void plainPostStaysPost() throws Exception {
        mockMvc.perform(post("/ping"))
                .andExpect(status().isMethodNotAllowed());
    }

    @Test
    @DisplayName("override 目标 GET 不在白名单，忽略改写仍 405")
    void overrideToGetIgnored() throws Exception {
        mockMvc.perform(post("/ping")
                        .header("X-HTTP-Method-Override", "GET"))
                .andExpect(status().isMethodNotAllowed());
    }
}
