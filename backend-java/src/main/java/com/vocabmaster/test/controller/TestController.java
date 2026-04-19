package com.vocabmaster.test.controller;

import com.vocabmaster.common.result.R;
import com.vocabmaster.security.UserContext;
import com.vocabmaster.test.dto.*;
import com.vocabmaster.test.service.TestService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/test")
@RequiredArgsConstructor
@Tag(name = "测试", description = "拼写 / 选择 / 听写测试")
public class TestController {

    private final TestService testService;

    @PostMapping("/generate")
    @Operation(summary = "生成测试题目")
    public R<GenerateTestResponse> generate(@Valid @RequestBody GenerateTestRequest req) {
        Long userId = UserContext.currentUserId();
        return R.ok(testService.generate(userId, req));
    }

    @PostMapping("/submit")
    @Operation(summary = "提交测试答案")
    public R<SubmitTestResponse> submit(@Valid @RequestBody SubmitTestRequest req) {
        Long userId = UserContext.currentUserId();
        return R.ok(testService.submit(userId, req));
    }
}
