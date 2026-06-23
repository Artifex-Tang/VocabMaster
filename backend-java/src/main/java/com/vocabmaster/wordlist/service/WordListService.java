package com.vocabmaster.wordlist.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vocabmaster.study.entity.UserWordProgress;
import com.vocabmaster.study.mapper.UserWordProgressMapper;
import com.vocabmaster.word.dto.WordDetailDto;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.mapper.WordBankMapper;
import com.vocabmaster.wordlist.dto.SubscribeResponse;
import com.vocabmaster.wordlist.dto.UnitSummaryDto;
import com.vocabmaster.wordlist.dto.WordListDetailDto;
import com.vocabmaster.wordlist.dto.WordListSummaryDto;
import com.vocabmaster.wordlist.entity.UserListSubscription;
import com.vocabmaster.wordlist.entity.WordList;
import com.vocabmaster.wordlist.mapper.UserListSubscriptionMapper;
import com.vocabmaster.wordlist.mapper.WordListItemMapper;
import com.vocabmaster.wordlist.mapper.WordListMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
@Slf4j
public class WordListService {

    private final WordListMapper wordListMapper;
    private final WordListItemMapper wordListItemMapper;
    private final UserListSubscriptionMapper userListSubscriptionMapper;
    private final WordBankMapper wordBankMapper;
    private final UserWordProgressMapper progressMapper;

    /**
     * 单元完成判定：learned>=total。
     * 空单元（0/0）视为已完成（learned 0>=total 0），避免空列表永远卡住。
     */
    public static boolean isUnitComplete(int totalCount, int learnedCount) {
        return learnedCount >= totalCount;
    }

    /**
     * 广场：列出所有词库，标记当前用户是否已订阅。
     */
    public List<WordListSummaryDto> listSquare(Long userId) {
        List<WordList> lists = wordListMapper.selectList(null);
        if (lists.isEmpty()) return List.of();

        // 一次性取该用户全部订阅，避免 N+1
        List<UserListSubscription> subs = userListSubscriptionMapper.selectList(
                Wrappers.<UserListSubscription>lambdaQuery()
                        .eq(UserListSubscription::getUserId, userId));
        java.util.Set<Long> subscribedIds = new java.util.HashSet<>();
        for (UserListSubscription s : subs) subscribedIds.add(s.getListId());

        List<WordListSummaryDto> result = new ArrayList<>(lists.size());
        for (WordList l : lists) {
            result.add(WordListSummaryDto.builder()
                    .id(l.getId())
                    .name(l.getName())
                    .description(l.getDescription())
                    .sourceType(l.getSourceType())
                    .originLevelCode(l.getOriginLevelCode())
                    .wordCount(l.getWordCount())
                    .coverEmoji(l.getCoverEmoji())
                    .subscribed(subscribedIds.contains(l.getId()))
                    .build());
        }
        return result;
    }

    /**
     * 详情：含每个单元的总数/已学/已掌握，及当前单元游标。
     */
    public WordListDetailDto detail(Long userId, Long listId) {
        WordList list = wordListMapper.selectById(listId);
        if (list == null) {
            throw new IllegalArgumentException("词库不存在: " + listId);
        }

        UserListSubscription sub = userListSubscriptionMapper.find(userId, listId);

        // 一次性拉该用户在该词库 originLevel 下的全部进度，建 wordId→stage 索引
        Map<Long, Integer> stageByWordId = buildStageIndex(userId, list.getOriginLevelCode());

        // 各单元的词数（unit_no, cnt）
        List<Map<String, Object>> unitCounts = wordListItemMapper.countByUnit(listId);

        List<UnitSummaryDto> units = new ArrayList<>(unitCounts.size());
        for (Map<String, Object> row : unitCounts) {
            Integer unitNo = ((Number) row.get("unit_no")).intValue();
            int total = ((Number) row.get("cnt")).intValue();

            List<Long> wordIds = wordListItemMapper.findWordIdsByUnit(listId, unitNo);
            int learned = 0;
            int mastered = 0;
            for (Long wid : wordIds) {
                Integer stage = stageByWordId.get(wid);
                if (stage != null && stage > 0) learned++;
                if (stage != null && stage == 9) mastered++;
            }

            boolean isCurrent = sub != null && sub.getCurrentUnitNo() != null
                    && sub.getCurrentUnitNo().equals(unitNo);
            units.add(UnitSummaryDto.builder()
                    .unitNo(unitNo)
                    .totalCount(total)
                    .learnedCount(learned)
                    .masteredCount(mastered)
                    .isCurrent(isCurrent)
                    .completed(isUnitComplete(total, learned))
                    .build());
        }

        return WordListDetailDto.builder()
                .id(list.getId())
                .name(list.getName())
                .description(list.getDescription())
                .originLevelCode(list.getOriginLevelCode())
                .wordCount(list.getWordCount())
                .unitCount(unitCounts.size())
                .subscribed(sub != null)
                .currentUnitNo(sub != null ? sub.getCurrentUnitNo() : null)
                .units(units)
                .build();
    }

