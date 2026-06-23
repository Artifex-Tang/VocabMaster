package com.vocabmaster.wordlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.wordlist.entity.WordList;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface WordListMapper extends BaseMapper<WordList> {
}
