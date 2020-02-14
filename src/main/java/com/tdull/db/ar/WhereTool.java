package com.tdull.db.ar;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

/**
 * 2020/2/14 17:45
 * 复杂where操作，进行while条件组合
 * @author huangtao
 */
public class WhereTool {
    private final Logger logger = LoggerFactory.getLogger(WhereTool.class);

    public static WhereTerm or(Where... ws) {
        WhereTerm t = new WhereTerm();
        for(Where b : ws){
            t.getList().add(b);
        }
        t.setExp("OR");
        return t;
    }
    public static WhereTerm or(WhereTerm... ts) {
        WhereTerm t = new WhereTerm();
        for(WhereTerm b : ts){
            t.getChild().add(b);
        }
        t.setExp("OR");
        return t;
    }
    public static WhereTerm and(Where... ws) {
        WhereTerm t = new WhereTerm();
        for(Where wb : ws){
            t.getList().add(wb);
        }
        t.setExp("AND");
        return t;
    }
    public static WhereTerm and(WhereTerm... ts) {
        WhereTerm t = new WhereTerm();
        for(WhereTerm tb : ts){
            t.getChild().add(tb);
        }
        t.setExp("AND");
        return t;
    }
    public static WhereTerm and(WhereTerm ta,Where... ws) {
        WhereTerm t = new WhereTerm();
        t.getChild().add(ta);
        for(Where w : ws){
            t.getList().add(w);
        }
        t.setExp("AND");
        return t;
    }

    /**
     * 抓换 普通 where 条件为树形节点
     * @param w
     * @return
     */
    public static WhereTerm whereToWhereTerm(Where w){
        WhereTerm t = new WhereTerm();
        t.getList().add(w);
        return t;
    }

    /**
     * 解析 树形 where 条件为 model 可用的查询条件
     * @param t 复炸树形查询条件的根节点
     * @return
     */
    public static WhereData parseTerm(WhereTerm t){
        StringBuffer sql = new StringBuffer();
        List<Object> data = new ArrayList<>();
        sql.append("(");
        List<String> sqls = new ArrayList<>();
        if(t.getList().size()>0) {
            for (Where w : t.getList()) {
                WhereData wd = w.getWhereData();
                sqls.add(wd.getSql());
                data.addAll(wd.getData());
            }
        }
        if(t.getChild().size()>0){
            for(WhereTerm ct : t.getChild()){
                WhereData wd = parseTerm(ct);
                sqls.add(wd.getSql());
                data.addAll(wd.getData());
            }
        }
        sql.append(StringUtils.join(sqls,") "+ t.getExp() +" ("));
        sql.append(")");
        return new WhereData(sql.toString(),data);
    }
}
