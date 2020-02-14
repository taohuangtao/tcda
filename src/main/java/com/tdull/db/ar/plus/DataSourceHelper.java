package com.tdull.db.ar.plus;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import javax.sql.DataSource;

/**
 * 2019/6/27 11:00
 * 用于接收全局数据源配置
 * @author huangtao
 */
public class DataSourceHelper {
    private final Logger LOG = LoggerFactory.getLogger(DataSourceHelper.class);
    private static DataSource dataSource;
    public static void setDataSource(DataSource dataSource){
        DataSourceHelper.dataSource = dataSource;
    }
    public static DataSource getDataSource(){
        return DataSourceHelper.dataSource;
    }
}
