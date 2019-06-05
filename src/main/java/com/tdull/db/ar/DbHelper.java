package com.tdull.db.ar;


import javax.sql.DataSource;


/**
 * Created by IntelliJ IDEA.
 * User: huangtao
 * Date: 2018/7/16
 * Time: 18:42
 */
public class DbHelper {
    private DataSource dataSource;
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }

    public Model getModelInstance() {
        Model m = new Model();
        m.setDataSource(dataSource);
        return m;
    }

    public Model getModelInstance(String tableName) {
        Model m = new Model(tableName);
        m.setDataSource(dataSource);
        return m;
    }
}
