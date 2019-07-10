package com.tdull.db.ar.plus;

import com.tdull.db.ar.DbException;
import com.tdull.db.ar.impl.ModelImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.util.CollectionUtils;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 2019/6/27 10:37
 * 对基础模型进行扩展，完成更多默认操作，简化用户操作
 *
 * @author huangtao
 */
public abstract class BaseModel extends ModelImpl {
    private final Logger logger = LoggerFactory.getLogger(BaseModel.class);

    /**
     * 主键
     */
    private String primaryKey = "id";

    /**
     * 获取主键名称
     *
     * @return
     */
    abstract String getPrimaryKey();

    public void get(Object val) throws SQLException {
        List<Map<String, Object>> list = this.where(new HashMap<String, Object>(1) {{
            put(getPrimaryKey(), val);
        }}).page(1, 1).select();
        if (!CollectionUtils.isEmpty(list)) {
            Map<String, Object> ndata = new HashMap<>(10);
            for (Map.Entry<String, Object> s : list.get(0).entrySet()) {
                String mname;
                if (super.mapUnderscoreToCamelCase) {
                    mname = mapUnderscoreToCamelCase(s.getKey());
                } else {
                    mname = s.getKey();
                }
                if (super.columnNameIgnoreCase) {
                    mname = mname.toLowerCase();
                }
                ndata.put(mname, s.getValue());
            }
            Method[] fi = this.getClass().getDeclaredMethods();
            for (Method m : fi) {
                String mname = m.getName();
                if (mname.indexOf("set") != 0) {
                    continue;
                }
                if (super.mapUnderscoreToCamelCase) {
                    mname = mapUnderscoreToCamelCase(mname);
                }
                if (super.columnNameIgnoreCase) {
                    mname = mname.toLowerCase();
                }
                mname = mname.substring(4);
                logger.debug("mname {}", mname);
                if (null != ndata.get(mname)) {
                    try {
                        m.invoke(this, ndata.get(mname));
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.debug(e.getMessage(),e);
                        throw new DbException(e);
                    }
                }
            }

        }
    }

    protected BaseModel() {
        super();
        String simpleName = this.getClass().getSimpleName();
        logger.debug("simpleName {}", simpleName);
        //去掉后面的model
        simpleName = simpleName.substring(0, simpleName.length() - 5);
        //将名称的驼峰命名方式，转换成 小写下划线方式
        char[] ss = simpleName.toCharArray();
        StringBuilder tableNameBuilder = new StringBuilder();
        for (char s : ss) {
            if (s >= 65 && s <= 90 && tableNameBuilder.length() != 0) {
                //大写
                tableNameBuilder.append("_");
            }
            tableNameBuilder.append(s);
        }
        String tableName = tableNameBuilder.toString().toLowerCase();
        this.table(tableName);
        this.setDataSource(DataSourceHelper.getDataSource());
    }
}
