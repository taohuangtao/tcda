package com.tdull.db.ar;


import com.tdull.db.ar.impl.ModelImpl;
import com.tdull.db.ar.plus.ModelPlus;

import javax.sql.DataSource;


/**
 * Created by IntelliJ IDEA.
 * User: huangtao
 * Date: 2018/7/16
 * Time: 18:42
 * @author huangtao
 */
public class DbHelper {
    private DataSource dataSource;
    public void setDataSource(DataSource dataSource) {
        this.dataSource = dataSource;
    }
    public Model getModelInstance() {
        Model m = new ModelImpl();
        m.setDataSource(dataSource);
        return m;
    }
    public Model getModelInstance(String tableName) {
        Model m = new ModelImpl();
        m.table(tableName);
        m.setDataSource(dataSource);
        return m;
    }
}
