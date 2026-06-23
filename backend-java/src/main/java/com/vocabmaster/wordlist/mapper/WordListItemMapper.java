package com.vocabmaster.wordlist.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.vocabmaster.wordlist.entity.WordListItem;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;
import java.util.Map;

@Mapper
public interface WordListItemMapper extends BaseMapper<WordListItem> {

    /** 某 list 各 unit 的词数（unit_no, cnt）。 */
    @Select("""
        SELECT unit_no, COUNT(*) AS cnt
        FROM word_list_item
        WHERE list_id = #{listId}
        GROUP BY unit_no
        ORDER BY unit_no
    """)
    List<Map<String, Object>> countByUnit(@Param("listId") Long listId);

    /** 某 list 某 unit 的全部 word_id（按 sort_order）。 */
    @Select("""
        SELECT word_id FROM word_list_item
        WHERE list_id = #{listId} AND unit_no = #{unitNo}
        ORDER BY sort_order
    """)
    List<Long> findWordIdsByUnit(@Param("listId") Long listId,
                                 @Param("unitNo") Integer unitNo);
}
