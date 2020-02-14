package com.tdull.db.ar;


import javax.sql.DataSource;
import java.sql.SQLException;
import java.util.List;
import java.util.Map;

/**
 * 2019/4/18 12:55
 *
 * @author huangtao
 */
public interface Model {

    void setDataSource(DataSource dataSource);

    void setMapUnderscoreToCamelCase(boolean mapUnderscoreToCamelCase);

    void setColumnNameIgnoreCase(boolean columnNameIgnoreCase);

    DataSource getDataSource();

    Model table(String table);

    Model orderBy(String orderBy);

    Model groupBy(String groupBy);

    /**
     * 清除所有参数
     */
    void cleanAllArgument();

    Model filter(String[] filter);

    Model filter(String filter);

    @SuppressWarnings("unchecked")
    Model where(Map<String, Object> where);

    Model where(String where);
    Model where(Where where);

    /**
     * 支持复杂条件  ((a>8) AND (b<7)) OR ((c=3) OR (d=1))
     * 利用 WhereTool 生成
     * @param where
     * @return
     */
    Model where(WhereTerm where);

    Model where(String where, List<Object> data);

    Model page(int page, int pageSize);

    Object[] parseWhere(Map<String, Object> where);

    List<Map<String, Object>> select() throws SQLException;

    long count() throws SQLException;

    <T> List<T> select(Class<T> mappedClass) throws SQLException, InstantiationException, IllegalAccessException;

    /**
     * 将pojo属性名称转成数据库的小写加下划线形式   userId      to    user_id
     *
     * @param name
     * @return
     */
    String mapUnderscoreToCamelCase(String name);

    List<Map<String, Object>> select(String sql, List<Object> whereData) throws SQLException;

    /**
     * 查询指定的内容列表
     * @param column
     * @param val
     * @return
     * @throws SQLException
     */
    List<Map<String, Object>> find(String column,Object val) throws SQLException;
    <T> List<T> find(String column,Object val,Class<T> c) throws SQLException;

    /**
     * 查询指定列的值，返回第一条查询到的数据
     * @param column
     * @param val
     * @return
     * @throws SQLException
     */
    Map<String, Object> findOne(String column,Object val) throws SQLException;
    <T> T findOne(String column,Object val,Class<T> c) throws SQLException;


    /**
     * 批量插入
     *
     * @param dataList [{字段名:值}]
     * @return 插入情况
     */
    int[] insert(List<Map<String, Object>> dataList) throws SQLException;

    /**
     * 插入单条数据，获取自增字段
     *
     * @param dataMap { 字段名 : 值 }
     * @return 返回影响的行数
     * @throws SQLException
     */
    int insert(Map<String, Object> dataMap) throws SQLException;

    /**
     * 插入单条数据，返回自增字段
     *
     * @param dataMap { 字段名 : 值 }
     * @return 返回自增字段
     * @throws SQLException
     */
    long insertRetrunGeneratedKeys(Map<String, Object> dataMap) throws SQLException;

    /**
     * 更新数据
     * 数据于map封装后传入，key为字段名，value为更新的值
     *
     * @param setMap { 字段名 : 值 }
     * @return
     * @throws SQLException
     */
    int update(Map<String, Object> setMap) throws SQLException;

    /**
     * 删除数据，更具where条件进行删除
     * 支持语句 DELETE FROM [table_name] WHERE col1=1
     * DELETE FROM [table_name] WHERE col1=1 AND col2='aaa'
     *
     * @return
     */
    int delete() throws SQLException;
}
