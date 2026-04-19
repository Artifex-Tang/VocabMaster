package com.vocabmaster.common.result;

import com.baomidou.mybatisplus.core.metadata.IPage;
import lombok.Data;

import java.util.List;
import java.util.function.Function;
import java.util.stream.Collectors;

@Data
public class PageResult<T> {

    private List<T> items;
    private long total;
    private int page;
    private int pageSize;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setItems(page.getRecords());
        result.setTotal(page.getTotal());
        result.setPage((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }

    public static <S, T> PageResult<T> of(IPage<S> page, Function<S, T> converter) {
        PageResult<T> result = new PageResult<>();
        result.setItems(page.getRecords().stream().map(converter).collect(Collectors.toList()));
        result.setTotal(page.getTotal());
        result.setPage((int) page.getCurrent());
        result.setPageSize((int) page.getSize());
        return result;
    }
}
