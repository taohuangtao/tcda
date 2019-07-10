package com.tdull.db.ar.plus;

import com.alibaba.fastjson.JSONObject;
import org.junit.Assert;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.support.FileSystemXmlApplicationContext;

import java.lang.reflect.InvocationTargetException;
import java.sql.SQLException;

import static org.junit.Assert.*;

/**
 * 2019/7/9 17:16
 *
 * @author huangtao
 */
public class BaseModelTest {
    private final Logger logger = LoggerFactory.getLogger(BaseModel.class);
    static {
        FileSystemXmlApplicationContext context =
                new FileSystemXmlApplicationContext(BaseModelTest.class.getResource("/spring-base.xml").getPath());

    }
    class BaseUserModel extends BaseModel{
        private String username;
        private String password;

        public String getUsername() {
            return username;
        }

        public void setUsername(String username) {
            this.username = username;
        }

        public String getPassword() {
            return password;
        }

        public void setPassword(String password) {
            this.password = password;
        }

        @Override
        String getPrimaryKey() {
            return "username";
        }
    }
    @Test
    public void setPrimaryKey() {
        BaseUserModel m = new BaseUserModel();
        try {
            m.get("admin");
        } catch (Exception e) {
            throw new AssertionError(e);
        }
        Assert.assertEquals("c199cd8e561a360bab92625a3e4d86146a7de22fc796ecae5f0ac0fc6a8f5752",m.getPassword());
        logger.info("{}", JSONObject.toJSONString(m));
    }

    @Test
    public void get() {
    }

    public static void main(String[] args) {
        BaseModelTest t = new BaseModelTest();
        t.setPrimaryKey();
    }
}