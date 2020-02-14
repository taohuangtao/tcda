package com.tdull.db.ar.plus;

import com.tdull.db.ar.DbException;
import com.tdull.db.ar.annotation.Primary;
import com.tdull.db.ar.impl.ModelImpl;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.util.CollectionUtils;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.sql.SQLException;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * 2019/6/27 10:37
 * 对基础模型进行扩展，完成更多默认操作，简化用户操作
 *
 * @author huangtao
 */
public abstract class BaseModel<T> extends ModelImpl {
    private transient final Logger logger = LoggerFactory.getLogger(BaseModel.class);

    /**
     * 主键
     */
    private transient String primaryKey = "id";
    /**
     * 主键值
     */
    private transient Object primaryKeyValue = null;
    /**
     * 主键是否自增
     */
    private transient boolean primaryKeyAutoIncrement = true;

    /**
     * 获取主键名称
     *
     * @return
     */
    abstract String getPrimaryKey();

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



        //判断主键是否自增
        Field[] fs = this.getClass().getDeclaredFields();
        for(Field f : fs){
            if(f.isAnnotationPresent(Primary.class)){
                // 自增组件功能
            }
        }
    }

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
                mname = fieldsToColumn(mname).substring(4);
                logger.debug("mname {}", mname);
                if (null != ndata.get(mname)) {
                    try {
                        m.invoke(this, ndata.get(mname));
                        if (mname.equals(getPrimaryKey())) {
                            primaryKeyValue = ndata.get(mname);
                        }
                    } catch (IllegalAccessException | InvocationTargetException e) {
                        logger.debug(e.getMessage(), e);
                        throw new DbException(e);
                    }
                }
            }
        }
    }

    /**
     * 删除
     * 删除前必须查询，获取到主键值
     * 不查询删除请调用delete方法
     *
     * @return
     * @throws SQLException
     */
    public boolean del() throws SQLException {
        if (null == primaryKeyValue) {
            return false;
        }
        int s = this.where(new HashMap<String, Object>(1) {{
            put(getPrimaryKey(), primaryKeyValue);
        }}).delete();
        if (s == 1) {
            return true;
        } else if (s > 1) {
            throw new DbException("删除数据超过一行");
        } else {
            return false;
        }
    }

    /**
     * 实例化模型后调用save方法表示新增；
     * 查询数据后调用save方法表示更新
     *
     * @throws SQLException
     */
    public void save() throws SQLException {
        Map<String, Object> dat;
        try {
            dat = objectToMap(this);
        } catch (InvocationTargetException | IllegalAccessException e) {
            logger.debug(e.getMessage(), e);
            throw new DbException(e);
        }
        if (null != primaryKeyValue) {
            //更新
            //更新时候移除主键
            dat.remove(getPrimaryKey());
            this.where(new HashMap<String, Object>(1) {{
                put(getPrimaryKey(), primaryKeyValue);
            }}).update(dat);
        } else {
            //新增
            //删除空值
            Iterator<Map.Entry<String, Object>> sit = dat.entrySet().iterator();
            while (sit.hasNext()) {
                Map.Entry<String, Object> s = sit.next();
                if (null == s.getValue()) {
                    sit.remove();
                }
            }
            this.insert(dat);
        }
    }

    /**
     * 将bean转换为map
     * map的key和数据库字段名称对应
     *
     * @param obj
     * @return
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public Map<String, Object> objectToMap(Object obj) throws InvocationTargetException, IllegalAccessException {
        Map<String, Object> map = new HashMap<>(5);
        Method[] fi = obj.getClass().getDeclaredMethods();
        for (Method m : fi) {
            String mname = m.getName();
            if (mname.indexOf("get") != 0) {
                continue;
            }
            mname = fieldsToColumn(mname).substring(4);
            logger.debug("mname {}", mname);
            map.put(mname, m.invoke(obj));
        }
        return map;
    }

    /**
     * 将MAP的数据映射到bean上面
     *
     * @param map
     * @param obj
     * @param <J>
     * @throws InvocationTargetException
     * @throws IllegalAccessException
     */
    public <J> void mapToObject(Map<String, Object> map, J obj) throws InvocationTargetException, IllegalAccessException {
        Method[] ms = obj.getClass().getDeclaredMethods();
        for (Method m : ms) {
            String mname = m.getName();
            if (mname.indexOf("set") != 0) {
                continue;
            }
            mname = fieldsToColumn(mname).substring(4);
            logger.debug("mname {}", mname);
            if (null != map.get(mname)) {
                m.invoke(obj, map.get(mname));
            }
        }
    }

    /**
     * 将MAP的数据映射到新的bean上面
     *
     * @param map
     * @param objClass
     * @param <J>
     * @return
     * @throws IllegalAccessException
     * @throws InstantiationException
     * @throws InvocationTargetException
     */
    public <J> J mapToObject(Map<String, Object> map, Class<J> objClass) throws IllegalAccessException
            , InstantiationException
            , InvocationTargetException {
        J obj = objClass.newInstance();
        mapToObject(map, obj);
        return obj;
    }

    /**
     * 将对象属性名称转换为数据库中列名称
     * 主要是大小写转换和驼峰处理
     *
     * @return
     */
    protected String fieldsToColumn(String mname) {
        if (super.mapUnderscoreToCamelCase) {
            mname = mapUnderscoreToCamelCase(mname);
        }
        if (super.columnNameIgnoreCase) {
            mname = mname.toLowerCase();
        }
        return mname;
    }
}
