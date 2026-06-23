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

    /** 短语判定：词含空格，或 PoS 为 phrase / phrasal verb。 */
    public static boolean isPhrase(String word, String pos) {
        if (word != null && word.trim().contains(" ")) return true;
        if (pos != null) {
            String p = pos.trim().toLowerCase();
            return p.equals("phrase") || p.equals("phrasal verb");
        }
        return false;
    }

    /** 该词是否允许某种测试模式。短语排除 spelling；choice/listening 全允许。 */
    public static boolean allowsMode(String word, String pos, String mode) {
        if (!VALID_MODES.contains(mode)) return false;
        if ("spelling".equals(mode) && isPhrase(word, pos)) return false;
        return true;
    }

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
        int target = req.getSize();
        List<TestSession.SessionQuestion> sessionQuestions = new ArrayList<>(target);
        List<TestQuestion> clientQuestions = new ArrayList<>(target);

        // 过滤掉不支持当前模式的词条（如 spelling 排除短语），达到目标数量即停止
        int qIdx = 0;
        for (WordBank wb : words) {
            if (sessionQuestions.size() >= target) break;
            if (!allowsMode(wb.getWord(), wb.getPos(), req.getMode())) continue;

            qIdx++;
            String qId = "q" + qIdx;

            sessionQuestions.add(TestSession.SessionQuestion.builder()
                    .questionId(qId)
                    .wordId(wb.getId())
                    .correctAnswer(wb.getWord().toLowerCase())
                    .build());

            clientQuestions.add(buildClientQuestion(qId, wb, req.getMode()));
        }
        // 候选词全部过滤后可能少于目标数量，保留现有题目不报错；但若一个都没剩下则报错
        if (clientQuestions.isEmpty()) {
            throw new BizException(ErrorCode.WORD_NOT_FOUND, "当前来源下没有可出题的单词");
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

    /** 查询各来源可用题目数量，供前端禁用无数据的选项 */
    public Map<String, Integer> getAvailability(Long userId, String levelCode) {
        LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC);

        int dueCount = (int) progressMapper.findDueForReview(userId, levelCode, now, 9999).stream()
                .map(UserWordProgress::getWordId).distinct().count();

        int wrongCount = Math.toIntExact(wrongWordMapper.selectCount(
                Wrappers.<WrongWord>lambdaQuery()
                        .eq(WrongWord::getUserId, userId)
                        .eq(WrongWord::getLevelCode, levelCode)
                        .eq(WrongWord::getResolved, 0)));

        int allCount = Math.toIntExact(wordBankMapper.selectCount(
                Wrappers.<WordBank>lambdaQuery()
                        .eq(WordBank::getLevelCode, levelCode)));

        return Map.of("due", dueCount, "wrong_words", wrongCount, "all", allCount);
    }

    // ---- private ----

    private List<WordBank> selectWords(Long userId, GenerateTestRequest req) {
        int limit = req.getSize();
        // spelling 模式会过滤掉短语，预取更多候选以保证最终题数尽量接近 size
        if ("spelling".equals(req.getMode())) {
            limit = limit + limit / 2 + 4;
        }
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
                    .enDefinition(wb.getEnDefinition())
                    .audioUrlUk(wb.getAudioUrlUk())
                    .audioUrlUs(wb.getAudioUrlUs())
                    .build();
            case "listening" -> QuestionPrompt.builder()
                    .audioUrlUk(wb.getAudioUrlUk())
                    .audioUrlUs(wb.getAudioUrlUs())
                    .word(wb.getWord())
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
        if (raw == null) {
            throw new BizException(ErrorCode.TEST_INVALID);
        }
        TestSession session;
        if (raw instanceof TestSession ts) {
            session = ts;
        } else {
            // JSON 反序列化为 LinkedHashMap，手动转换
            com.fasterxml.jackson.databind.ObjectMapper mapper = new com.fasterxml.jackson.databind.ObjectMapper();
            mapper.setPropertyNamingStrategy(com.fasterxml.jackson.databind.PropertyNamingStrategies.SNAKE_CASE);
            session = mapper.convertValue(raw, TestSession.class);
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
