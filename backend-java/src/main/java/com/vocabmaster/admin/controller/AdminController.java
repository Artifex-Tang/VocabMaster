package com.vocabmaster.admin.controller;

import com.vocabmaster.admin.dto.AdminUserDto;
import com.vocabmaster.admin.dto.DashboardResponse;
import com.vocabmaster.admin.service.AdminService;
import com.vocabmaster.common.result.PageResult;
import com.vocabmaster.common.result.R;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.service.WordImportService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "管理后台", description = "仅管理员可访问")
public class AdminController {

    private final AdminService adminService;
    private final WordImportService wordImportService;

    // ---- 统计看板 ----

    @GetMapping("/dashboard")
    @Operation(summary = "统计看板（DAU / 新注册 / 词库规模）")
    public R<DashboardResponse> dashboard() {
        return R.ok(adminService.dashboard());
    }

    // ---- 用户管理 ----

    @GetMapping("/users")
    @Operation(summary = "用户列表（分页，支持关键词搜索）")
    public R<PageResult<AdminUserDto>> listUsers(
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(adminService.listUsers(keyword, page, pageSize));
    }

    @PatchMapping("/users/{uuid}/status")
    @Operation(summary = "修改用户状态（0=禁用，1=正常）")
    public R<Void> setUserStatus(@PathVariable String uuid, @RequestParam int status) {
        adminService.setUserStatus(uuid, status);
        return R.ok(null);
    }

    // ---- 词库管理 ----

    @GetMapping("/words")
    @Operation(summary = "词库列表（分页，支持等级/关键词筛选）")
    public R<PageResult<WordBank>> listWords(
            @RequestParam(required = false) String level,
            @RequestParam(required = false) String keyword,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "20") int pageSize) {
        return R.ok(adminService.listWords(level, keyword, page, pageSize));
    }

    @PostMapping(value = "/words/import", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    @Operation(summary = "CSV 批量导入词库")
    public R<String> importWords(@RequestParam("file") MultipartFile file) {
        int count = wordImportService.importFromMultipart(file);
        return R.ok("成功导入 " + count + " 条词条");
    }

    @PostMapping("/words/{id}/audit")
    @Operation(summary = "审核词条（auditStatus: 1=通过，-1=拒绝）")
    public R<Void> auditWord(@PathVariable Long id, @RequestParam int auditStatus) {
        adminService.auditWord(id, auditStatus);
        return R.ok(null);
    }
}
