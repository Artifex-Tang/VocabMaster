package com.vocabmaster.wrongword.controller;

import com.vocabmaster.common.result.PageResult;
import com.vocabmaster.common.result.R;
import com.vocabmaster.security.UserContext;
import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.wrongword.dto.WrongWordDto;
import com.vocabmaster.wrongword.service.WrongWordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/wrong-words")
@RequiredArgsConstructor
@Tag(name = "错题本", description = "错词列表 / 错题复习 / 标记解决")
public class WrongWordController {

    private final WrongWordService wrongWordService;

    @GetMapping
    @Operation(summary = "获取错题列表")
    public R<PageResult<WrongWordDto>> list(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) Integer resolved,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(wrongWordService.list(UserContext.currentUserId(), level, resolved, page, pageSize));
    }

    @PostMapping("/review")
    @Operation(summary = "获取错题复习词卡（用 /study/answer 上报答题结果）")
    public R<List<WordDetailDto>> review(
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "20") int limit) {
        return R.ok(wrongWordService.reviewWords(UserContext.currentUserId(), level, limit));
    }

    @PostMapping("/resolve")
    @Operation(summary = "手动标记错词为已解决")
    public R<Void> resolve(@RequestParam Long wordId) {
        wrongWordService.resolve(UserContext.currentUserId(), wordId);
        return R.ok(null);
    }
}
