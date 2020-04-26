package com.tdull.db.ar;

import java.util.List;

/**
 * 2020/2/14 16:36
 *
 * @author huangtao
 */

public class WhereData {
    private String sql;
    private List<Object> data;

    public WhereData(String sql, List<Object> data) {
        this.sql = sql;
        this.data = data;
    }

    public String getSql() {
        return sql;
    }

    public void setSql(String sql) {
        this.sql = sql;
    }

    public List<Object> getData() {
        return data;
    }

    public void setData(List<Object> data) {
        this.data = data;
    }
}