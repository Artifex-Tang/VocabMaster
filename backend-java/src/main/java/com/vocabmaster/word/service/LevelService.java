package com.vocabmaster.word.service;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
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

    @SuppressWarnings("unchecked")
    public List<Level> getLevels() {
        Object cached = redisTemplate.opsForValue().get(RedisKey.LEVEL_LIST);
        if (cached instanceof List) {
            return (List<Level>) cached;
        }
        List<Level> levels = levelMapper.selectList(
                Wrappers.<Level>lambdaQuery().orderByAsc(Level::getSortOrder));
        redisTemplate.opsForValue().set(RedisKey.LEVEL_LIST, levels);
        return levels;
    }

    @SuppressWarnings("unchecked")
    public List<WordTopic> getTopics() {
        Object cached = redisTemplate.opsForValue().get(RedisKey.TOPIC_LIST);
        if (cached instanceof List) {
            return (List<WordTopic>) cached;
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
