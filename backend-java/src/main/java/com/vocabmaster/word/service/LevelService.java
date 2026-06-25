package com.vocabmaster.word.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.vocabmaster.common.constant.RedisKey;
import com.vocabmaster.word.entity.Level;
import com.vocabmaster.word.entity.WordTopic;
import com.vocabmaster.word.mapper.LevelMapper;
import com.vocabmaster.word.mapper.WordTopicMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class LevelService {

    private final LevelMapper levelMapper;
    private final WordTopicMapper wordTopicMapper;
    private final RedisTemplate<String, Object> redisTemplate;
    /** @Primary 全局 ObjectMapper（SNAKE_CASE），与 Redis 序列化器同一 bean，键名策略一致 */
    private final ObjectMapper objectMapper;

    public List<Level> getLevels() {
        Object cached = redisTemplate.opsForValue().get(RedisKey.LEVEL_LIST);
        if (cached != null) {
            // Redis 无类型信息，读回是 List<LinkedHashMap>，用与序列化同策略的 mapper 还原
            return objectMapper.convertValue(cached, new TypeReference<List<Level>>() {});
        }
        List<Level> levels = levelMapper.selectList(
                Wrappers.<Level>lambdaQuery().orderByAsc(Level::getSortOrder));
        redisTemplate.opsForValue().set(RedisKey.LEVEL_LIST, levels);
        return levels;
    }

    public List<WordTopic> getTopics() {
        Object cached = redisTemplate.opsForValue().get(RedisKey.TOPIC_LIST);
        if (cached != null) {
            return objectMapper.convertValue(cached, new TypeReference<List<WordTopic>>() {});
        }
        List<WordTopic> topics = wordTopicMapper.selectList(
                Wrappers.<WordTopic>lambdaQuery().orderByAsc(WordTopic::getSortOrder));
        redisTemplate.opsForValue().set(RedisKey.TOPIC_LIST, topics);
        return topics;
    }

    /** 管理员修改等级/主题后调用，主动失效缓存 */
    public void evictCache() {
        redisTemplate.delete(RedisKey.LEVEL_LIST);
        redisTemplate.delete(RedisKey.TOPIC_LIST);
    }
}
