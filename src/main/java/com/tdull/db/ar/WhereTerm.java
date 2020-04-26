package com.tdull.db.ar;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020/2/14 17:35
 * 数 节点
 * @author huangtao
 */
public class WhereTerm {
    /**
     * list 中 各个where之间关系
     * or and
     */
    private String exp;
    /**
     * 条件可能是多层嵌套，子节点
     */
    private List<WhereTerm> child = new ArrayList<>();
    /**
     * 单层的 条件 数据 和child不能同时有数据，只能最低节点才有数据
     */
    private List<Where> list = new ArrayList<>();
    public String getExp() {
        return exp;
    }

    public void setExp(String exp) {
        this.exp = exp;
    }

    public List<WhereTerm> getChild() {
        return child;
    }

    public void setChild(List<WhereTerm> child) {
        this.child = child;
    }

    public List<Where> getList() {
        return list;
    }

    public void setList(List<Where> list) {
        this.list = list;
    }
}
