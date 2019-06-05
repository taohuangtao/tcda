package com.tdull.db.ar.impl;

import com.tdull.db.ar.Model;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.*;
import java.util.*;

/**
 * 2019/4/18 12:53
 *
 * @author huangtao
 */
public class ModelImpl implements Model {
    private final Logger LOG = LoggerFactory.getLogger(ModelImpl.class);
    /**
     * 是否将驼峰发的属性名称转换为小写加下划线形式 ，只用于查询 pojo 的时候
     */
    private boolean mapUnderscoreToCamelCase = true;
    /**
     * 数据库列名是否忽略大小写
     */
    private boolean columnNameIgnoreCase = true;
    private DataSource dataSource;

    @Override
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    @Override
    public void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase) {
        this.mapUnderscoreToCamelCase = mapUnderscoreToCamelCase;
    }

    @Override
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
    private final static List<String> EXTS = Arrays.asList(">", "<", "=", "<>", "!=", "LIKE");

    public ModelImpl() {
    }

    public ModelImpl(String table) {
        this.table(table);
    }

    @Override
    public DataSource getDataSource() {
        return dataSource;
    }

    @Override
    public Model table(String table) {
        String space = " ";
        if (table.contains(space)) {
            //可能时多表链接或者是使用了别名
            this.table = table;
        } else {
            this.table = "`" + table + "`";
        }
        return this;
    }

    @Override
    public Model orderBy(String orderBy) {
        this.orderBy = " ORDER BY " + orderBy;
        return this;
    }

    @Override
    public Model groupBy(String groupBy) {
        this.groupBy = " GROUP BY " + groupBy;
        return this;
    }

    /**
     * 清除所有参数
     */
    @Override
    public void cleanAllArgument() {
        this.where = null;
        this.limit = null;
        this.filter = "*";
        this.orderBy = null;
    }

    @Override
    public Model filter(String[] filter) {
        String[] filterNew = new String[filter.length];
        for (int i = 0; i < filterNew.length; i++) {
            if (filter[i].contains("(") || filter[i].contains(" ")) {
                filterNew[i] = filter[i];
            } else {
                filterNew[i] = "`" + filter[i] + "`";
            }
        }
        this.filter = StringUtils.join(filterNew, ",");
        return this;
    }

    @Override
    public Model filter(String filter) {
        this.filter = filter;
        return this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public Model where(Map<String, Object> where) {
        Object[] wv = parseWhere(where);
        this.where = (String) wv[0];
        this.whereData = (List<Object>) wv[1];
        return this;
    }

    @Override
    public Model where(String where) {
        this.where = where;
        return this;
    }

    @Override
    public Model where(String where, List<Object> data) {
        this.where = where;
        this.whereData = data;
        return this;
    }

    @Override
    public Model page(int page, int pageSize) {
        if (page < 1) {
            throw new IllegalArgumentException("page can't be less than 1");
        }
        this.limit = String.format(" LIMIT %d,%d", (page - 1) * pageSize, pageSize);
        return this;
    }

    @Override
    public Object[] parseWhere(Map<String, Object> where) {
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
            }else if(column.indexOf("(") > 0){
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
        return new Object[]{sql.toString(), values};
    }

    @Override
    public List<Map<String, Object>> select() throws SQLException {
        LOG.debug("sql {}", getSelectSql());
        LOG.debug("whereData {}", this.whereData);
        List<Map<String, Object>> s = select(getSelectSql(), this.whereData);
        cleanAllArgument();
        return s;
    }

    @Override
    public long count() throws SQLException {
        LOG.debug("sql {}", getSelectSql());
        LOG.debug("whereData {}", this.whereData);
        this.filter("COUNT(*) c");
        List<Map<String, Object>> dataList = select(getSelectSql(), this.whereData);
        cleanAllArgument();
        return Long.parseLong(dataList.get(0).get("c").toString());
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

    @Override
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
            List<Map<String, Object>> dataList = new ArrayList<>();
            for (Map<String, Object> item : list) {
                Map<String, Object> dataItem = new HashMap<>(10);
                for (Map.Entry<String, Object> i : item.entrySet()) {
                    dataItem.put(i.getKey().toLowerCase(), i.getValue());
                    //利用原始的key进行保存
                    dataItem.put(i.getKey(), i.getValue());
                    if(mapUnderscoreToCamelCase) {
                        //column name 使用下划线命名方式，将属性名进行转换
                        dataItem.put(mapUnderscoreToCamelCase(i.getKey()), i.getValue());
                    }
                }
                dataList.add(dataItem);
            }
            list = dataList;
        }
        for (Map<String, Object> item : list) {
            //实例化类对象
            T ob = mappedClass.newInstance();
            for (Field ff : fi) {
                ff.setAccessible(true);
                String columnName;
                if (mapUnderscoreToCamelCase) {
                    //column name 使用下划线命名方式，将属性名进行转换
                    columnName = mapUnderscoreToCamelCase(ff.getName());
                } else {
                    columnName = ff.getName();
                }
                if (columnNameIgnoreCase) {
                    //忽略大小写，上面已经将column name全部转换为小写
                    columnName = columnName.toLowerCase();
                }
                try {
                    ff.set(ob, item.get(columnName));
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
    @Override
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

    @Override
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
                Map<String, Object> item = new HashMap<>(10);
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
     * @param dataList [{字段名:值}]
     * @return 插入情况
     */
    @Override
    public int[] insert(List<Map<String, Object>> dataList) throws SQLException {
        List<String> insertColumns = new ArrayList<>();
        List<String> insertValuePlaceholders = new ArrayList<>();
        String[] columns = new String[dataList.get(0).entrySet().size()];
        int columnsIndex = 0;
        for (Map.Entry<String, Object> m : dataList.get(0).entrySet()) {
            insertColumns.add("`" + m.getKey() + "`");
            insertValuePlaceholders.add("?");
            columns[columnsIndex] = m.getKey();
            columnsIndex++;
        }
        StringBuilder sql = new StringBuilder();
        sql.append("INSERT INTO ");
        sql.append(this.table);
        sql.append(" (" + StringUtils.join(insertColumns, ",") + ")");
        sql.append(" VALUES(");
        sql.append(StringUtils.join(insertValuePlaceholders, ","));
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

    private long insert(Map<String, Object> dataMap, boolean generatedKey) throws SQLException {
        List<String> insertSql = new ArrayList<>();
        //占位 ?
        List<String> insertSqlPlaceholders = new ArrayList<>();
        String[] columns = new String[dataMap.size()];
        int columsIndex = 0;
        for (Map.Entry<String, Object> m : dataMap.entrySet()) {
            insertSql.add("`" + m.getKey() + "`");
            insertSqlPlaceholders.add("?");
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
        sql.append(StringUtils.join(insertSqlPlaceholders, ","));
        sql.append(")");
        LOG.debug("sql {}", sql);
        LOG.debug("dataMap {}", dataMap);

        cleanAllArgument();

        Connection con = null;
        try {
            con = this.getDataSource().getConnection();
            PreparedStatement ps ;
            if(generatedKey){
                ps = con.prepareStatement(sql.toString(),PreparedStatement.RETURN_GENERATED_KEYS);
            }else{
                ps = con.prepareStatement(sql.toString());
            }


            for (int i = 0; i < columns.length; i++) {
                ps.setObject(i + 1, dataMap.get(columns[i]));
            }

            int s = ps.executeUpdate();
            if (generatedKey) {
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

    /**
     * 插入单条数据，获取自增字段
     * @param dataMap { 字段名 : 值 }
     * @return 返回影响的行数
     * @throws SQLException
     */
    @Override
    public int insert(Map<String, Object> dataMap) throws SQLException {
        return (int) insert(dataMap, false);
    }

    /**
     * 插入单条数据，返回自增字段
     * @param dataMap { 字段名 : 值 }
     * @return 返回自增字段
     * @throws SQLException
     */
    @Override
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

    /**
     * 更新数据
     * 数据于map封装后传入，key为字段名，value为更新的值
     * @param setMap { 字段名 : 值 }
     * @return
     * @throws SQLException
     */
    @Override
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
        return executeUpdate(sql.toString(),data);
    }

    /**
     * 执行更新或删除操作，内部调用
     * @param sql 查询的sql
     * @param data 用于替换占位符的数据，需要和占位符顺序一致
     * @return 返回影响行数
     * @throws SQLException
     */
    private int executeUpdate(String sql,List<Object> data)throws SQLException{
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
    /**
     * 删除数据，更具where条件进行删除
     * 支持语句 DELETE FROM [table_name] WHERE col1=1
     *          DELETE FROM [table_name] WHERE col1=1 AND col2='aaa'
     * @return
     */
    @Override
    public int delete() throws SQLException{
        //构造完整SQL
        StringBuilder sql = new StringBuilder();
        List<Object> data = this.whereData;
        sql.append("DELETE ");
        sql.append(this.table);
        sql.append(" FROM ");
        sql.append(this.table);
        if (null != this.where) {
            sql.append(" WHERE ");
            sql.append(this.where);
        }
        LOG.debug("sql {}", sql);
        LOG.debug("data {}", data);
        cleanAllArgument();
        //执行SQL
        return executeUpdate(sql.toString(),data);

    }
}
