package com.tdull.db.ar;

import java.sql.SQLException;

/**
 * Created by IntelliJ IDEA.
 * User: huangtao
 * Date: 2018/7/10
 * Time: 18:21
 */
public class DbException extends SQLException {
    public DbException(){

    }
    public DbException(String msg){
        super(msg);
    }
}
