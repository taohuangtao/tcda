package com.tdull.db.ar;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
//import org.springframework.jdbc.core.BeanPropertyRowMapper;
//import org.springframework.jdbc.core.JdbcTemplate;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;
import java.util.Date;

/**
 * Created by IntelliJ IDEA.
 * User: huangtao
 * Date: 2018/7/9
 * Time: 17:35
 */
final public class Model {
    private final static Logger LOG = LoggerFactory.getLogger(Model.class);
    /**
     * 是否将驼峰发的属性名称转换为小写加下划线形式 ，只用于查询 pojo 的时候
     */
    private boolean mapUnderscoreToCamelCase = true;
    /**
     * 数据库列名是否忽略大小写
     */
    private boolean columnNameIgnoreCase = true;
    private DataSource dataSource;

    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
        this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
    }

    public void setColumnNameIgnoreCase(boolean columnNameIgnoreCase) {
        this.columnNameIgnoreCase = columnNameIgnoreCase;
    }

    private String table;
    private String where;
    private List<Object> whereData;
    private String set;
    private List<Object> setData;
    private String limit;
    private String filter = "*";
    private String orderBy;
    private String groupBy;
    private final static List<String> exts = Arrays.asList(">", "<", "=", "<>", "!=", "LIKE");

    public Model() {
    }

    public Model(String table) {
        this.table(table);
    }

    public DataSource getDataSource() {
        return dataSource;
    }

    public Model table(String table) {
        if (table.contains(" ")) {
            this.table = table;
        } else {
            this.table = "`" + table + "`";
        }
        return this;
    }

    public Model orderBy(String orderBy) {
        this.orderBy = " ORDER BY " + orderBy;
        return this;
    }

    public Model groupBy(String groupBy) {
        this.groupBy = " GROUP BY " + groupBy;
        return this;
    }

    /**
     * 清除所有参数
     */
    public void cleanAllArgument() {
        this.where = null;
        this.limit = null;
        this.filter = "*";
        this.orderBy = null;
    }

    public Model filter(String[] filter) {
        String[] _filter = new String[filter.length];
        for (int i = 0; i < _filter.length; i++) {
            if (filter[i].contains("(") || filter[i].contains(" ")) {
                _filter[i] = filter[i];
            } else {
                _filter[i] = "`" + filter[i] + "`";
            }
        }
        this.filter = StringUtils.join(_filter, ",");
        return this;
    }

    public Model filter(String filter) {
        this.filter = filter;
        return this;
    }

    @SuppressWarnings("unchecked")
    public Model where(Map<String, Object> where) {
        Object[] wv = parseWhere(where);
        this.where = (String) wv[0];
        this.whereData = (List<Object>) wv[1];
        return this;
    }

    public Model where(String where) {
        this.where = where;
        return this;
    }

    public Model where(String where, List<Object> data) {
        this.where = where;
        this.whereData = data;
        return this;
    }

    public Model page(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("page can't be less than 1");
        }
        this.limit = String.format(" LIMIT %d,%d", (page - 1) * pageSize, pageSize);
        return this;
    }

    public Object[] parseWhere(Map<String, Object> where) {
        StringBuilder sql = new StringBuilder();
        List<String> whereList = new ArrayList<>();
        List<Object> values = new ArrayList<>();
        for (Map.Entry<String, Object> m : where.entrySet()) {
            StringBuilder whereStrOne = new StringBuilder();
            String _key = m.getKey();
            Object v = m.getValue();
            if ("_string".equals(_key) && v instanceof String) {
                //是表达式，直接拼接
                whereStrOne.append(" ( ");
                whereStrOne.append(v);
                whereStrOne.append(" )");
                continue;
            }

            whereStrOne.append(" (");
            if (_key.indexOf(".") > 0) {
                //有点 是表名加字段名，取消反引号
                whereStrOne.append(_key);
            }else if(_key.indexOf("(") > 0){
                //有括号 是函数，取消反引号
                whereStrOne.append(_key);
            } else {
                whereStrOne.append("`" + _key + "`");
            }
            whereStrOne.append(" ");

            if (v instanceof List) {
                String ee = ((List) v).get(0).toString().toUpperCase();//比较条件
                Object vv = ((List) v).get(1);//第一个值
                if (exts.contains(ee)) {
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
//                            values.add(vv);
                    } else {
                        throw new IllegalArgumentException(String.format("IN Unsupported types [%s]", vv.getClass()));
                    }
                    whereStrOne.append(")");
                } else if ("BETWEEN".equals(ee)) {
                    //区间
                    whereStrOne.append(" ");
                    whereStrOne.append("BETWEEN ? AND ?");
                    Object vv2 = ((List) v).get(2);//第二个值
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
        return new Object[]{sql.toString(), values};
    }

    public List<Map<String, Object>> select() throws SQLException {
        LOG.debug("sql {}", getSelectSql());
        LOG.debug("whereData {}", this.whereData);
        List<Map<String, Object>> s = select(getSelectSql(), this.whereData);
        cleanAllArgument();
        return s;
    }

    public long count() throws SQLException {
        LOG.debug("sql {}", getSelectSql());
        LOG.debug("whereData {}", this.whereData);
        this.filter("COUNT(*) c");
        List<Map<String, Object>> _count = select(getSelectSql(), this.whereData);
        cleanAllArgument();
        return Long.parseLong(_count.get(0).get("c").toString());
    }

    private String getSelectSql() {
        StringBuilder sql = new StringBuilder();
        sql.append("SELECT ");
        sql.append(this.filter);
        sql.append(" FROM ");
        sql.append(this.table);
        List<Object> values = new ArrayList<>();
        if (StringUtils.isNotEmpty(this.where)) {
            sql.append(" WHERE ");
            sql.append(where);
        }
        if (StringUtils.isNotEmpty(this.groupBy)) {
            sql.append(this.groupBy);
        }
        if (StringUtils.isNotEmpty(this.orderBy)) {
            sql.append(this.orderBy);
        }
        if (StringUtils.isNotEmpty(this.limit)) {
            sql.append(this.limit);
        }
        return sql.toString();
    }

    public <T> List<T> select(Class<T> mappedClass) throws SQLException, InstantiationException, IllegalAccessException {
        LOG.debug("sql {}", getSelectSql());
        LOG.debug("whereData {}", this.whereData);
        List<T> list = select(getSelectSql(), this.whereData, mappedClass);
        cleanAllArgument();
        return list;
    }

    @SuppressWarnings("unchecked")
    private <T> List<T> select(String sql, List<Object> whereData, Class<T> mappedClass) throws SQLException, IllegalAccessException, InstantiationException {

        List<T> ar = new ArrayList<>();
        Field[] fi = mappedClass.getDeclaredFields();


        List<Map<String, Object>> list = select(sql, whereData);

        if (columnNameIgnoreCase) {
            //将数据库查询到的列名全部转换为小写
            List<Map<String, Object>> new_list = new ArrayList<>();
            for (Map<String, Object> item : list) {
                Map<String, Object> new_item = new HashMap<>();
                for (Map.Entry<String, Object> i : item.entrySet()) {
                    new_item.put(i.getKey().toLowerCase(), i.getValue());
                    new_item.put(i.getKey(), i.getValue());//利用原始的key进行保存
                    if(mapUnderscoreToCamelCase) {
                        //column name 使用下划线命名方式，将属性名进行转换
                        new_item.put(mapUnderscoreToCamelCase(i.getKey()), i.getValue());
                    }
                }
                new_list.add(new_item);
            }
            list = new_list;
        }
        for (Map<String, Object> item : list) {
            T ob = mappedClass.newInstance();//实例化类对象
            for (Field ff : fi) {
                ff.setAccessible(true);
                String new_name;
                if (mapUnderscoreToCamelCase) {
                    //column name 使用下划线命名方式，将属性名进行转换
                    new_name = mapUnderscoreToCamelCase(ff.getName());
                } else {
                    new_name = ff.getName();
                }
                if (columnNameIgnoreCase) {
                    //忽略大小写，上面已经将column name全部转换为小写
                    new_name = new_name.toLowerCase();
                }
                try {
                    ff.set(ob, item.get(new_name));
                } catch (Exception e) {
                    LOG.error(e.getMessage(), e);
                }
            }
            ar.add(ob);
        }

        return ar;
    }

    /**
     * 将pojo属性名称转成数据库的小写加下划线形式   userId      to    user_id
     *
     * @param name
     * @return
     */
    public String mapUnderscoreToCamelCase(String name) {
        char[] ss = name.toCharArray();
        StringBuilder column = new StringBuilder();
        for (char s : ss) {
            if (Character.isUpperCase(s)) {
                column.append("_" + String.valueOf(s).toLowerCase());
            } else {
                column.append(s);
            }
        }
        return column.toString();
    }

    public List<Map<String, Object>> select(String sql, List<Object> whereData) throws SQLException {
        Connection con = null;
        try {
            con = this.getDataSource().getConnection();
            PreparedStatement ps = con.prepareStatement(sql);
            LOG.debug(sql);
            if (null != whereData && whereData.size() > 0) {
                for (int i = 0; i < whereData.size(); i++) {
                    ps.setObject(i + 1, whereData.get(i));
                }
            }
            ResultSet res = ps.executeQuery();
            ResultSetMetaData rsmd = res.getMetaData();
            int count = rsmd.getColumnCount();
            String[] name = new String[count];
            for (int i = 0; i < count; i++) {
                name[i] = rsmd.getColumnLabel(i + 1);
            }
            List<Map<String, Object>> data = new ArrayList<>();
            while (res.next()) {
                Map<String, Object> item = new HashMap<>();
                for (String lname : name) {
                    item.put(lname, res.getObject(lname));
                }
                data.add(item);
            }
            res.close();
            ps.close();
            return data;
        } finally {
            con.close();
        }
    }

    /**
     * 批量插入
     *
     * @param dataList
     * @return
     */
    public int[] insert(List<Map<String, Object>> dataList) throws SQLException {
        List<String> insert_columns = new ArrayList<>();
        List<String> insert_v = new ArrayList<>();
        String[] columns = new String[dataList.get(0).entrySet().size()];
        int columnsIndex = 0;
        for (Map.Entry<String, Object> m : dataList.get(0).entrySet()) {
            insert_columns.add("`" + m.getKey() + "`");
            insert_v.add("?");
            columns[columnsIndex] = m.getKey();
            columnsIndex++;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(this.table);
        sql.append(" (" + StringUtils.join(insert_columns, ",") + ")");
        sql.append(" VALUES(");
        sql.append(StringUtils.join(insert_v, ","));
        sql.append(")");
        LOG.debug("sql {}", sql);


        cleanAllArgument();

        Connection con = null;
        try {
            con = this.getDataSource().getConnection();
            PreparedStatement ps = con.prepareStatement(sql.toString());
            for (Map<String, Object> m : dataList) {
                for (int i = 0; i < columns.length; i++) {
                    ps.setObject(i + 1, m.get(columns[i]));
                }
                ps.addBatch();
            }
            int[] ns = ps.executeBatch();
            ps.close();
            return ns;
        } finally {
            con.close();
        }
    }

    private long insert(Map<String, Object> dataMap, boolean GeneratedKey) throws SQLException {
        List<String> insertSql = new ArrayList<>();
        List<String> insertSql_vv = new ArrayList<>();//占位 ?
        String[] columns = new String[dataMap.size()];
        int columsIndex = 0;
        for (Map.Entry<String, Object> m : dataMap.entrySet()) {
            insertSql.add("`" + m.getKey() + "`");
            insertSql_vv.add("?");
            columns[columsIndex] = m.getKey();
            columsIndex++;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(this.table);
        sql.append(" (");
        sql.append(StringUtils.join(insertSql, ","));
        sql.append(")");
        sql.append(" VALUES(");
        sql.append(StringUtils.join(insertSql_vv, ","));
        sql.append(")");
        LOG.debug("sql {}", sql);
        LOG.debug("dataMap {}", dataMap);

        cleanAllArgument();

        Connection con = null;
        try {
            con = this.getDataSource().getConnection();
            PreparedStatement ps ;
            if(GeneratedKey){
                ps = con.prepareStatement(sql.toString(),PreparedStatement.RETURN_GENERATED_KEYS);
            }else{
                ps = con.prepareStatement(sql.toString());
            }


            for (int i = 0; i < columns.length; i++) {
                ps.setObject(i + 1, dataMap.get(columns[i]));
            }

            int s = ps.executeUpdate();
            if (GeneratedKey) {
                ResultSet kk = ps.getGeneratedKeys();
                kk.next();
                long key = kk.getLong(1);
                kk.close();
                ps.close();
                return key;
            } else {
                return s;
            }
        } finally {
            con.close();
        }
    }

    public int insert(Map<String, Object> dataMap) throws SQLException {
        return (int) insert(dataMap, false);
    }

    public long insertRetrunGeneratedKeys(Map<String, Object> dataMap) throws SQLException {
        return insert(dataMap, true);
    }

    private Model set(Map<String, Object> setMap) {
        List<String> sqlList = new ArrayList<>();
        List<Object> setData = new ArrayList<>();
        for (Map.Entry<String, Object> m : setMap.entrySet()) {
            sqlList.add(new StringBuilder(" `").append(m.getKey()).append("` = ?").toString());
            setData.add(m.getValue());
        }
        this.set = StringUtils.join(sqlList, ",");
        this.setData = setData;
        return this;
    }

    public int update(Map<String, Object> setMap) throws SQLException {
        //构造数据
        this.set(setMap);

        //构造完整SQL
        StringBuilder sql = new StringBuilder();
        List<Object> data = this.setData;
        sql.append("UPDATE ");
        sql.append(this.table);
        sql.append(" SET ");
        sql.append(this.set);
        if (null != this.where) {
            sql.append(" WHERE ");
            sql.append(this.where);
            data.addAll(this.whereData);
        }
        LOG.debug("sql {}", sql);
        LOG.debug("data {}", data);

        cleanAllArgument();

        //执行SQL
        Connection con = null;
        try {
            con = this.getDataSource().getConnection();
            PreparedStatement ps = con.prepareStatement(sql.toString());

            for (int i = 0; i < data.size(); i++) {
                ps.setObject(i + 1, data.get(i));
            }
            int n = ps.executeUpdate();
            ps.close();
            return n;
        } finally {
            con.close();
        }
    }
}
