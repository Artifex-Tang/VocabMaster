package com.vocabmaster.wordlist.controller;

import com.vocabmaster.common.result.R;
import com.vocabmaster.security.UserContext;
import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.wordlist.dto.SubscribeResponse;
import com.vocabmaster.wordlist.dto.WordListDetailDto;
import com.vocabmaster.wordlist.dto.WordListSummaryDto;
import com.vocabmaster.wordlist.service.WordListService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/word-lists")
@RequiredArgsConstructor
@Tag(name = "词库", description = "自定义词库：广场 / 订阅 / 单元 / 学新词")
public class WordListController {

    private final WordListService wordListService;

    @GetMapping
    @Operation(summary = "词库广场（默认内置；source_type=imported 看我的上传）")
    public R<List<WordListSummaryDto>> square(@RequestParam(value = "source_type",
                                                            required = false,
                                                            defaultValue = "builtin") String sourceType) {
        return R.ok(wordListService.listSquare(UserContext.currentUserId(), sourceType));
    }

    @GetMapping("/{id}")
    @Operation(summary = "词库详情（含各单元进度 + 当前单元游标）")
    public R<WordListDetailDto> detail(@PathVariable Long id) {
        return R.ok(wordListService.detail(UserContext.currentUserId(), id));
    }

    @PostMapping("/{id}/subscribe")
    @Operation(summary = "订阅词库（幂等，起始单元=1）")
    public R<SubscribeResponse> subscribe(@PathVariable Long id) {
        return R.ok(wordListService.subscribe(UserContext.currentUserId(), id));
    }

    @GetMapping("/{id}/learn")
    @Operation(summary = "拉取当前/指定单元的新词（未学词）")
    public R<List<WordDetailDto>> learn(@PathVariable Long id,
                                        @RequestParam(value = "unit", required = false) Integer unit,
                                        @RequestParam(value = "limit", defaultValue = "20") int limit) {
        return R.ok(wordListService.learnNewWords(UserContext.currentUserId(), id, unit, limit));
    }

    @PostMapping("/{id}/units/{unitNo}/advance")
    @Operation(summary = "推进单元游标到目标单元")
    public R<Integer> advance(@PathVariable Long id, @PathVariable Integer unitNo) {
        return R.ok(wordListService.advanceUnit(UserContext.currentUserId(), id, unitNo));
    }
}
