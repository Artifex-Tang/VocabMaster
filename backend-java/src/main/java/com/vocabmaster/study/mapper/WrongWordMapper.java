package com.vocabmaster.study.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.vocabmaster.study.entity.WrongWord;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

@Mapper
public interface WrongWordMapper extends BaseMapper<WrongWord> {

    IPage<WrongWord> findByUserAndLevel(Page<WrongWord> page,
                                        @Param("userId") Long userId,
                                        @Param("levelCode") String levelCode,
                                        @Param("resolved") Integer resolved);
}
