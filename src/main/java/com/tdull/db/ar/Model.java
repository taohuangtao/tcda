package com.tdull.db.ar;


import javax.sql.DataSource;
import java.sql.*;
import java.util.*;

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

    Model where(String where, List<Object> data);

    Model page(int page, int pageSize);

    Object[] parseWhere(Map<String, Object> where);

    List<Map<String, Object>> select() throws SQLException;

    long count() throws SQLException;

    <T> List<T> select(Class<T> mappedClass) throws DbException;

    /**
     * 将pojo属性名称转成数据库的小写加下划线形式   userId      to    user_id
     *
     * @param name
     * @return
     */
    String mapUnderscoreToCamelCase(String name);

    List<Map<String, Object>> select(String sql, List<Object> whereData) throws DbException;

    /**
     * 批量插入
     *
     * @param dataList [{字段名:值}]
     * @return 插入情况
     */
    int[] insert(List<Map<String, Object>> dataList) throws DbException;

    /**
     * 插入单条数据，获取自增字段
     *
     * @param dataMap { 字段名 : 值 }
     * @return 返回影响的行数
     * @throws SQLException
     */
    int insert(Map<String, Object> dataMap) throws DbException;

    /**
     * 插入单条数据，返回自增字段
     *
     * @param dataMap { 字段名 : 值 }
     * @return 返回自增字段
     * @throws SQLException
     */
    long insertRetrunGeneratedKeys(Map<String, Object> dataMap) throws DbException;

    /**
     * 更新数据
     * 数据于map封装后传入，key为字段名，value为更新的值
     *
     * @param setMap { 字段名 : 值 }
     * @return
     * @throws SQLException
     */
    int update(Map<String, Object> setMap) throws DbException;

    /**
     * 删除数据，更具where条件进行删除
     * 支持语句 DELETE FROM [table_name] WHERE col1=1
     * DELETE FROM [table_name] WHERE col1=1 AND col2='aaa'
     *
     * @return
     */
    int delete() throws DbException;
}
