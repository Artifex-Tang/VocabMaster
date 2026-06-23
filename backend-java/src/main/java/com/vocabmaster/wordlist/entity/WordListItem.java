package com.vocabmaster.wordlist.entity;

import com.baomidou.mybatisplus.annotation.*;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@TableName("word_list_item")
public class WordListItem {

    @TableId(type = IdType.AUTO)
    private Long id;
    private Long listId;
    private Long wordId;
    private Integer unitNo;
    private Integer page;
    private Integer sortOrder;
}
