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

    public static WhereTerm or(Where where,Where... wbs) {
        WhereTerm t = new WhereTerm();
        t.getList().add(where);
        for(Where b : wbs){
            t.getList().add(b);
        }
        t.setExp("OR");
        return t;
    }
    public static WhereTerm or(WhereTerm ta,WhereTerm... tb) {
        WhereTerm t = new WhereTerm();
        t.getChild().add(ta);
        for(WhereTerm b : tb){
            t.getChild().add(b);
        }
        t.setExp("OR");
        return t;
    }
    public static WhereTerm and(Where wa,Where... wbs) {
        WhereTerm t = new WhereTerm();
        t.getList().add(wa);
        for(Where wb : wbs){
            t.getList().add(wb);
        }
        t.setExp("AND");
        return t;
    }
    public static WhereTerm and(WhereTerm ta,WhereTerm... tbs) {
        WhereTerm t = new WhereTerm();
        t.getChild().add(ta);
        for(WhereTerm tb : tbs){
            t.getChild().add(tb);
        }
        t.setExp("AND");
        return t;
    }
    public static WhereTerm and(WhereTerm ta,Where... wbs) {
        WhereTerm t = new WhereTerm();
        t.getChild().add(ta);
        for(Where tb : wbs){
            t.getList().add(tb);
        }
        t.setExp("AND");
        return t;
    }
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
