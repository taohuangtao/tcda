package com.tdull.db.ar;



/**
 * 2019/1/14 17:11
 * huangtao
 */

public class ModelTest {

    public static void main(String[] args) {
        Model m = new Model();
        System.out.println(m.mapUnderscoreToCamelCase("userId"));
        System.out.println(m.mapUnderscoreToCamelCase("accountIdJksdjf"));
    }

}