    /**
     * 订阅词库（幂等 upsert），起始单元 = 1。
     */
    public SubscribeResponse subscribe(Long userId, Long listId) {
        WordList list = wordListMapper.selectById(listId);
        if (list == null) {
            throw new IllegalArgumentException("词库不存在: " + listId);
        }
        userListSubscriptionMapper.upsertSubscribe(userId, listId);
        return SubscribeResponse.builder()
                .listId(listId)
                .currentUnitNo(1)
                .build();
    }

    /**
     * 学新词：取该单元内用户尚未学过（stage=0 或无记录）的词。
     * unitNo 为空时用订阅游标。
     */
    public List<WordDetailDto> learnNewWords(Long userId, Long listId, Integer unitNo, int limit) {
        UserListSubscription sub = userListSubscriptionMapper.find(userId, listId);
        if (sub == null) {
            throw new IllegalStateException("未订阅该词库");
        }
        int unit = unitNo != null ? unitNo : sub.getCurrentUnitNo();
        List<WordBank> words = wordBankMapper.findNewWordsByUnit(listId, unit, userId, limit);
        List<WordDetailDto> result = new ArrayList<>(words.size());
        for (WordBank w : words) {
            result.add(toDetailDto(w));
        }
        return result;
    }

    /**
     * 推进单元游标到目标单元。
     */
    public Integer advanceUnit(Long userId, Long listId, Integer targetUnitNo) {
        UserListSubscription sub = userListSubscriptionMapper.find(userId, listId);
        if (sub == null) {
            throw new IllegalStateException("未订阅该词库");
        }
        userListSubscriptionMapper.advanceUnit(userId, listId, targetUnitNo);
        return targetUnitNo;
    }

    // ---- private ----

    /** 取用户在某 originLevel 下所有词的进度，建 wordId→stage 索引。 */
    private Map<Long, Integer> buildStageIndex(Long userId, String levelCode) {
        List<UserWordProgress> rows = progressMapper.selectList(
                Wrappers.<UserWordProgress>lambdaQuery()
                        .eq(UserWordProgress::getUserId, userId)
                        .eq(UserWordProgress::getLevelCode, levelCode));
        Map<Long, Integer> idx = new HashMap<>(rows.size() * 2);
        for (UserWordProgress p : rows) {
            idx.put(p.getWordId(), p.getStage());
        }
        return idx;
    }

    /** WordBank → WordDetailDto，仅映射 DTO 上真实存在的字段。 */
    private WordDetailDto toDetailDto(WordBank w) {
        return WordDetailDto.builder()
                .id(w.getId())
                .levelCode(w.getLevelCode())
                .word(w.getWord())
                .ipaUk(w.getIpaUk())
                .ipaUs(w.getIpaUs())
                .enDefinition(w.getEnDefinition())
                .zhDefinition(w.getZhDefinition())
                .exampleEn(w.getExampleEn())
                .exampleZh(w.getExampleZh())
                .topicCode(w.getTopicCode())
                .audioUrlUk(w.getAudioUrlUk())
                .audioUrlUs(w.getAudioUrlUs())
                .imageUrl(w.getImageUrl())
                .emoji(w.getEmoji())
                .pos(w.getPos())
                .difficulty(w.getDifficulty())
                .frequency(w.getFrequency())
                .relatedWords(w.getRelatedWords())
                .build();
    }
}
