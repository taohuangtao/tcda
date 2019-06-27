package com.tdull.db.ar.plus;

import com.tdull.db.ar.impl.ModelImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.lang.reflect.Method;
import java.sql.SQLException;

/**
 * 2019/6/27 10:37
 * 对基础模型进行扩展，完成更多默认操作，简化用户操作
 *
 * @author huangtao
 */
public abstract class BaseModel<T> extends ModelImpl {
    private final Logger LOG = LoggerFactory.getLogger(BaseModel.class);

    /**
     * 主键
     */
    private String primaryKey = "id";

    public String getPrimaryKey() {
        return primaryKey;
    }

    public void setPrimaryKey(String primaryKey) {
        this.primaryKey = primaryKey;
    }
    public T find(Object val){
        throw new RuntimeException("未实现");
    }
    protected BaseModel() {
        super();
        String simpleName = this.getClass().getSimpleName();
        //去掉后面的model
        simpleName = simpleName.substring(0, simpleName.length() - 5);
        //将名称的驼峰命名方式，转换成 小写下划线方式
        char[] ss = simpleName.toCharArray();
        StringBuilder tableNmaeBuilder = new StringBuilder();
        for (char s : ss) {
            if (s >= 65 && s <= 90) {
                //大写
                tableNmaeBuilder.append("_");
            }
            tableNmaeBuilder.append(s);
        }
        String tableName = tableNmaeBuilder.toString().toLowerCase();
        this.table(tableName);
        this.setDataSource(DataSourceHelper.getDataSource());
        //获取所有声明方法
        Method[] ms = this.getClass().getDeclaredMethods();
    }
}
