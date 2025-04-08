package org.jmqtt.starter.api.entity;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

import java.util.List;

@Getter
@Setter
@ToString
@AllArgsConstructor
public class PageVo<T> {

    /**
     * 总记录数
     */
    private long totalCount;

    /**
     * 每页记录数
     */
    private int pageSize;

    /**
     * 总页数
     */
    private int totalPage;

    /**
     * 当前页数
     */
    private int currPage;

    /**
     * 列表数据
     */
    private List<T> list;

    /**
     * 分页结果
     *
     * @param list       列表数据
     * @param totalCount 总记录数
     * @param currPage   当前页数
     * @param pageSize   每页记录数
     */
    public PageVo(List<T> list, long totalCount, int currPage, int pageSize) {
        this.list = list;
        this.totalCount = totalCount;
        this.currPage = currPage;
        this.pageSize = pageSize;
        this.totalPage = (int) Math.ceil((double) totalCount / pageSize);
    }
}
