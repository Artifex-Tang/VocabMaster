package com.vocabmaster.word.controller;

import com.vocabmaster.common.constant.AppConstants;
import com.vocabmaster.common.result.PageResult;
import com.vocabmaster.common.result.R;
import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.word.dto.WordDownloadResponse;
import com.vocabmaster.word.dto.WordSummaryDto;
import com.vocabmaster.word.entity.Level;
import com.vocabmaster.word.entity.WordTopic;
import com.vocabmaster.word.service.LevelService;
import com.vocabmaster.word.service.WordImportService;
import com.vocabmaster.word.service.WordService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

@RestController
@RequestMapping("/words")
@RequiredArgsConstructor
@Tag(name = "词库", description = "等级/主题/词条查询与下载")
public class WordController {

    private final LevelService levelService;
    private final WordService wordService;
    private final WordImportService wordImportService;

    // ---- 公开接口（无需登录）----

    @GetMapping("/levels")
    @Operation(summary = "获取等级列表（公开）")
    public R<List<Level>> getLevels() {
        return R.ok(levelService.getLevels());
    }

    @GetMapping("/topics")
    @Operation(summary = "获取主题列表（公开）")
    public R<List<WordTopic>> getTopics() {
        return R.ok(levelService.getTopics());
    }

    // ---- 需要登录的接口 ----

    @GetMapping("/{id}")
    @Operation(summary = "查询单词详情（按 ID）")
    public R<WordDetailDto> getById(@PathVariable Long id) {
        return R.ok(wordService.getById(id));
    }

    @GetMapping("/by-word")
    @Operation(summary = "查询单词详情（按 level + word）")
    public R<WordDetailDto> getByWord(@RequestParam String level,
                                      @RequestParam String word) {
        return R.ok(wordService.getByLevelAndWord(level, word));
    }

    @GetMapping("/search")
    @Operation(summary = "关键词搜索单词")
    public R<PageResult<WordSummaryDto>> search(
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String level,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(wordService.search(level, q, page, pageSize));
    }

    @GetMapping("/download")
    @Operation(summary = "批量下载某等级词库（离线用，支持增量）")
    public R<WordDownloadResponse> download(
            @RequestParam String level,
            @RequestParam(required = false) String since) {
        return R.ok(wordService.download(level, since));
    }

    // ---- 管理员接口（/admin/words 路径，由 SecurityConfig 保护）----

    @PostMapping(value = "/admin/import",
                 consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "管理员批量导入 CSV 词库")
    public R<String> adminImport(@RequestParam("file") MultipartFile file) {
        int count = wordImportService.importFromMultipart(file);
        return R.ok("成功导入 " + count + " 条词条");
    }
}
