package com.vocabmaster.word.service;

import cn.hutool.core.io.FileUtil;
import cn.hutool.core.text.csv.CsvReader;
import cn.hutool.core.text.csv.CsvRow;
import cn.hutool.core.text.csv.CsvUtil;
import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vocabmaster.common.constant.AppConstants;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.mapper.WordBankMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.File;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordImportService {

    private static final int BATCH_SIZE = 200;

    private final WordBankMapper wordBankMapper;

    @Value("${app.seed.csv-path:../seed/words_sample.csv}")
    private String seedCsvPath;

    /**
     * 应用启动完成后异步检查：若 word_bank 为空则自动导入种子词库。
     */
    @Async
    @EventListener(ApplicationReadyEvent.class)
    public void autoImportOnStartup() {
        try {
            Long count = wordBankMapper.selectCount(Wrappers.emptyWrapper());
            if (count != null && count > 0) {
                log.info("word_bank 已有 {} 条记录，跳过种子导入", count);
                return;
            }

            File csvFile = new File(seedCsvPath);
            if (!csvFile.exists()) {
                // 尝试相对于工作目录的父目录
                csvFile = new File(System.getProperty("user.dir"), seedCsvPath);
            }
            if (!csvFile.exists()) {
                log.warn("种子 CSV 文件不存在: {}，跳过自动导入", seedCsvPath);
                return;
            }

            log.info("word_bank 为空，开始导入种子数据: {}", csvFile.getAbsolutePath());
            int imported = importFromFile(csvFile);
            log.info("种子数据导入完成，共 {} 条", imported);
        } catch (Exception e) {
            log.error("种子数据自动导入失败", e);
        }
    }

    /**
     * 管理员通过 CSV 批量导入词库（跳过重复记录）。
     * @return 成功导入条数
     */
    @Transactional
    public int importFromMultipart(MultipartFile file) {
        try (InputStream is = file.getInputStream()) {
            return importFromStream(is);
        } catch (Exception e) {
            log.error("CSV 导入失败", e);
            throw new RuntimeException("CSV 导入失败: " + e.getMessage(), e);
        }
    }

    // ---- private ----

    private int importFromFile(File file) {
        try (var reader = new java.io.FileInputStream(file)) {
            return importFromStream(reader);
        } catch (Exception e) {
            log.error("读取 CSV 文件失败", e);
            throw new RuntimeException(e);
        }
    }

    private int importFromStream(InputStream is) {
        CsvReader reader = CsvUtil.getReader();
        List<CsvRow> rows;
        try {
            rows = reader.read(new InputStreamReader(is, StandardCharsets.UTF_8)).getRows();
        } catch (Exception e) {
            throw new RuntimeException("CSV 解析失败", e);
        }

        if (rows.isEmpty()) return 0;

        // 第 0 行是 header，跳过
        List<WordBank> batch = new ArrayList<>(BATCH_SIZE);
        int imported = 0;

        for (int i = 1; i < rows.size(); i++) {
            CsvRow row = rows.get(i);
            if (row.size() < 9) continue;

            WordBank wb = buildWordBank(row);
            if (wb == null) continue;

            batch.add(wb);
            if (batch.size() >= BATCH_SIZE) {
                imported += batchInsertIgnore(batch);
                batch.clear();
            }
        }
        if (!batch.isEmpty()) {
            imported += batchInsertIgnore(batch);
        }
        return imported;
    }

    private WordBank buildWordBank(CsvRow row) {
        try {
            String levelCode = cell(row, 0);
            String word = cell(row, 1);
            if (levelCode.isBlank() || word.isBlank()) return null;

            String topicRaw = cell(row, 8);
            String topicCode = topicRaw.isBlank() ? null : topicRaw.toUpperCase();

            BigDecimal frequency = parseDecimal(cell(row, 14));
            // CSV 种子数据 frequency 使用 1-10 评分，归一化到 0-1
            if (frequency != null && frequency.compareTo(BigDecimal.ONE) > 0) {
                frequency = frequency.divide(BigDecimal.TEN, 3, RoundingMode.HALF_UP);
            }

            return WordBank.builder()
                    .levelCode(levelCode)
                    .word(word)
                    .wordLower(word.toLowerCase())
                    .ipaUk(cell(row, 2))
                    .ipaUs(cell(row, 3))
                    .enDefinition(cell(row, 4))
                    .zhDefinition(cell(row, 5))
                    .exampleEn(cell(row, 6))
                    .exampleZh(cell(row, 7))
                    .topicCode(topicCode)
                    .audioUrlUk(cell(row, 9))
                    .audioUrlUs(cell(row, 10))
                    .imageUrl(cell(row, 11))
                    .emoji(cell(row, 12))
                    .difficulty(parseInt(cell(row, 13)))
                    .frequency(frequency)
                    .auditStatus(AppConstants.AUDIT_PASS)
                    .build();
        } catch (Exception e) {
            log.warn("CSV 行解析失败，跳过: {}", row, e);
            return null;
        }
    }

    /**
     * 逐条插入，遇到重复 key（uk_level_word）则跳过。
     * MyBatis-Plus 不原生支持 INSERT IGNORE，用 try-catch 模拟。
     */
    private int batchInsertIgnore(List<WordBank> batch) {
        int count = 0;
        for (WordBank wb : batch) {
            try {
                wordBankMapper.insert(wb);
                count++;
            } catch (Exception e) {
                // 唯一键冲突，跳过
                if (isDuplicateKeyException(e)) {
                    log.debug("跳过重复词条: {} / {}", wb.getLevelCode(), wb.getWord());
                } else {
                    log.warn("插入词条失败: {} / {}", wb.getLevelCode(), wb.getWord(), e);
                }
            }
        }
        return count;
    }

    private boolean isDuplicateKeyException(Exception e) {
        String msg = e.getMessage();
        return msg != null && (msg.contains("Duplicate entry") || msg.contains("duplicate key"));
    }

    private String cell(CsvRow row, int index) {
        if (index >= row.size()) return "";
        String val = row.get(index);
        return val == null ? "" : val.trim();
    }

    private Integer parseInt(String s) {
        if (s == null || s.isBlank()) return null;
        try { return Integer.parseInt(s); } catch (NumberFormatException e) { return null; }
    }

    private BigDecimal parseDecimal(String s) {
        if (s == null || s.isBlank()) return null;
        try { return new BigDecimal(s); } catch (NumberFormatException e) { return null; }
    }
}
