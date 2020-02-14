package com.tdull.db.ar;

import org.apache.commons.lang3.StringUtils;
import java.util.*;

/**
 * 2019/4/18 12:51
 *
 * @author huangtao
 */
public class Where {
    /**
     * 具体的条件和数据
     */
    private Map<String, Object> w = new HashMap<>(4);
    /**
     * 可用的条件
     */
    private final static List<String> EXTS = Arrays.asList(">", ">=", "<", "<=", "=", "<>", "!=", "LIKE");

    /**
     * 在集合内
     *
     * @param col
     * @param data
     */
    public void in(String col, List<Object> data) {
        this.w.put(col, Arrays.asList("IN", data));
    }

    /**
     * 不在集合内
     *
     * @param col
     * @param data
     */
    public void notIn(String col, List<Object> data) {
        this.w.put(col, Arrays.asList("NOT IN", data));
    }

    /**
     * 大于
     *
     * @param col
     * @param val
     */
    public void gt(String col, Object val) {
        this.w.put(col, Arrays.asList(">", val));
    }

    /**
     * 大于等于
     *
     * @param col
     * @param val
     */
    public void egt(String col, Object val) {
        this.w.put(col, Arrays.asList(">=", val));
    }

    /**
     * 小于
     *
     * @param col
     * @param val
     */
    public void lt(String col, Object val) {
        this.w.put(col, Arrays.asList("<", val));
    }

    /**
     * 小于等于
     *
     * @param col
     * @param val
     */
    public void elt(String col, Object val) {
        this.w.put(col, Arrays.asList("<=", val));
    }

    /**
     * 在区间内
     *
     * @param col
     * @param min
     * @param max
     */
    public void between(String col, Object min, Object max) {
        this.w.put(col, Arrays.asList("BETWEEN", Arrays.asList(min, max)));
    }

    /**
     * 不在区间内
     *
     * @param col
     * @param min
     * @param max
     */
    public void notBetween(String col, Object min, Object max) {
        this.w.put(col, Arrays.asList("NOT BETWEEN", Arrays.asList(min, max)));
    }

    /**
     * 等于
     *
     * @param col
     * @param val
     */
    public void eq(String col, Object val) {
        this.w.put(col, Arrays.asList("=", val));
    }

    /**
     * 不等于
     *
     * @param col
     * @param val
     */
    public void neq(String col, Object val) {
        this.w.put(col, Arrays.asList("!=", val));
    }

    /**
     * 表达式，合法的 sql 片段 会作为 WHERE 的条件
     *
     * @param expression
     */
    public void _string(String expression) {
        this.w.put("_string", expression);
    }

    public WhereData getWhereData() {
        return Where.parseWhere(this.w);
    }

    public static WhereData parseWhere(Map<String, Object> where) {
        StringBuilder sql = new StringBuilder();
        List<String> whereList = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> m : where.entrySet()) {
            StringBuilder whereStrOne = new StringBuilder();
            String column = m.getKey();
            Object v = m.getValue();
            if ("_string".equals(column) && v instanceof String) {
                //是表达式，直接拼接
                whereStrOne.append(" ( ");
                whereStrOne.append(v);
                whereStrOne.append(" )");
                continue;
            }

            whereStrOne.append(" (");
            if (column.indexOf(".") > 0) {
                //有点 是表名加字段名，取消反引号
                whereStrOne.append(column);
            } else if (column.indexOf("(") > 0) {
                //有括号 是函数，取消反引号
                whereStrOne.append(column);
            } else {
                whereStrOne.append("`" + column + "`");
            }
            whereStrOne.append(" ");

            if (v instanceof List) {
                //比较条件
                String ee = ((List) v).get(0).toString().toUpperCase();
                //第一个值
                Object vv = ((List) v).get(1);
                if (EXTS.contains(ee)) {
                    //单值类型的条件
                    whereStrOne.append(" ");
                    whereStrOne.append(ee);
                    whereStrOne.append(" ?");
                    values.add(vv);
                } else if ("IN".equals(ee) || "NOT IN".equals(ee)) {
                    whereStrOne.append(" ");
                    whereStrOne.append(ee);
                    whereStrOne.append(" (");
                    if (vv instanceof List) {
                        List<String> placeholders = new ArrayList<>();
                        for (Object vvv : (List) vv) {
                            placeholders.add("?");
                            values.add(vvv);
                        }
                        whereStrOne.append(StringUtils.join(placeholders, ","));
                    } else if (vv instanceof String) {
                        whereStrOne.append(vv);
                    } else {
                        throw new IllegalArgumentException(String.format("IN Unsupported types [%s]", vv.getClass()));
                    }
                    whereStrOne.append(")");
                } else if ("BETWEEN".equals(ee)) {
                    //区间
                    whereStrOne.append(" ");
                    whereStrOne.append("BETWEEN ? AND ?");
                    //第二个值
                    Object vv2 = ((List) v).get(2);
                    values.add(vv);
                    values.add(vv2);
                } else {
                    throw new IllegalArgumentException(String.format("IN Unsupported types [%s]", vv.getClass().getName()));
                }
            } else {
                //直接判断是否相等
                whereStrOne.append(" =");
                whereStrOne.append(" ?");
                values.add(v);
            }
            whereStrOne.append(")");
            whereList.add(whereStrOne.toString());
        }
        sql.append(StringUtils.join(whereList, " AND"));
        return new WhereData(sql.toString(), values);
    }
}