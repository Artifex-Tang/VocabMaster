package com.vocabmaster.test.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.vocabmaster.common.constant.AppConstants;
import com.vocabmaster.common.constant.RedisKey;
import com.vocabmaster.common.exception.BizException;
import com.vocabmaster.common.result.ErrorCode;
import com.vocabmaster.study.dto.AnswerRequest;
import com.vocabmaster.study.entity.WrongWord;
import com.vocabmaster.study.entity.UserWordProgress;
import com.vocabmaster.study.mapper.UserWordProgressMapper;
import com.vocabmaster.study.mapper.WrongWordMapper;
import com.vocabmaster.study.service.StudyService;
import com.vocabmaster.test.dto.*;
import com.vocabmaster.test.model.TestSession;
import com.vocabmaster.word.entity.WordBank;
import com.vocabmaster.word.mapper.WordBankMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class TestService {

    private static final int TEST_SESSION_TTL_HOURS = 1;
    private static final int CHOICE_DISTRACTOR_COUNT = 3;
    private static final java.util.Set<String> VALID_MODES = Set.of("spelling", "choice", "listening");
    private static final java.util.Set<String> VALID_SOURCES = Set.of("due", "all", "wrong_words");

    private final WordBankMapper wordBankMapper;
    private final UserWordProgressMapper progressMapper;
    private final WrongWordMapper wrongWordMapper;
    private final StudyService studyService;
    private final RedisTemplate<String, Object> redisTemplate;

    /** 生成测试：选词、构建题目、存 Redis 会话，返回题目（不含答案）。 */
    public GenerateTestResponse generate(Long userId, GenerateTestRequest req) {
        if (!VALID_MODES.contains(req.getMode())) {
            throw new BizException(ErrorCode.PARAM_INVALID, "mode 不支持: " + req.getMode());
        }

        List<WordBank> words = selectWords(userId, req);
        if (words.isEmpty()) {
            throw new BizException(ErrorCode.WORD_NOT_FOUND, "当前来源下没有可出题的单词");
        }

        String testId = "tst_" + UUID.randomUUID().toString().replace("-", "");
        List<TestSession.SessionQuestion> sessionQuestions = new ArrayList<>(words.size());
        List<TestQuestion> clientQuestions = new ArrayList<>(words.size());

        for (int i = 0; i < words.size(); i++) {
            WordBank wb = words.get(i);
            String qId = "q" + (i + 1);

            sessionQuestions.add(TestSession.SessionQuestion.builder()
                    .questionId(qId)
                    .wordId(wb.getId())
                    .correctAnswer(wb.getWord().toLowerCase())
                    .build());

            clientQuestions.add(buildClientQuestion(qId, wb, req.getMode()));
        }

        TestSession session = TestSession.builder()
                .testId(testId)
                .userId(userId)
                .levelCode(req.getLevelCode())
                .mode(req.getMode())
                .questions(sessionQuestions)
                .build();

        redisTemplate.opsForValue().set(
                RedisKey.testSession(testId), session,
                TEST_SESSION_TTL_HOURS, TimeUnit.HOURS);

        return GenerateTestResponse.builder()
                .testId(testId)
                .mode(req.getMode())
                .levelCode(req.getLevelCode())
                .questions(clientQuestions)
                .build();
    }

    /** 提交答案：评分 + 触发 StudyService.answer() 更新进度。 */
    public SubmitTestResponse submit(Long userId, SubmitTestRequest req) {
        TestSession session = loadSession(req.getTestId(), userId);

        Map<String, TestSession.SessionQuestion> qMap = session.getQuestions().stream()
                .collect(Collectors.toMap(TestSession.SessionQuestion::getQuestionId, Function.identity()));

        List<QuestionResult> results = new ArrayList<>(req.getAnswers().size());
        int correctCount = 0;

        for (TestAnswerItem item : req.getAnswers()) {
            TestSession.SessionQuestion sq = qMap.get(item.getQuestionId());
            if (sq == null) continue;

            boolean correct = isCorrect(item.getAnswer(), sq.getCorrectAnswer());
            if (correct) correctCount++;

            results.add(QuestionResult.builder()
                    .questionId(item.getQuestionId())
                    .wordId(sq.getWordId())
                    .correct(correct)
                    .userAnswer(item.getAnswer())
                    .correctAnswer(sq.getCorrectAnswer())
                    .build());

            // 触发学习进度更新（test 模式）
            triggerStudyAnswer(userId, sq, session.getLevelCode(), correct,
                    item.getDurationMs(), session.getMode());
        }

        // 提交后删除会话，防止重复提交
        redisTemplate.delete(RedisKey.testSession(req.getTestId()));

        int total = results.size();
        double accuracy = total == 0 ? 0.0 : (double) correctCount / total;

        return SubmitTestResponse.builder()
                .testId(req.getTestId())
                .mode(session.getMode())
                .totalCount(total)
                .correctCount(correctCount)
                .accuracy(Math.round(accuracy * 1000) / 1000.0)
                .results(results)
                .build();
    }

    // ---- private ----

    private List<WordBank> selectWords(Long userId, GenerateTestRequest req) {
        int limit = req.getSize();
        String levelCode = req.getLevelCode();
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        return switch (req.getSource()) {
            case "due" -> {
                List<UserWordProgress> due = progressMapper.findDueForReview(userId, levelCode, now, limit);
                List<Long> ids = due.stream().map(UserWordProgress::getWordId).toList();
                yield ids.isEmpty() ? List.of() : wordBankMapper.selectBatchIds(ids);
            }
            case "wrong_words" -> {
                List<WrongWord> wrongs = wrongWordMapper.selectList(
                        Wrappers.<WrongWord>lambdaQuery()
                                .eq(WrongWord::getUserId, userId)
                                .eq(WrongWord::getLevelCode, levelCode)
                                .eq(WrongWord::getResolved, 0)
                                .orderByDesc(WrongWord::getLastWrongAt)
                                .last("LIMIT " + limit));
                List<Long> ids = wrongs.stream().map(WrongWord::getWordId).toList();
                yield ids.isEmpty() ? List.of() : wordBankMapper.selectBatchIds(ids);
            }
            default -> // "all"
                wordBankMapper.findNewWords(levelCode, Collections.emptyList(), "random", limit);
        };
    }

    private TestQuestion buildClientQuestion(String qId, WordBank wb, String mode) {
        QuestionPrompt prompt = buildPrompt(wb, mode);

        List<String> choices = null;
        if ("choice".equals(mode)) {
            choices = buildChoices(wb);
        }

        return TestQuestion.builder()
                .questionId(qId)
                .wordId(wb.getId())
                .prompt(prompt)
                .choices(choices)
                .build();
    }

    private QuestionPrompt buildPrompt(WordBank wb, String mode) {
        return switch (mode) {
            case "spelling" -> QuestionPrompt.builder()
                    .zhDefinition(wb.getZhDefinition())
                    .audioUrlUk(wb.getAudioUrlUk())
                    .audioUrlUs(wb.getAudioUrlUs())
                    .build();
            case "listening" -> QuestionPrompt.builder()
                    .audioUrlUk(wb.getAudioUrlUk())
                    .audioUrlUs(wb.getAudioUrlUs())
                    .build();
            default -> // choice
                QuestionPrompt.builder()
                        .zhDefinition(wb.getZhDefinition())
                        .build();
        };
    }

    private List<String> buildChoices(WordBank wb) {
        List<WordBank> distractors = wordBankMapper.pickDistractors(
                wb.getLevelCode(), wb.getTopicCode(), wb.getId(), CHOICE_DISTRACTOR_COUNT);

        List<String> choices = new ArrayList<>();
        choices.add(wb.getWord());
        distractors.stream().map(WordBank::getWord).forEach(choices::add);

        // 如果干扰项不够 3 个，仍然返回现有选项
        Collections.shuffle(choices);
        return choices;
    }

    private boolean isCorrect(String userAnswer, String correctAnswer) {
        if (userAnswer == null || userAnswer.isBlank()) return false;
        return userAnswer.trim().equalsIgnoreCase(correctAnswer);
    }

    private TestSession loadSession(String testId, Long userId) {
        Object raw = redisTemplate.opsForValue().get(RedisKey.testSession(testId));
        if (!(raw instanceof TestSession session)) {
            throw new BizException(ErrorCode.TEST_INVALID);
        }
        if (!userId.equals(session.getUserId())) {
            throw new BizException(ErrorCode.FORBIDDEN);
        }
        return session;
    }

    private void triggerStudyAnswer(Long userId, TestSession.SessionQuestion sq,
                                     String levelCode, boolean correct,
                                     Integer durationMs, String mode) {
        try {
            AnswerRequest req = new AnswerRequest();
            req.setWordId(sq.getWordId());
            req.setLevelCode(levelCode);
            req.setResult(correct ? "correct" : "wrong");
            req.setMode(mode);
            req.setDurationMs(durationMs);
            req.setClientTs(LocalDateTime.now(ZoneOffset.UTC));
            studyService.answer(userId, req);
        } catch (Exception e) {
            log.warn("test submit: failed to update progress for wordId={}", sq.getWordId(), e);
        }
    }
}
